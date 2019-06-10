/*
 * Copyright 2017 Kafdrop contributors.
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

package kafdrop.service;

import com.fasterxml.jackson.databind.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import kafdrop.model.*;
import kafdrop.util.*;
import kafka.utils.*;
import org.apache.commons.lang3.*;
import org.apache.curator.framework.*;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.io.*;
import java.util.Optional;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

@Service
public class CuratorKafkaMonitor implements KafkaMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(CuratorKafkaMonitor.class);

  private final CuratorFramework curatorFramework;

  private final ObjectMapper objectMapper;

  private PathChildrenCache brokerPathCache;
  private PathChildrenCache topicConfigPathCache;
  private TreeCache consumerTreeCache;
  private NodeCache controllerNodeCache;

  private final Map<Integer, BrokerVO> brokerCache = new TreeMap<>();

  private final AtomicInteger cacheInitCounter = new AtomicInteger();

  private final KafkaHighLevelConsumer kafkaHighLevelConsumer;

  public CuratorKafkaMonitor(CuratorFramework curatorFramework, ObjectMapper objectMapper, CuratorKafkaMonitorProperties properties, KafkaHighLevelConsumer kafkaHighLevelConsumer) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
    this.kafkaHighLevelConsumer = kafkaHighLevelConsumer;
  }

  @PostConstruct
  public void start() throws Exception {
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

    final TreeCache topicTreeCache = new TreeCache(curatorFramework, ZkUtils.BrokerTopicsPath());
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

  private void updateController() {
    Optional.ofNullable(controllerNodeCache.getCurrentData())
        .map(data -> {
          try {
            final Map controllerData = objectMapper.readerFor(Map.class).readValue(data.getData());
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
  public void stop() throws IOException {
    consumerTreeCache.close();
    topicConfigPathCache.close();
    brokerPathCache.close();
    controllerNodeCache.close();
  }

  private void addBroker(BrokerVO broker) {
    final BrokerVO oldBroker = brokerCache.put(broker.getId(), broker);
    LOG.info("Kafka broker {} was {}", broker.getId(), oldBroker == null ? "added" : "updated");
  }

  private void removeBroker(int brokerId) {
    final BrokerVO broker = brokerCache.remove(brokerId);
    LOG.info("Kafka broker {} was removed", broker.getId());
  }

  @Override
  public List<BrokerVO> getBrokers() {
    validateInitialized();
    return new ArrayList<>(brokerCache.values());
  }

  @Override
  public Optional<BrokerVO> getBroker(int id) {
    validateInitialized();
    return Optional.ofNullable(brokerCache.get(id));
  }

  @Override
  public ClusterSummaryVO getClusterSummary(Collection<TopicVO> topics) {
    final ClusterSummaryVO topicSummary = topics.stream()
        .map(topic -> {
          final ClusterSummaryVO summary = new ClusterSummaryVO();
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
    topicSummary.setPreferredReplicaPercent(topics.isEmpty() ? 0 : topicSummary.getPreferredReplicaPercent() / topics.size());
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
    final Optional<TopicVO> topicVo = Optional.ofNullable(getTopicMetadata(topic).get(topic));
    topicVo.ifPresent(vo -> vo.setPartitions(getTopicPartitionSizes(vo)));
    return topicVo;
  }

  private Map<String, TopicVO> getTopicMetadata(String... topics) {
    return kafkaHighLevelConsumer.getTopicsInfo(topics);
  }

  @Override
  public List<MessageVO> getMessages(TopicPartition topicPartition, long offset, long count,
                                     MessageDeserializer deserializer) {
    final List<ConsumerRecord<String, String>> records =
        kafkaHighLevelConsumer.getLatestRecords(topicPartition, offset, count, deserializer);
    if (records != null) {
      final List<MessageVO> messageVos = Lists.newArrayList();
      for (ConsumerRecord<String, String> record : records) {
        final MessageVO messageVo = new MessageVO();
        messageVo.setKey(record.key());
        messageVo.setMessage(record.value());
        messageVo.setHeaders(Arrays.toString(record.headers().toArray()));
        messageVos.add(messageVo);
      }
      return messageVos;
    } else {
      return Collections.emptyList();
    }
  }

  private Map<Integer, TopicPartitionVO> getTopicPartitionSizes(TopicVO topic) {
    return kafkaHighLevelConsumer.getPartitionSize(topic.getName());
  }

  private final class BrokerListener implements PathChildrenCacheListener {
    @Override
    public void childEvent(CuratorFramework framework, PathChildrenCacheEvent event) {
      switch (event.getType()) {
        case CHILD_REMOVED: {
          removeBroker(brokerId(event.getData()));
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
        final BrokerVO broker = objectMapper.readerFor(BrokerVO.class).readValue(input.getData());
        broker.setId(brokerId(input));
        return broker;
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
