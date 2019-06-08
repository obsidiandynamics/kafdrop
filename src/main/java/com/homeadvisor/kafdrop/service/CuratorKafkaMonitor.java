/*
 * Copyright 2017 HomeAdvisor, Inc.
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

import com.fasterxml.jackson.databind.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.util.*;
import kafka.api.*;
import kafka.cluster.*;
import kafka.common.*;
import kafka.javaapi.ConsumerMetadataResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.network.*;
import kafka.utils.*;
import org.apache.commons.lang3.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.retry.backoff.*;
import org.springframework.retry.policy.*;
import org.springframework.retry.support.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

@Service
public class CuratorKafkaMonitor implements KafkaMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(CuratorKafkaMonitor.class);

  @Autowired
  private CuratorFramework curatorFramework;f

  @Autowired
  private ObjectMapper objectMapper;

  private PathChildrenCache brokerPathCache;
  private PathChildrenCache topicConfigPathCache;
  private TreeCache topicTreeCache;
  private TreeCache consumerTreeCache;
  private NodeCache controllerNodeCache;

  private int controllerId = -1;

  private Map<Integer, BrokerVO> brokerCache = new TreeMap<>();

  private AtomicInteger cacheInitCounter = new AtomicInteger();

  private ForkJoinPool threadPool;

  @Autowired
  private CuratorKafkaMonitorProperties properties;
  @Autowired
  private KafkaHighLevelConsumer kafkaHighLevelConsumer;
  @Autowired
  private KafkaHighLevelAdminClient kafkaHighLevelAdminClient;

  private Version kafkaVersion;

  private RetryTemplate retryTemplate;

  @PostConstruct
  public void start()
  throws Exception {
    try {
      kafkaVersion = new Version(properties.getKafkaVersion());
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid kafka version: " + properties.getKafkaVersion(), ex);
    }

    threadPool = new ForkJoinPool(properties.getThreadPoolSize());

    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(properties.getRetry().getBackoffMillis());

    final SimpleRetryPolicy retryPolicy =
        new SimpleRetryPolicy(properties.getRetry().getMaxAttempts(),
                              ImmutableMap.of(InterruptedException.class, false,
                                              Exception.class, true));

    retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.setRetryPolicy(retryPolicy);

    cacheInitCounter.set(4);

    brokerPathCache = new PathChildrenCache(curatorFramework, ZkUtils.BrokerIdsPath(), true);
    brokerPathCache.getListenable().addListener(new BrokerListener());
    brokerPathCache.getListenable().addListener((f, e) -> {
      if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
        cacheInitCounter.decrementAndGet();
        LOG.info("Broker cache initialized");
      }
    });
    brokerPathCache.start(StartMode.POST_INITIALIZED_EVENT);

    topicConfigPathCache = new PathChildrenCache(curatorFramework, ZkUtils.TopicConfigPath(), true);
    topicConfigPathCache.getListenable().addListener((f, e) -> {
      if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
        cacheInitCounter.decrementAndGet();
        LOG.info("Topic configuration cache initialized");
      }
    });
    topicConfigPathCache.start(StartMode.POST_INITIALIZED_EVENT);

    topicTreeCache = new TreeCache(curatorFramework, ZkUtils.BrokerTopicsPath());
    topicTreeCache.getListenable().addListener((client, event) -> {
      if (event.getType() == TreeCacheEvent.Type.INITIALIZED) {
        cacheInitCounter.decrementAndGet();
        LOG.info("Topic tree cache initialized");
      }
    });
    topicTreeCache.start();

    consumerTreeCache = new TreeCache(curatorFramework, ZkUtils.ConsumersPath());
    consumerTreeCache.getListenable().addListener((client, event) -> {
      if (event.getType() == TreeCacheEvent.Type.INITIALIZED) {
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

  private String clientId() {
    return properties.getClientId();
  }

  @Override
  public Version getKafkaVersion() {
    return kafkaVersion;
  }

  private void updateController() {
    Optional.ofNullable(controllerNodeCache.getCurrentData())
        .map(data -> {
          try {
            Map controllerData = objectMapper.reader(Map.class).readValue(data.getData());
            return (Integer) controllerData.get("brokerid");
          } catch (IOException e) {
            LOG.error("Unable to read controller data", e);
            return null;
          }
        })
        .ifPresent(this::updateController);
  }

  private void updateController(int brokerId) {
    brokerCache.values()
        .forEach(broker -> broker.setController(broker.getId() == brokerId));
  }

  private void validateInitialized() {
    if (cacheInitCounter.get() > 0) {
      throw new NotInitializedException();
    }
  }

  @PreDestroy
  public void stop()
  throws IOException {
    consumerTreeCache.close();
    topicConfigPathCache.close();
    brokerPathCache.close();
    controllerNodeCache.close();
  }

  private int brokerId(ChildData input) {
    return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
  }

  private BrokerVO addBroker(BrokerVO broker) {
    final BrokerVO oldBroker = brokerCache.put(broker.getId(), broker);
    LOG.info("Kafka broker {} was {}", broker.getId(), oldBroker == null ? "added" : "updated");
    return oldBroker;
  }

  private BrokerVO removeBroker(int brokerId) {
    final BrokerVO broker = brokerCache.remove(brokerId);
    LOG.info("Kafka broker {} was removed", broker.getId());
    return broker;
  }

  @Override
  public List<BrokerVO> getBrokers() {
    validateInitialized();
    return brokerCache.values().stream().collect(Collectors.toList());
  }

  @Override
  public Optional<BrokerVO> getBroker(int id) {
    validateInitialized();
    return Optional.ofNullable(brokerCache.get(id));
  }

  private BrokerChannel brokerChannel(Integer brokerId) {
    if (brokerId == null) {
      brokerId = randomBroker();
      if (brokerId == null) {
        throw new BrokerNotFoundException("No brokers available to select from");
      }
    }

    Integer finalBrokerId = brokerId;
    BrokerVO broker = getBroker(brokerId)
        .orElseThrow(() -> new BrokerNotFoundException("Broker " + finalBrokerId + " is not available"));

    return BrokerChannel.forBroker(broker.getHost(), broker.getPort());
  }

  private Integer randomBroker() {
    if (brokerCache.size() > 0) {
      List<Integer> brokerIds = brokerCache.keySet().stream().collect(Collectors.toList());
      Collections.shuffle(brokerIds);
      return brokerIds.get(0);
    } else {
      return null;
    }
  }

  public ClusterSummaryVO getClusterSummary() {
    return getClusterSummary(getTopics());
  }

  @Override
  public ClusterSummaryVO getClusterSummary(Collection<TopicVO> topics) {
    final ClusterSummaryVO topicSummary = topics.stream()
        .map(topic -> {
          ClusterSummaryVO summary = new ClusterSummaryVO();
          summary.setPartitionCount(topic.getPartitions().size());
          summary.setUnderReplicatedCount(topic.getUnderReplicatedPartitions().size());
          summary.setPreferredReplicaPercent(topic.getPreferredReplicaPercent());
          topic.getPartitions()
              .forEach(partition -> {
                if (partition.getLeader() != null) {
                  summary.addBrokerLeaderPartition(partition.getLeader().getId());
                }
                if (partition.getPreferredLeader() != null) {
                  summary.addBrokerPreferredLeaderPartition(partition.getPreferredLeader().getId());
                }
                partition.getReplicas()
                    .forEach(replica -> summary.addExpectedBrokerId(replica.getId()));
              });
          return summary;
        })
        .reduce((s1, s2) -> {
          s1.setPartitionCount(s1.getPartitionCount() + s2.getPartitionCount());
          s1.setUnderReplicatedCount(s1.getUnderReplicatedCount() + s2.getUnderReplicatedCount());
          s1.setPreferredReplicaPercent(s1.getPreferredReplicaPercent() + s2.getPreferredReplicaPercent());
          s2.getBrokerLeaderPartitionCount().forEach(s1::addBrokerLeaderPartition);
          s2.getBrokerPreferredLeaderPartitionCount().forEach(s1::addBrokerPreferredLeaderPartition);
          return s1;
        })
        .orElseGet(ClusterSummaryVO::new);
    topicSummary.setTopicCount(topics.size());
    topicSummary.setPreferredReplicaPercent(topicSummary.getPreferredReplicaPercent() / topics.size());
    return topicSummary;
  }

  @Override
  public List<TopicVO> getTopics() {
    validateInitialized();
    return getTopicMetadata().values().stream()
        .sorted(Comparator.comparing(TopicVO::getName))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<TopicVO> getTopic(String topic) {
    validateInitialized();
    final Optional<TopicVO> topicVO = Optional.ofNullable(getTopicMetadata(topic).get(topic));
    if (kafkaVersion.compareTo(new Version(0, 8, 2)) > 0) {
      topicVO.ifPresent(vo -> {
        vo.setPartitions(getTopicPartitionSizes(vo));
      });
    } else {
      topicVO.ifPresent(
          vo -> {
            getTopicPartitionSizes(vo, kafka.api.OffsetRequest.LatestTime())
                .entrySet()
                .forEach(entry -> vo.getPartition(entry.getKey()).ifPresent(p -> p.setSize(entry.getValue())));
            getTopicPartitionSizes(vo, kafka.api.OffsetRequest.EarliestTime())
                .entrySet()
                .forEach(entry -> vo.getPartition(entry.getKey()).ifPresent(p -> p.setFirstOffset(entry.getValue())));
          }
      );
    }
    return topicVO;
  }

  private Map<String, TopicVO> getTopicMetadata(String... topics) {
    if (kafkaVersion.compareTo(new Version(0, 9, 0)) >= 0) {
      return retryTemplate.execute(
          context -> brokerChannel(null)
              .execute(channel -> getTopicMetadata(channel, topics)));
    } else {
      Stream<String> topicStream;
      if (topics == null || topics.length == 0) {
        topicStream =
            Optional.ofNullable(
                topicTreeCache.getCurrentChildren(ZkUtils.BrokerTopicsPath()))
                .map(Map::keySet)
                .map(Collection::stream)
                .orElse(Stream.empty());
      } else {
        topicStream = Stream.of(topics);
      }

      return topicStream
          .map(this::getTopicZkData)
          .filter(Objects::nonNull)
          .collect(Collectors.toMap(TopicVO::getName, topic -> topic));
    }
  }

  private TopicVO getTopicZkData(String topic) {
    return Optional.ofNullable(topicTreeCache.getCurrentData(ZkUtils.getTopicPath(topic)))
        .map(this::parseZkTopic)
        .orElse(null);
  }

  public TopicVO parseZkTopic(ChildData input) {
    try {
      final TopicVO topic = new TopicVO(StringUtils.substringAfterLast(input.getPath(), "/"));

      final TopicRegistrationVO topicRegistration =
          objectMapper.reader(TopicRegistrationVO.class).readValue(input.getData());

      topic.setConfig(
          Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
              .map(this::readTopicConfig)
              .orElse(Collections.emptyMap()));

      for (Map.Entry<Integer, List<Integer>> entry : topicRegistration.getReplicas().entrySet()) {
        final int partitionId = entry.getKey();
        final List<Integer> partitionBrokerIds = entry.getValue();

        final TopicPartitionVO partition = new TopicPartitionVO(partitionId);

        final Optional<TopicPartitionStateVO> partitionState = partitionState(topic.getName(), partition.getId());

        partitionBrokerIds.stream()
            .map(brokerId -> {
              TopicPartitionVO.PartitionReplica replica = new TopicPartitionVO.PartitionReplica();
              replica.setId(brokerId);
              replica.setInService(partitionState.map(ps -> ps.getIsr().contains(brokerId)).orElse(false));
              replica.setLeader(partitionState.map(ps -> brokerId == ps.getLeader()).orElse(false));
              return replica;
            })
            .forEach(partition::addReplica);

        topic.addPartition(partition);
      }

      // todo: get partition sizes here as single bulk request?

      return topic;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Map<String, TopicVO> getTopicMetadata(BlockingChannel channel, String... topics) {
    return kafkaHighLevelConsumer.getTopicsInfo(topics);
  }

  private TopicVO processTopicMetadata(TopicMetadata tmd) {
    TopicVO topic = new TopicVO(tmd.topic());

    topic.setConfig(
        Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
            .map(this::readTopicConfig)
            .orElse(Collections.emptyMap()));

    topic.setPartitions(
        tmd.partitionsMetadata().stream()
            .map((pmd) -> parsePartitionMetadata(tmd.topic(), pmd))
            .collect(Collectors.toMap(TopicPartitionVO::getId, p -> p))
    );
    return topic;
  }

  private TopicPartitionVO parsePartitionMetadata(String topic, PartitionMetadata pmd) {
    TopicPartitionVO partition = new TopicPartitionVO(pmd.partitionId());
    if (pmd.leader() != null) {
      partition.addReplica(new TopicPartitionVO.PartitionReplica(pmd.leader().id(), true, true));
    }

    final List<Integer> isr = getIsr(topic, pmd);
    pmd.replicas().stream()
        .map(replica -> new TopicPartitionVO.PartitionReplica(replica.id(), isr.contains(replica.id()), false))
        .forEach(partition::addReplica);
    return partition;
  }

  private List<Integer> getIsr(String topic, PartitionMetadata pmd) {
    return pmd.isr().stream().map(Broker::id).collect(Collectors.toList());
  }

  private Map<String, Object> readTopicConfig(ChildData d) {
    try {
      final Map<String, Object> configData = objectMapper.reader(Map.class).readValue(d.getData());
      return (Map<String, Object>) configData.get("config");
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Optional<TopicPartitionStateVO> partitionState(String topicName, int partitionId)
  throws IOException {
    final Optional<byte[]> partitionData = Optional.ofNullable(topicTreeCache.getCurrentData(
        ZkUtils.getTopicPartitionLeaderAndIsrPath(topicName, partitionId)))
        .map(ChildData::getData);
    if (partitionData.isPresent()) {
      return Optional.ofNullable(objectMapper.reader(TopicPartitionStateVO.class).readValue(partitionData.get()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public List<MessageVO> getMessages(TopicPartition topicPartition, long offset, long count) {
    List<ConsumerRecord<String, String>> records = kafkaHighLevelConsumer.getLatestRecords(topicPartition, offset, count);
    if (records != null) {
      List<MessageVO> messageVOS = Lists.newArrayList();
      for (ConsumerRecord<String, String> record : records) {
        MessageVO messageVo = new MessageVO();
        messageVo.setKey(record.key());
        messageVo.setMessage(record.value());
        messageVo.setHeaders(Arrays.toString(record.headers().toArray()));

        messageVOS.add(messageVo);
      }
      return messageVOS;
    } else {
      return Collections.emptyList();
    }
  }

  private Map<Integer, TopicPartitionVO> getTopicPartitionSizes(TopicVO topic) {
    return kafkaHighLevelConsumer.getPartitionSize(topic.getName());
  }

  private Map<Integer, Long> getTopicPartitionSizes(TopicVO topic, long time) {
    try {
      PartitionOffsetRequestInfo requestInfo = new PartitionOffsetRequestInfo(time, 1);

      return threadPool.submit(() ->
                                   topic.getPartitions().parallelStream()
                                       .filter(p -> p.getLeader() != null)
                                       .collect(Collectors.groupingBy(p -> p.getLeader().getId())) // Group partitions by leader broker id
                                       .entrySet().parallelStream()
                                       .map(entry -> {
                                         final Integer brokerId = entry.getKey();
                                         final List<TopicPartitionVO> brokerPartitions = entry.getValue();
                                         try {
                                           // Get the size of the partitions for a topic from the leader.
                                           final OffsetResponse offsetResponse =
                                               sendOffsetRequest(brokerId, topic, requestInfo, brokerPartitions);

                                           // Build a map of partitionId -> topic size from the response
                                           return brokerPartitions.stream()
                                               .collect(Collectors.toMap(TopicPartitionVO::getId,
                                                                         partition -> Optional.ofNullable(
                                                                             offsetResponse.offsets(topic.getName(), partition.getId()))
                                                                             .map(Arrays::stream)
                                                                             .orElse(LongStream.empty())
                                                                             .findFirst()
                                                                             .orElse(-1L)));
                                         } catch (Exception ex) {
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
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw Throwables.propagate(e);
    } catch (ExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  private OffsetResponse sendOffsetRequest(Integer brokerId, TopicVO topic,
                                           PartitionOffsetRequestInfo requestInfo,
                                           List<TopicPartitionVO> brokerPartitions) {
    final OffsetRequest offsetRequest = new OffsetRequest(
        brokerPartitions.stream()
            .collect(Collectors.toMap(
                partition -> new TopicAndPartition(topic.getName(), partition.getId()),
                partition -> requestInfo)),
        (short) 0, clientId());

    LOG.debug("Sending offset request: {}", offsetRequest);

    return retryTemplate.execute(
        context ->
            brokerChannel(brokerId)
                .execute(channel ->
                         {
                           channel.send(offsetRequest.underlying());
                           final kafka.api.OffsetResponse underlyingResponse =
                               kafka.api.OffsetResponse.readFrom(channel.receive().buffer());

                           LOG.debug("Received offset response: {}", underlyingResponse);

                           return new OffsetResponse(underlyingResponse);
                         }));
  }

  private class BrokerListener implements PathChildrenCacheListener {
    @Override
    public void childEvent(CuratorFramework framework, PathChildrenCacheEvent event)
    throws Exception {
      switch (event.getType()) {
        case CHILD_REMOVED: {
          BrokerVO broker = removeBroker(brokerId(event.getData()));
          break;
        }

        case CHILD_ADDED:
        case CHILD_UPDATED: {
          addBroker(parseBroker(event.getData()));
          break;
        }

        case INITIALIZED: {
          brokerPathCache.getCurrentData().stream()
              .map(BrokerListener.this::parseBroker)
              .forEach(CuratorKafkaMonitor.this::addBroker);
          break;
        }
      }
      updateController();
    }

    private int brokerId(ChildData input) {
      return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
    }

    private BrokerVO parseBroker(ChildData input) {
      try {
        final BrokerVO broker = objectMapper.reader(BrokerVO.class).readValue(input.getData());
        broker.setId(brokerId(input));
        return broker;
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
