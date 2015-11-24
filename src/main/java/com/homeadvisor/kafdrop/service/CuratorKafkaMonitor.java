package com.homeadvisor.kafdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.homeadvisor.kafdrop.model.*;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.consumer.SimpleConsumer;
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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
   private TreeCache topicTreeCache;
   private TreeCache consumerTreeCache;

   private Map<Integer, BrokerVO> brokerCache = new TreeMap<>();

   private Map<Integer, SimpleConsumer> consumerMap = new ConcurrentHashMap<>();

   private AtomicInteger cacheInitCounter = new AtomicInteger();

   @PostConstruct
   public void start() throws Exception
   {
      cacheInitCounter.set(4);

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

      topicTreeCache = new TreeCache(curatorFramework, ZkUtils.BrokerTopicsPath());
      topicTreeCache.getListenable().addListener((client, event) -> {
         if (event.getType() == TreeCacheEvent.Type.INITIALIZED)
         {
            cacheInitCounter.decrementAndGet();
            LOG.info("Topic tree cache initialized");
         }
      });
      topicTreeCache.start();

      consumerTreeCache = new TreeCache(curatorFramework, ZkUtils.ConsumersPath());
      consumerTreeCache.getListenable().addListener((client, event) -> {
         if (event.getType() == TreeCacheEvent.Type.INITIALIZED)
         {
            cacheInitCounter.decrementAndGet();
            LOG.info("Consumer tree cache initialized");
         }
      });
      consumerTreeCache.start();
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
      topicTreeCache.close();
      topicConfigPathCache.close();
      brokerPathCache.close();
   }

   private SimpleConsumer getSimpleConsumer(int brokerId) throws BrokerNotFoundException
   {
      return consumerMap.computeIfAbsent(brokerId, id -> createSimpleConsumer(id).orElseThrow(BrokerNotFoundException::new));
   }

   private void removeSimpleConsumer(int brokerId)
   {
      try
      {
         Optional.ofNullable(consumerMap.remove(brokerId)).ifPresent(SimpleConsumer::close);
      }
      catch (Exception ex)
      {
      }
   }

   private Optional<SimpleConsumer> createSimpleConsumer(int brokerId)
   {
      return Optional.ofNullable(brokerCache.get(brokerId))
         .map(b -> new SimpleConsumer(b.getHost(), b.getPort(), 10000, 100000, "kafka-watch"));
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

   private BrokerVO removeBroker(PathChildrenCacheEvent event)
   {
      final BrokerVO broker = brokerCache.remove(brokerId(event.getData()));
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

   @Override
   public List<TopicVO> getTopics()
   {
      validateInitialized();
      return topicTreeCache.getCurrentChildren(ZkUtils.BrokerTopicsPath()).values().stream()
         .map(this::parseTopic)
         .sorted(Comparator.comparing(TopicVO::getName))
         .collect(Collectors.toList());
   }

   @Override
   public Optional<TopicVO> getTopic(String topic)
   {
      validateInitialized();
      final Optional<TopicVO> topicVO = getTopicData(topic).map(this::parseTopic);
      topicVO.ifPresent(
         vo ->
            getTopicPartitionSizes(vo)
               .entrySet().stream()
               .forEach(entry -> vo.getPartition(entry.getKey()).ifPresent(p -> p.setSize(entry.getValue()))));
      return topicVO;
   }

   public TopicVO parseTopic(ChildData input)
   {
      try
      {
         final TopicVO topic = new TopicVO(StringUtils.substringAfterLast(input.getPath(), "/"));

         final TopicRegistrationVO topicRegistration =
            objectMapper.reader(TopicRegistrationVO.class).readValue(input.getData());

         topic.setConfig(
            Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
               .map(this::readTopicConfig)
               .orElse(Collections.emptyMap()));

         for (Map.Entry<Integer, List<Integer>> entry : topicRegistration.getReplicas().entrySet())
         {
            final int partitionId = entry.getKey();
            final List<Integer> partitionBrokerIds = entry.getValue();

            final TopicPartitionVO partition = new TopicPartitionVO(partitionId);

            final TopicPartitionStateVO partitionState = partitionState(topic.getName(), partition.getId());

            partitionBrokerIds.stream()
               .map(brokerId -> {
                  TopicPartitionVO.PartitionReplica replica = new TopicPartitionVO.PartitionReplica();
                  replica.setId(brokerId);
                  replica.setInService(partitionState.getIsr().contains(brokerId));
                  replica.setLeader(brokerId == partitionState.getLeader());
                  return replica;
               })
               .forEach(partition::addReplica);

            topic.addPartition(partition);
         }

         // todo: get partition sizes here as single bulk request?

         return topic;
      }
      catch (IOException e)
      {
         throw Throwables.propagate(e);
      }
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


   private TopicPartitionStateVO partitionState(String topicName, int partitionId)
      throws IOException
   {
      return objectMapper.reader(TopicPartitionStateVO.class).readValue(
         topicTreeCache.getCurrentData(
            ZkUtils.getTopicPartitionLeaderAndIsrPath(topicName, partitionId))
            .getData());
   }

   private Optional<ChildData> getTopicData(String topic)
   {
      return Optional.ofNullable(topicTreeCache.getCurrentData(ZkUtils.getTopicPath(topic)));
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
         vo -> (consumerTreeCache.getCurrentData(groupDirs.consumerGroupDir() + "/offsets/" + vo.getName()) != null)
            ? Stream.of(vo.getName()) : Stream.<String>empty())
         .orElse(
            Optional.ofNullable(
               consumerTreeCache.getCurrentChildren(groupDirs.consumerGroupDir() + "/offsets"))
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

         return topic.getPartitions().stream()
            .map(partition -> {
               int partitionId = partition.getId();

               final ConsumerPartitionVO consumerPartition = new ConsumerPartitionVO(groupId, topicName, partitionId);
               consumerPartition.setOwner(
                  Optional.ofNullable(
                     consumerTreeCache.getCurrentData(groupTopicDirs.consumerOwnerDir() + "/" + partitionId))
                     .map(data -> new String(data.getData()))
                     .orElse(null));
               consumerPartition.setOffset(
                  Optional.ofNullable(
                     consumerTreeCache.getCurrentData(groupTopicDirs.consumerOffsetDir() + "/" + partitionId))
                     .map(d -> Long.parseLong(new String(d.getData())))
                     .orElse(-1L));
               consumerPartition.setSize(topic.getPartition(partitionId).map(TopicPartitionVO::getSize).orElse(-1L));

               return consumerPartition;
            });
      }
      else
      {
         return Stream.empty();
      }
   }

   private Map<Integer, Long> getTopicPartitionSizes(TopicVO topic)
   {
      PartitionOffsetRequestInfo requestInfo = new PartitionOffsetRequestInfo(kafka.api.OffsetRequest.LatestTime(), 1);

      return topic.getPartitions().stream()
         .collect(Collectors.groupingBy(p -> p.getLeader().getId())) // Group partitions by their leader broker id
         .entrySet().parallelStream()
         .map(entry -> {
            final Integer brokerId = entry.getKey();
            final List<TopicPartitionVO> brokerPartitions = entry.getValue();
            try
            {
               // Get the size of the partitions by making a request for all partitions for a topic to the leader.
               final OffsetResponse response =
                  getSimpleConsumer(brokerId).getOffsetsBefore(
                     new OffsetRequest(
                        brokerPartitions.stream()
                           .collect(Collectors.toMap(
                              partition -> new TopicAndPartition(topic.getName(), partition.getId()),
                              partition -> requestInfo)),
                        (short) 1, ""));

               // Build a map of partitionId -> topic size from the response
               return brokerPartitions.stream()
                  .collect(Collectors.toMap(TopicPartitionVO::getId,
                                            partition -> Optional.ofNullable(response.offsets(topic.getName(), partition.getId()))
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
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
               BrokerVO broker = removeBroker(event);
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
                  .map(CuratorKafkaMonitor.this::parseBroker)
                  .forEach(CuratorKafkaMonitor.this::addBroker);
               break;
            }
         }
      }

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
