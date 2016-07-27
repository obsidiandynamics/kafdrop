/*
 * Copyright 2016 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.homeadvisor.kafdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.util.BrokerChannel;
import kafka.api.ConsumerMetadataRequest;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.network.BlockingChannel;
import kafka.utils.ZKGroupDirs;
import kafka.utils.ZKGroupTopicDirs;
import kafka.utils.ZkUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Service
public class CuratorKafkaMonitor implements KafkaMonitor
{
   private final Logger LOG = LoggerFactory.getLogger(getClass());

   @Autowired
   private CuratorFramework curatorFramework;

   @Autowired
   private ObjectMapper objectMapper;

   private PathChildrenCache brokerPathCache;
   private PathChildrenCache topicConfigPathCache;
   private TreeCache consumerTreeCache;
   private NodeCache controllerNodeCache;

   private int controllerId = -1;

   private Map<Integer, BrokerVO> brokerCache = new TreeMap<>();

   private AtomicInteger cacheInitCounter = new AtomicInteger();

   private ForkJoinPool threadPool;

   private static final int DEFAULT_THREAD_POOL_SIZE = 10;

   @Value("${kafdrop.monitor.threadPoolSize:10}")
   private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;


   @PostConstruct
   public void start() throws Exception
   {
      if (threadPoolSize <= 0)
      {
         LOG.warn("Thread pool size {} is less than or equal to zero. Using default of {}",
                  threadPoolSize, DEFAULT_THREAD_POOL_SIZE);
         threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
      }
      threadPool = new ForkJoinPool(threadPoolSize);

      cacheInitCounter.set(3);

      brokerPathCache = new PathChildrenCache(curatorFramework, ZkUtils.BrokerIdsPath(), true);
      brokerPathCache.getListenable().addListener(new BrokerListener());
      brokerPathCache.getListenable().addListener((f, e) -> {
         if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED)
         {
            cacheInitCounter.decrementAndGet();
            LOG.info("Broker cache initialized");
         }
      });
      brokerPathCache.start(StartMode.POST_INITIALIZED_EVENT);

      topicConfigPathCache = new PathChildrenCache(curatorFramework, ZkUtils.TopicConfigPath(), true);
      topicConfigPathCache.getListenable().addListener((f, e) -> {
         if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED)
         {
            cacheInitCounter.decrementAndGet();
            LOG.info("Topic configuration cache initialized");
         }
      });
      topicConfigPathCache.start(StartMode.POST_INITIALIZED_EVENT);

      consumerTreeCache = new TreeCache(curatorFramework, ZkUtils.ConsumersPath());
      consumerTreeCache.getListenable().addListener((client, event) -> {
         if (event.getType() == TreeCacheEvent.Type.INITIALIZED)
         {
            cacheInitCounter.decrementAndGet();
            LOG.info("Consumer tree cache initialized");
         }
      });
      consumerTreeCache.start();

      controllerNodeCache = new NodeCache(curatorFramework, ZkUtils.ControllerPath());
      controllerNodeCache.getListenable().addListener(this::updateController);
      controllerNodeCache.start(true);
      updateController();
   }

   private void updateController()
   {
      Optional.ofNullable(controllerNodeCache.getCurrentData())
         .map(data -> {
            try
            {
               Map controllerData = objectMapper.reader(Map.class).readValue(data.getData());
               return (Integer) controllerData.get("brokerid");
            }
            catch (IOException e)
            {
               LOG.error("Unable to read controller data", e);
               return null;
            }
         })
         .ifPresent(this::updateController);
   }

   private void updateController(int brokerId)
   {
      brokerCache.values()
         .forEach(broker -> broker.setController(broker.getId() == brokerId));
   }

   private void validateInitialized()
   {
      if (cacheInitCounter.get() > 0)
      {
         throw new NotInitializedException();
      }
   }


   @PreDestroy
   public void stop() throws IOException
   {
      consumerTreeCache.close();
      topicConfigPathCache.close();
      brokerPathCache.close();
      controllerNodeCache.close();
   }

   private int brokerId(ChildData input)
   {
      return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
   }

   private BrokerVO addBroker(BrokerVO broker)
   {
      final BrokerVO oldBroker = brokerCache.put(broker.getId(), broker);
      LOG.info("Kafka broker {} was {}", broker.getId(), oldBroker == null ? "added" : "updated");
      return oldBroker;
   }

   private BrokerVO removeBroker(int brokerId)
   {
      final BrokerVO broker = brokerCache.remove(brokerId);
      LOG.info("Kafka broker {} was removed", broker.getId());
      return broker;
   }

   @Override
   public List<BrokerVO> getBrokers()
   {
      validateInitialized();
      return brokerCache.values().stream().collect(Collectors.toList());
   }

   @Override
   public Optional<BrokerVO> getBroker(int id)
   {
      validateInitialized();
      return Optional.ofNullable(brokerCache.get(id));
   }

   private BrokerChannel brokerChannel(Integer brokerId)
   {
      if (brokerId == null)
      {
         brokerId = randomBroker();
         if (brokerId == null)
         {
            throw new BrokerNotFoundException("No brokers available to select from");
         }
      }

      Integer finalBrokerId = brokerId;
      BrokerVO broker = getBroker(brokerId)
         .orElseThrow(() -> new BrokerNotFoundException("Broker " + finalBrokerId + " is not available"));

      return BrokerChannel.forBroker(broker.getHost(), broker.getPort());
   }

   private Integer randomBroker()
   {
      List<Integer> brokerIds = brokerCache.keySet().stream().collect(Collectors.toList());
      Collections.shuffle(brokerIds);
      return brokerIds.get(0);
   }

   @Override
   public List<TopicVO> getTopics()
   {
      validateInitialized();
      return getTopicMetadata().values().stream()
         .sorted(Comparator.comparing(TopicVO::getName))
         .collect(Collectors.toList());
   }

   @Override
   public Optional<TopicVO> getTopic(String topic)
   {
      validateInitialized();
      final Optional<TopicVO> topicVO = Optional.ofNullable(getTopicMetadata(topic).get(topic));
      topicVO.ifPresent(
         vo -> {
            getTopicPartitionSizes(vo, kafka.api.OffsetRequest.LatestTime())
               .entrySet().stream()
               .forEach(entry -> vo.getPartition(entry.getKey()).ifPresent(p -> p.setSize(entry.getValue())));
            getTopicPartitionSizes(vo, kafka.api.OffsetRequest.EarliestTime())
               .entrySet().stream()
               .forEach(entry -> vo.getPartition(entry.getKey()).ifPresent(p -> p.setFirstOffset(entry.getValue())));
         }
      );
      return topicVO;
   }

   private Map<String, TopicVO> getTopicMetadata(String... topics)
   {
      return brokerChannel(null).execute(channel ->
      {
         final TopicMetadataRequest request = new TopicMetadataRequest((short) 0, 0, "", Arrays.asList(topics));
         LOG.debug("Sending topic metadata request: {}", request);
         channel.send(request);
         final kafka.api.TopicMetadataResponse underlyingResponse =
            kafka.api.TopicMetadataResponse.readFrom(channel.receive().buffer());

         LOG.debug("Received topic metadata response: {}", underlyingResponse);

         TopicMetadataResponse response = new TopicMetadataResponse(underlyingResponse);
         return response.topicsMetadata().stream()
            .filter(tmd -> tmd.errorCode() == ErrorMapping.NoError())
            .map(this::processTopicMetadata)
            .collect(Collectors.toMap(TopicVO::getName, t -> t));
      });
   }

   private TopicVO processTopicMetadata(TopicMetadata tmd)
   {
      TopicVO topic = new TopicVO(tmd.topic());

      topic.setConfig(
         Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
            .map(this::readTopicConfig)
            .orElse(Collections.emptyMap()));

      topic.setPartitions(
         tmd.partitionsMetadata().stream()
            .map(this::parsePartitionMetadata)
            .collect(Collectors.toMap(TopicPartitionVO::getId, p -> p))
      );
      return topic;
   }

   private TopicPartitionVO parsePartitionMetadata(PartitionMetadata pmd)
   {
      TopicPartitionVO partition = new TopicPartitionVO(pmd.partitionId());
      if (pmd.leader() != null)
      {
         partition.addReplica(new TopicPartitionVO.PartitionReplica(pmd.leader().id(), true, true));
      }

      pmd.replicas().stream()
         .map(r -> new TopicPartitionVO.PartitionReplica(r.id(), pmd.isr().contains(r), false))
         .forEach(partition::addReplica);
      return partition;
   }

   private Map<String, Object> readTopicConfig(ChildData d)
   {
      try
      {
         final Map<String, Object> configData = objectMapper.reader(Map.class).readValue(d.getData());
         return (Map<String, Object>) configData.get("config");
      }
      catch (IOException e)
      {
         throw Throwables.propagate(e);
      }
   }

   @Override
   public List<ConsumerVO> getConsumers()
   {
      validateInitialized();
      return getConsumerStream(Optional.empty()).collect(Collectors.toList());
   }

   @Override
   public List<ConsumerVO> getConsumers(final TopicVO topic)
   {
      validateInitialized();
      return getConsumerStream(Optional.of(topic))
         .filter(consumer -> consumer.getTopic(topic.getName()) != null)
         .collect(Collectors.toList());
   }

   @Override
   public List<ConsumerVO> getConsumers(final String topic)
   {
      return getConsumers(getTopic(topic).get());
   }

   private Stream<ConsumerVO> getConsumerStream(Optional<TopicVO> topic)
   {
      return consumerTreeCache.getCurrentChildren(ZkUtils.ConsumersPath()).keySet().stream()
         .map(g -> getConsumerByTopic(g, topic))
         .filter(Optional::isPresent)
         .map(Optional::get)
         .sorted(Comparator.comparing(ConsumerVO::getGroupId));
   }

   @Override
   public Optional<ConsumerVO> getConsumer(String groupId)
   {
      validateInitialized();
      return getConsumerByTopic(groupId, Optional.<TopicVO>empty());
   }

   @Override
   public Optional<ConsumerVO> getConsumerByTopicName(String groupId, Optional<String> topicName)
   {
      return getConsumerByTopic(groupId, topicName.map(this::getTopic).orElse(Optional.<TopicVO>empty()));
   }

   @Override
   public Optional<ConsumerVO> getConsumerByTopic(String groupId, Optional<TopicVO> topic)
   {
      final ConsumerVO consumer = new ConsumerVO(groupId);
      final ZKGroupDirs groupDirs = new ZKGroupDirs(groupId);

      if (consumerTreeCache.getCurrentData(groupDirs.consumerGroupDir()) == null) return Optional.empty();

      // todo: get number of threads in each instance (subscription -> topic -> # threads)
      Optional.ofNullable(consumerTreeCache.getCurrentChildren(groupDirs.consumerRegistryDir()))
         .ifPresent(
            children ->
               children.keySet().stream()
                  .map(id -> readConsumerRegistration(groupDirs, id))
                  .forEach(consumer::addActiveInstance));

      Stream<String> topicStream;

      topic.map(
         vo -> (consumerTreeCache.getCurrentData(groupDirs.consumerGroupDir() + "/owners/" + vo.getName()) != null)
            ? Stream.of(vo.getName()) : Stream.<String>empty())
         .orElse(
            Optional.ofNullable(
               consumerTreeCache.getCurrentChildren(groupDirs.consumerGroupDir() + "/owners"))
               .map(c -> c.keySet().stream())
               .orElse(Stream.<String>empty()))
         .map(ConsumerTopicVO::new)
         .forEach(consumerTopic -> {
            getConsumerPartitionStream(groupId, consumerTopic.getTopic(), topic)
               .forEach(consumerTopic::addOffset);
            consumer.addTopic(consumerTopic);
         });
      return Optional.of(consumer);
   }

   private ConsumerRegistrationVO readConsumerRegistration(ZKGroupDirs groupDirs, String id)
   {
      try
      {
         ChildData data = consumerTreeCache.getCurrentData(groupDirs.consumerRegistryDir() + "/" + id);
         final Map<String, Object> consumerData = objectMapper.reader(Map.class).readValue(data.getData());
         Map<String, Integer> subscriptions = (Map<String, Integer>) consumerData.get("subscription");

         ConsumerRegistrationVO vo = new ConsumerRegistrationVO(id);
         vo.setSubscriptions(subscriptions);
         return vo;
      }
      catch (IOException ex)
      {
         throw Throwables.propagate(ex);
      }
   }

   private Stream<ConsumerPartitionVO> getConsumerPartitionStream(String groupId,
                                                                  String topicName,
                                                                  Optional<TopicVO> topicOpt)
   {
      ZKGroupTopicDirs groupTopicDirs = new ZKGroupTopicDirs(groupId, topicName);

      if (!topicOpt.isPresent() || !topicOpt.get().getName().equals(topicName))
      {
         topicOpt = getTopic(topicName);
      }

      if (topicOpt.isPresent())
      {
         TopicVO topic = topicOpt.get();

         Map<Integer, Long> consumerOffsets = getConsumerOffsets(groupId, topic);

         return topic.getPartitions().stream()
            .map(partition -> {
               int partitionId = partition.getId();

               final ConsumerPartitionVO consumerPartition = new ConsumerPartitionVO(groupId, topicName, partitionId);
               consumerPartition.setOwner(
                  Optional.ofNullable(
                     consumerTreeCache.getCurrentData(groupTopicDirs.consumerOwnerDir() + "/" + partitionId))
                     .map(data -> new String(data.getData()))
                     .orElse(null));

               consumerPartition.setOffset(consumerOffsets.getOrDefault(partitionId, -1L));

               final Optional<TopicPartitionVO> topicPartition = topic.getPartition(partitionId);
               consumerPartition.setSize(topicPartition.map(TopicPartitionVO::getSize).orElse(-1L));
               consumerPartition.setFirstOffset(topicPartition.map(TopicPartitionVO::getFirstOffset).orElse(-1L));

               return consumerPartition;
            });
      }
      else
      {
         return Stream.empty();
      }
   }

   private Map<Integer, Long> getConsumerOffsets(String groupId, TopicVO topic)
   {
      try
      {
         // Kafka doesn't really give us an indication of whether a consumer is
         // using Kafka or Zookeeper based offset tracking. So look up the offsets
         // for both and assume that the largest offset is the correct one.

         ForkJoinTask<Map<Integer, Long>> kafkaTask = threadPool.submit(
            () -> brokerChannel(offsetManagerBroker(groupId))
               .execute(channel -> getConsumerOffsets(channel, groupId, topic, false))
         );

         ForkJoinTask<Map<Integer, Long>> zookeeperTask = threadPool.submit(
            () -> brokerChannel(null).execute(channel -> getConsumerOffsets(channel, groupId, topic, true))
         );

         Map<Integer, Long> zookeeperOffsets = zookeeperTask.get();
         Map<Integer, Long> kafkaOffsets = kafkaTask.get();
         zookeeperOffsets.entrySet().stream()
            .forEach(entry -> kafkaOffsets.merge(entry.getKey(), entry.getValue(), Math::max));
         return kafkaOffsets;
      }
      catch (InterruptedException ex)
      {
         Thread.currentThread().interrupt();
         throw Throwables.propagate(ex);
      }
      catch (ExecutionException ex)
      {
         throw Throwables.propagate(ex.getCause());
      }
   }

   /**
    * Returns the map of partitionId to consumer offset for the given group and
    * topic. Uses the given blocking channel to execute the offset fetch request.
    *
    * @param channel          The channel to send requests on
    * @param groupId          Consumer group to use
    * @param topic            Topic to query
    * @param zookeeperOffsets If true, use a version of the API that retrieves
    *                         offsets from Zookeeper. Otherwise use a version
    *                         that pulls the offsets from Kafka itself.
    * @return Map where the key is partitionId and the value is the consumer
    * offset for that partition.
    */
   private Map<Integer, Long> getConsumerOffsets(BlockingChannel channel,
                                                 String groupId,
                                                 TopicVO topic,
                                                 boolean zookeeperOffsets)
   {

      final OffsetFetchRequest request = new OffsetFetchRequest(
         groupId,
         topic.getPartitions().stream()
            .map(p -> new TopicAndPartition(topic.getName(), p.getId()))
            .collect(Collectors.toList()),
         (short) (zookeeperOffsets ? 0 : 1), 0, // version 0 = zookeeper offsets, 1 = kafka offsets
         kafka.api.OffsetFetchRequest.DefaultClientId());

      LOG.debug("Sending consumer offset request: {}", request);

      channel.send(request.underlying());

      final kafka.api.OffsetFetchResponse underlyingResponse =
         kafka.api.OffsetFetchResponse.readFrom(channel.receive().buffer());

      LOG.debug("Received consumer offset response: {}", underlyingResponse);

      OffsetFetchResponse response = new OffsetFetchResponse(underlyingResponse);

      return response.offsets().entrySet().stream()
         .filter(entry -> entry.getValue().error() == ErrorMapping.NoError())
         .collect(Collectors.toMap(entry -> entry.getKey().partition(), entry -> entry.getValue().offset()));
   }

   /**
    * Returns the broker Id that is the offset coordinator for the given group id. If not found, returns null
    */
   private Integer offsetManagerBroker(String groupId)
   {
      return brokerChannel(null).execute(channel -> {
         final ConsumerMetadataRequest request =
            new ConsumerMetadataRequest(groupId, (short) 0, 0, ConsumerMetadataRequest.DefaultClientId());
         LOG.debug("Sending consumer metadata request: {}", request);
         channel.send(request);
         ConsumerMetadataResponse response = ConsumerMetadataResponse.readFrom(channel.receive().buffer());
         LOG.debug("Received consumer metadata response: {}", response);
         return (response.errorCode() == ErrorMapping.NoError()) ? response.coordinator().id() : null;
      });
   }

   private Map<Integer, Long> getTopicPartitionSizes(TopicVO topic)
   {
      return getTopicPartitionSizes(topic, kafka.api.OffsetRequest.LatestTime());
   }

   private Map<Integer, Long> getTopicPartitionSizes(TopicVO topic, long time)
   {
      try
      {
         PartitionOffsetRequestInfo requestInfo = new PartitionOffsetRequestInfo(time, 1);
         return threadPool.submit(() ->
               topic.getPartitions().stream()
                  .filter(p -> p.getLeader() != null)
                  .collect(Collectors.groupingBy(p -> p.getLeader().getId())) // Group partitions by leader broker id
                  .entrySet().parallelStream()
                  .map(entry -> {
                     final Integer brokerId = entry.getKey();
                     final List<TopicPartitionVO> brokerPartitions = entry.getValue();
                     try
                     {
                        // Get the size of the partitions for a topic from the leader.
                        final OffsetRequest offsetRequest = new OffsetRequest(
                           brokerPartitions.stream()
                              .collect(Collectors.toMap(
                                 partition -> new TopicAndPartition(topic.getName(), partition.getId()),
                                 partition -> requestInfo)),
                           (short) 0, "");
                        LOG.debug("Sending offset request: {}", offsetRequest);
                        final OffsetResponse offsetResponse =
                           brokerChannel(brokerId).execute(channel -> {
                              channel.send(offsetRequest.underlying());
                              final kafka.api.OffsetResponse underlyingResponse =
                                 kafka.api.OffsetResponse.readFrom(channel.receive().buffer());

                              LOG.debug("Received offset response: {}", underlyingResponse);

                              return new OffsetResponse(underlyingResponse);
                           });


                        // Build a map of partitionId -> topic size from the response
                        return brokerPartitions.stream()
                           .collect(Collectors.toMap(TopicPartitionVO::getId,
                                                     partition -> Optional.ofNullable(
                                                        offsetResponse.offsets(topic.getName(), partition.getId()))
                                                        .map(Arrays::stream)
                                                        .orElse(LongStream.empty())
                                                        .findFirst()
                                                        .orElse(-1L)));
                     }
                     catch (Exception ex)
                     {
                        LOG.error("Unable to get partition log size for topic {} partitions ({})",
                                  topic.getName(),
                                  brokerPartitions.stream()
                                     .map(TopicPartitionVO::getId)
                                     .map(String::valueOf)
                                     .collect(Collectors.joining(",")),
                                  ex);

                        // Map each partition to -1, indicating we got an error
                        return brokerPartitions.stream().collect(Collectors.toMap(TopicPartitionVO::getId, tp -> -1L));
                     }
                  })
                  .map(Map::entrySet)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .get();
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw Throwables.propagate(e);
      }
      catch (ExecutionException e)
      {
         throw Throwables.propagate(e.getCause());
      }
   }

   private class BrokerListener implements PathChildrenCacheListener
   {
      @Override
      public void childEvent(CuratorFramework framework, PathChildrenCacheEvent event) throws Exception
      {
         switch (event.getType())
         {
            case CHILD_REMOVED:
            {
               BrokerVO broker = removeBroker(brokerId(event.getData()));
               break;
            }

            case CHILD_ADDED:
            case CHILD_UPDATED:
            {
               addBroker(parseBroker(event.getData()));
               break;
            }

            case INITIALIZED:
            {
               brokerPathCache.getCurrentData().stream()
                  .map(BrokerListener.this::parseBroker)
                  .forEach(CuratorKafkaMonitor.this::addBroker);
               break;
            }
         }
         updateController();
      }

      private int brokerId(ChildData input)
      {
         return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
      }


      private BrokerVO parseBroker(ChildData input)
      {
         try
         {
            final BrokerVO broker = objectMapper.reader(BrokerVO.class).readValue(input.getData());
            broker.setId(brokerId(input));
            return broker;
         }
         catch (IOException e)
         {
            throw Throwables.propagate(e);
         }
      }
   }

}
