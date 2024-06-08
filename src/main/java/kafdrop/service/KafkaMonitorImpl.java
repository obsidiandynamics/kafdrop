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

import kafdrop.model.AclVO;
import kafdrop.model.BrokerVO;
import kafdrop.model.ClusterSummaryVO;
import kafdrop.model.ConsumerPartitionVO;
import kafdrop.model.ConsumerTopicVO;
import kafdrop.model.ConsumerVO;
import kafdrop.model.CreateTopicVO;
import kafdrop.model.MessageVO;
import kafdrop.model.SearchResultsVO;
import kafdrop.model.TopicPartitionVO;
import kafdrop.model.TopicVO;
import kafdrop.util.Deserializers;
import org.apache.kafka.clients.admin.ConfigEntry.ConfigSource;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
public final class KafkaMonitorImpl implements KafkaMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaMonitorImpl.class);

  private final KafkaHighLevelConsumer highLevelConsumer;

  private final KafkaHighLevelAdminClient highLevelAdminClient;

  public KafkaMonitorImpl(KafkaHighLevelConsumer highLevelConsumer, KafkaHighLevelAdminClient highLevelAdminClient) {
    this.highLevelConsumer = highLevelConsumer;
    this.highLevelAdminClient = highLevelAdminClient;
  }

  @Override
  public List<BrokerVO> getBrokers() {
    final var clusterDescription = highLevelAdminClient.describeCluster();
    final var brokerVos = new ArrayList<BrokerVO>(clusterDescription.nodes.size());
    for (var node : clusterDescription.nodes) {
      final var isController = node.id() == clusterDescription.controller.id();
      brokerVos.add(new BrokerVO(node.id(), node.host(), node.port(), node.rack(), isController));
    }
    return brokerVos;
  }

  @Override
  public Optional<BrokerVO> getBroker(int id) {
    return getBrokers().stream().filter(brokerVo -> brokerVo.getId() == id).findAny();
  }

  @Override
  public ClusterSummaryVO getClusterSummary(Collection<TopicVO> topics) {
    final var topicSummary = topics.stream()
      .map(topic -> {
        final var summary = new ClusterSummaryVO();
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
    topicSummary.setPreferredReplicaPercent(topics.isEmpty() ? 0 :
      topicSummary.getPreferredReplicaPercent() / topics.size());
    return topicSummary;
  }

  @Override
  public List<TopicVO> getTopics() {
    return getTopicMetadata(highLevelConsumer.getAllTopics()).values().stream()
      .sorted(Comparator.comparing(TopicVO::getName))
      .collect(Collectors.toList());
  }

  public List<TopicVO> getTopics(String[] topics) {
    Map<String, List<PartitionInfo>> topicsMap = highLevelConsumer.getAllTopics();

    ArrayList<TopicVO> topicVos = new ArrayList<>(getTopicMetadata(topicsMap, topics).values());
    setTopicPartitionSizes(topicVos);

    return topicVos;
  }

  @Override
  public Optional<TopicVO> getTopic(String topic) {
    String[] topics = {topic};

    return getTopics(topics).stream().findAny();
  }

  private Map<String, TopicVO> getTopicMetadata(Map<String, List<PartitionInfo>> allTopicsMap, String... topics) {
    final var topicInfos = highLevelConsumer.getTopicInfos(allTopicsMap, topics);
    final var retrievedTopicNames = topicInfos.keySet();
    final var topicConfigs = highLevelAdminClient.describeTopicConfigs(retrievedTopicNames);

    for (var topicVo : topicInfos.values()) {
      final var config = topicConfigs.get(topicVo.getName());
      if (config != null) {
        final var configMap = new TreeMap<String, String>();
        for (var configEntry : config.entries()) {
          if (configEntry.source() != ConfigSource.DEFAULT_CONFIG &&
            configEntry.source() != ConfigSource.STATIC_BROKER_CONFIG) {
            configMap.put(configEntry.name(), configEntry.value());
          }
        }
        topicVo.setConfig(configMap);
      } else {
        LOG.warn("Missing config for topic {}", topicVo.getName());
      }
    }
    return topicInfos;
  }

  @Override
  public List<MessageVO> getMessages(String topic, int count,
                                     Deserializers deserializers) {
    final var records = highLevelConsumer.getLatestRecords(topic, count, deserializers);
    if (records != null) {
      final var messageVos = new ArrayList<MessageVO>();
      for (var rec : records) {
        final var messageVo = new MessageVO();
        messageVo.setPartition(rec.partition());
        messageVo.setOffset(rec.offset());
        messageVo.setKey(rec.key());
        messageVo.setMessage(rec.value());
        messageVo.setHeaders(headersToMap(rec.headers()));
        messageVo.setTimestamp(new Date(rec.timestamp()));
        messageVos.add(messageVo);
      }
      return messageVos;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<MessageVO> getMessages(TopicPartition topicPartition, long offset, int count,
                                     Deserializers deserializers) {
    final var records = highLevelConsumer.getLatestRecords(topicPartition, offset, count, deserializers);
    if (records != null) {
      final var messageVos = new ArrayList<MessageVO>();
      for (var rec : records) {
        final var messageVo = new MessageVO();
        messageVo.setPartition(topicPartition.partition());
        messageVo.setOffset(rec.offset());
        messageVo.setKey(rec.key());
        messageVo.setMessage(rec.value());
        messageVo.setHeaders(headersToMap(rec.headers()));
        messageVo.setTimestamp(new Date(rec.timestamp()));
        messageVos.add(messageVo);
      }
      return messageVos;
    } else {
      return Collections.emptyList();
    }
  }

  private static Map<String, String> headersToMap(Headers headers) {
    final var map = new TreeMap<String, String>();
    for (var header : headers) {
      final var value = header.value();
      map.put(header.key(), (value == null) ? null : new String(value));
    }
    return map;
  }

  private void setTopicPartitionSizes(List<TopicVO> topics) {
    highLevelConsumer.setTopicPartitionSizes(topics);
  }

  public List<ConsumerVO> getConsumersByGroup(String groupId) {
    List<ConsumerGroupOffsets> consumerGroupOffsets = getConsumerOffsets(groupId);

    String[] uniqueTopicNames = consumerGroupOffsets.stream()
      .flatMap(consumerGroupOffset -> consumerGroupOffset.offsets.keySet()
        .stream().map(TopicPartition::topic))
      .distinct()
      .toArray(String[]::new);

    List<TopicVO> topicVOs = getTopics(uniqueTopicNames);

    LOG.debug("consumerGroupOffsets: {}", consumerGroupOffsets);
    LOG.debug("topicVos: {}", topicVOs);
    return convert(consumerGroupOffsets, topicVOs);
  }

  @Override
  public List<ConsumerVO> getConsumersByTopics(Collection<TopicVO> topicVos) {
    final var topics = topicVos.stream().map(TopicVO::getName).collect(Collectors.toSet());
    final var consumerGroupOffsets = getConsumerOffsets(topics);
    LOG.debug("consumerGroupOffsets: {}", consumerGroupOffsets);
    LOG.debug("topicVos: {}", topicVos);
    return convert(consumerGroupOffsets, topicVos);
  }

  @Override
  public SearchResultsVO searchMessages(String topic,
                                        String searchString,
                                        Integer maxmuimCount,
                                        Date startTimestamp,
                                        Deserializers deserializers) {
    final var records = highLevelConsumer.searchRecords(topic, searchString, maxmuimCount, startTimestamp,
      deserializers);
    final var results = new SearchResultsVO();

    if (records != null) {
      final var messageVos = new ArrayList<MessageVO>();
      results.setMessages(messageVos);

      for (var record : records.getResults()) {
        final var messageVo = new MessageVO();
        messageVo.setPartition(record.partition());
        messageVo.setOffset(record.offset());
        messageVo.setKey(record.key());
        messageVo.setMessage(record.value());
        messageVo.setHeaders(headersToMap(record.headers()));
        messageVo.setTimestamp(new Date(record.timestamp()));
        messageVos.add(messageVo);
      }

      switch (records.getCompletionReason()) {
        case FOUND_REQUESTED_NUMBER_OF_RESULTS:
          results.setCompletionDetails(String.format("Search completed after finding requested number of results.  " +
            "Scanned %d messages.", records.getMessagesScannedCount()));
          break;
        case EXCEEDED_MAX_SCAN_COUNT:
          results.setCompletionDetails(
            String.format(
              "Search timed out after scanning %d messages. Last scanned message timestamp was %2$tF %2$tT." +
                " Adjust your time span for more results.",
              records.getMessagesScannedCount(),
              records.getFinalMessageTimestamp()));
          break;
        case NO_MORE_MESSAGES_IN_TOPIC:
          results.setCompletionDetails(
            String.format(
              "Search reached the end of the topic before finding requested number of results.  Scanned %d messages.",
              records.getMessagesScannedCount()));
          break;
      }

    } else {
      results.setCompletionDetails("Unknown error");
      results.setMessages(Collections.emptyList());
    }

    return results;
  }

  @Override
  public void createTopic(CreateTopicVO createTopicDto) {
    var newTopic = new NewTopic(
      createTopicDto.getName(), createTopicDto.getPartitionsNumber(), (short) createTopicDto.getReplicationFactor()
    );
    highLevelAdminClient.createTopic(newTopic);
  }

  @Override
  public void deleteTopic(String topic) {
    highLevelAdminClient.deleteTopic(topic);
  }

  @Override
  public List<AclVO> getAcls() {
    final var acls = highLevelAdminClient.listAcls();
    final var aclVos = new ArrayList<AclVO>(acls.size());
    for (var acl : acls) {
      aclVos.add(new AclVO(acl.pattern().resourceType().toString(), acl.pattern().name(),
        acl.pattern().patternType().toString(), acl.entry().principal(),
        acl.entry().host(), acl.entry().operation().toString(),
        acl.entry().permissionType().toString()));
    }
    Collections.sort(aclVos);
    return aclVos;
  }

  private static List<ConsumerVO> convert(List<ConsumerGroupOffsets> consumerGroupOffsets,
                                          Collection<TopicVO> topicVos) {
    final var topicVoMap = topicVos.stream().collect(Collectors.toMap(TopicVO::getName, Function.identity()));
    final var groupTopicPartitionOffsetMap = new TreeMap<String, Map<String, Map<Integer, Long>>>();

    for (var consumerGroupOffset : consumerGroupOffsets) {
      final var groupId = consumerGroupOffset.groupId;

      for (var topicPartitionOffset : consumerGroupOffset.offsets.entrySet()) {
        final var topic = topicPartitionOffset.getKey().topic();
        final var partition = topicPartitionOffset.getKey().partition();
        final var offset = topicPartitionOffset.getValue().offset();
        groupTopicPartitionOffsetMap
          .computeIfAbsent(groupId, unused -> new TreeMap<>())
          .computeIfAbsent(topic, unused -> new TreeMap<>())
          .put(partition, offset);
      }
    }

    final var consumerVos = new ArrayList<ConsumerVO>(consumerGroupOffsets.size());
    for (var groupTopicPartitionOffset : groupTopicPartitionOffsetMap.entrySet()) {
      final var groupId = groupTopicPartitionOffset.getKey();
      final var consumerVo = new ConsumerVO(groupId);
      consumerVos.add(consumerVo);

      for (var topicPartitionOffset : groupTopicPartitionOffset.getValue().entrySet()) {
        final var topic = topicPartitionOffset.getKey();
        final var consumerTopicVo = new ConsumerTopicVO(topic);
        consumerVo.addTopic(consumerTopicVo);

        for (var partitionOffset : topicPartitionOffset.getValue().entrySet()) {
          final var partition = partitionOffset.getKey();
          final var offset = partitionOffset.getValue();
          final var offsetVo = new ConsumerPartitionVO(groupId, topic, partition);
          consumerTopicVo.addOffset(offsetVo);
          offsetVo.setOffset(offset);
          final var topicVo = topicVoMap.get(topic);
          final var topicPartitionVo = topicVo.getPartition(partition);
          offsetVo.setSize(topicPartitionVo.map(TopicPartitionVO::getSize).orElse(-1L));
          offsetVo.setFirstOffset(topicPartitionVo.map(TopicPartitionVO::getFirstOffset).orElse(-1L));
        }
      }
    }

    return consumerVos;
  }

  private static final class ConsumerGroupOffsets {
    final String groupId;
    final Map<TopicPartition, OffsetAndMetadata> offsets;

    ConsumerGroupOffsets(String groupId, Map<TopicPartition, OffsetAndMetadata> offsets) {
      this.groupId = groupId;
      this.offsets = offsets;
    }

    boolean isEmpty() {
      return offsets.isEmpty();
    }

    ConsumerGroupOffsets forTopics(Set<String> topics) {
      final var filteredOffsets = offsets.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .filter(e -> topics.contains(e.getKey().topic()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      return new ConsumerGroupOffsets(groupId, filteredOffsets);
    }

    @Override
    public String toString() {
      return ConsumerGroupOffsets.class.getSimpleName() + " [groupId=" + groupId + ", offsets=" + offsets + "]";
    }
  }

  private ConsumerGroupOffsets resolveOffsets(String groupId) {
    return new ConsumerGroupOffsets(groupId, highLevelAdminClient.listConsumerGroupOffsetsIfAuthorized(groupId));
  }

  private List<ConsumerGroupOffsets> getConsumerOffsets(String groupId) {
    return Collections.singletonList(resolveOffsets(groupId));
  }

  private List<ConsumerGroupOffsets> getConsumerOffsets(Set<String> topics) {
    final var consumerGroups = highLevelAdminClient.listConsumerGroups();
    return consumerGroups.stream()
      .map(this::resolveOffsets)
      .map(offsets -> offsets.forTopics(topics))
      .filter(not(ConsumerGroupOffsets::isEmpty))
      .collect(Collectors.toList());
  }
}
