package kafdrop.service;

import kafdrop.model.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.function.Predicate.*;

@Service
public final class KafkaConsumerMonitor implements ConsumerMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerMonitor.class);

  private final KafkaHighLevelAdminClient highLevelAdminClient;

  public KafkaConsumerMonitor(KafkaHighLevelAdminClient highLevelAdminClient) {
    this.highLevelAdminClient = highLevelAdminClient;
  }

  @Override
  public List<ConsumerVO> getConsumers(Collection<TopicVO> topicVos) {
    final var topics = topicVos.stream().map(TopicVO::getName).collect(Collectors.toSet());
    final var consumerGroupOffsets = getConsumerOffsets(topics);
    LOG.debug("consumerGroupOffsets: {}", consumerGroupOffsets);
    LOG.debug("topicVos: {}", topicVos);
    return convert(consumerGroupOffsets, topicVos);
  }

  private static List<ConsumerVO> convert(List<ConsumerGroupOffsets> consumerGroupOffsets, Collection<TopicVO> topicVos) {
    final var topicVoMap = topicVos.stream().collect(Collectors.toMap(TopicVO::getName, Function.identity()));
    final var groupTopicPartitionOffsetMap = new TreeMap<String, Map<String, Map<Integer, Long>>>();

    for (var consumerGroupOffset : consumerGroupOffsets) {
      final var groupId = consumerGroupOffset.groupId;

      for (var topicPartitionOffset : consumerGroupOffset.offsets.entrySet()) {
        final var topic = topicPartitionOffset.getKey().topic();
        final var partition = topicPartitionOffset.getKey().partition();
        final var offset = topicPartitionOffset.getValue().offset();
        groupTopicPartitionOffsetMap
            .computeIfAbsent(groupId, __ -> new TreeMap<>())
            .computeIfAbsent(topic, __ -> new TreeMap<>())
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
    return new ConsumerGroupOffsets(groupId, highLevelAdminClient.listConsumerGroupOffsets(groupId));
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
