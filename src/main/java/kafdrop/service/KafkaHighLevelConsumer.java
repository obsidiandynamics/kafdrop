package kafdrop.service;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kafdrop.config.KafkaConfiguration;
import kafdrop.model.TopicPartitionVO;
import kafdrop.model.TopicVO;
import kafdrop.util.MessageDeserializer;

@Service
public final class KafkaHighLevelConsumer {
  private static final int POLL_TIMEOUT_MS = 200;

  private static final Logger LOG = LoggerFactory.getLogger(KafkaHighLevelConsumer.class);

  private KafkaConsumer<String, byte[]> kafkaConsumer;

  private final KafkaConfiguration kafkaConfiguration;

  public KafkaHighLevelConsumer(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  private void initializeClient() {
    if (kafkaConsumer == null) {
      final var properties = new Properties();
      properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
      properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
      properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
      properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafdrop-consumer");
      properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
      kafkaConfiguration.applyCommon(properties);

      kafkaConsumer = new KafkaConsumer<>(properties);
    }
  }

  synchronized Map<Integer, TopicPartitionVO> getPartitionSize(String topic) {
    initializeClient();

    final var partitionInfoSet = kafkaConsumer.partitionsFor(topic);
    kafkaConsumer.assign(partitionInfoSet.stream()
                             .map(partitionInfo -> new TopicPartition(partitionInfo.topic(),
                                                                      partitionInfo.partition()))
                             .collect(Collectors.toList()));

    kafkaConsumer.poll(Duration.ofMillis(0));
    final Set<TopicPartition> assignedPartitionList = kafkaConsumer.assignment();
    final TopicVO topicVO = getTopicInfo(topic);
    final Map<Integer, TopicPartitionVO> partitionsVo = topicVO.getPartitionMap();

    kafkaConsumer.seekToBeginning(assignedPartitionList);
    assignedPartitionList.forEach(topicPartition -> {
      final TopicPartitionVO topicPartitionVo = partitionsVo.get(topicPartition.partition());
      final TopicPartitionVO defaultPartitionVo = partitionsVo.get(-1);
      final long startOffset = kafkaConsumer.position(topicPartition);
      LOG.debug("topic: {}, partition: {}, startOffset: {}", topicPartition.topic(), topicPartition.partition(), startOffset);
      topicPartitionVo.setFirstOffset(startOffset);
      if (defaultPartitionVo.getFirstOffset() == -1)
        defaultPartitionVo.setFirstOffset(startOffset);
      else
        defaultPartitionVo.setFirstOffset(Math.min(startOffset, defaultPartitionVo.getFirstOffset()));
    });

    kafkaConsumer.seekToEnd(assignedPartitionList);
    assignedPartitionList.forEach(topicPartition -> {
      final long latestOffset = kafkaConsumer.position(topicPartition);
      TopicPartitionVO defaultPartitionVo = partitionsVo.get(-1);
      LOG.debug("topic: {}, partition: {}, latestOffset: {}", topicPartition.topic(), topicPartition.partition(), latestOffset);
      final TopicPartitionVO partitionVo = partitionsVo.get(topicPartition.partition());
      partitionVo.setSize(latestOffset);
      if (defaultPartitionVo.getSize() < 0)
        defaultPartitionVo.setSize(latestOffset);
      else
        defaultPartitionVo.setSize(Math.max(latestOffset, defaultPartitionVo.getSize())); 
    });
    
    return partitionsVo;
  }

  /**
   * Retrieves latest records from the given offset.
   * @param partition Topic partition
   * @param offset Offset to seek from
   * @param count Maximum number of records returned
   * @param deserializer Message deserialiser
   * @return Latest records
   */
  synchronized List<ConsumerRecord<String, String>> getLatestRecords(TopicPartition partition, long offset, int count,
                                                                     MessageDeserializer deserializer) {
    initializeClient();
    List<TopicPartition> partitions = null;
    if (partition.partition() == -1) {      
      return this.getLatestRecords(partition.topic(), offset, count, deserializer);
    } else {
      partitions = Collections.singletonList(partition);
      kafkaConsumer.assign(partitions);
      kafkaConsumer.seek(partition, offset);
      
      final var maxEmptyPolls = 3; // caps the number of empty polls
      final var rawRecords = new ArrayList<ConsumerRecord<String, byte[]>>(count);
      final var earliestOffset = kafkaConsumer.beginningOffsets(partitions).get(partition);
      final var latestOffset = kafkaConsumer.endOffsets(partitions).get(partition) - 1;
      if (earliestOffset > latestOffset) return Collections.emptyList();
      var currentOffset = offset - 1;

      // stop if got to count or get to the latest offset
      var emptyPolls = 0;
      while (rawRecords.size() < count && currentOffset < latestOffset) {
        final var polled = kafkaConsumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS)).records(partition);

        if (! polled.isEmpty()) {
          rawRecords.addAll(polled);
          currentOffset = polled.get(polled.size() - 1).offset();
          emptyPolls = 0;
        } else if (++emptyPolls == maxEmptyPolls) {
          break;
        }
      }
      
      return rawRecords
          .subList(0, Math.min(count, rawRecords.size()))
          .stream()
          .map(rec -> new ConsumerRecord<>(rec.topic(),
                                           rec.partition(),
                                           rec.offset(),
                                           rec.timestamp(),
                                           rec.timestampType(),
                                           0L,
                                           rec.serializedKeySize(),
                                           rec.serializedValueSize(),
                                           rec.key(),
                                           deserialize(deserializer, rec.value()),
                                           rec.headers(),
                                           rec.leaderEpoch()))
          .collect(Collectors.toList());
    }
    
    

    
  }

  /**
   * Gets records from all partitions of a given topic.
   * @param count The maximum number of records getting back.
   * @param deserializer Message deserializer
   * @return A list of consumer records for a given topic.
   */
  synchronized List<ConsumerRecord<String, String>> getLatestRecords(String topic,
                                                                     int count,
                                                                     MessageDeserializer deserializer) {
    initializeClient();
    final var partitionInfoSet = kafkaConsumer.partitionsFor(topic);
    final var partitions = partitionInfoSet.stream()
        .map(partitionInfo -> new TopicPartition(partitionInfo.topic(),
            partitionInfo.partition()))
        .collect(Collectors.toList());
    kafkaConsumer.assign(partitions);
    final var latestOffsets = kafkaConsumer.endOffsets(partitions);    
    for (var partition : partitions) {
      final var latestOffset = Math.max(0, latestOffsets.get(partition) - 1);
      kafkaConsumer.seek(partition, Math.max(0, latestOffset - count));
    }

    final var totalCount = count * partitions.size();
    final Map<TopicPartition, List<ConsumerRecord<String, byte[]>>> rawRecords
        = partitions.stream().collect(Collectors.toMap(p -> p , p -> new ArrayList<>(count)));

    var moreRecords = true;
    while (rawRecords.size() < totalCount && moreRecords) {
      final var polled = kafkaConsumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS));

      moreRecords = false;
      for (var partition : polled.partitions()) {
        var records = polled.records(partition);
        if (!records.isEmpty()) {
          rawRecords.get(partition).addAll(records);
          moreRecords = records.get(records.size() - 1).offset() < latestOffsets.get(partition) - 1;
        }
      }
    }

    return rawRecords
        .values()
        .stream()
        .flatMap(Collection::stream)
        .map(rec -> new ConsumerRecord<>(rec.topic(),
            rec.partition(),
            rec.offset(),
            rec.timestamp(),
            rec.timestampType(),
            0L,
            rec.serializedKeySize(),
            rec.serializedValueSize(),
            rec.key(),
            deserialize(deserializer, rec.value()),
            rec.headers(),
            rec.leaderEpoch()))
        .collect(Collectors.toList());
  }
  
  
  /**
   * Gets records from all partitions of a given topic.
   * @param name of topic to read from
   * @offset starting offset
   * @param count The maximum number of records getting back.
   * @param deserializer Message deserializer
   * @return A list of consumer records for a given topic.
   */
  synchronized List<ConsumerRecord<String, String>> getLatestRecords(String topic,
                                                                     long offset,
                                                                     int count,
                                                                     MessageDeserializer deserializer) {
    initializeClient();
    final var partitionInfoSet = kafkaConsumer.partitionsFor(topic);
    final var partitions = partitionInfoSet.stream()
        .map(partitionInfo -> new TopicPartition(partitionInfo.topic(),
            partitionInfo.partition()))
        .collect(Collectors.toList());

    final var totalCount = count; // * partitions.size();
    final Map<TopicPartition, List<ConsumerRecord<String, byte[]>>> rawRecords
        = partitions.stream().collect(Collectors.toMap(p -> p , p -> new ArrayList<>(count)));

    final var maxEmptyPolls = 3; 
    var moreRecords = true;    
    int totalRead = 0;
    for (TopicPartition partition : partitions) {
      long readRecords = 0;
      int emptyPolls = 0;
      
      while (readRecords < totalCount && moreRecords) {
        kafkaConsumer.assign(Collections.singletonList(partition));
        final var latestOffsets = kafkaConsumer.endOffsets(partitions);
        final var startingOffset = Math.max(0, offset);
        final var latestOffset = Math.min(startingOffset, latestOffsets.get(partition) - 1);
        kafkaConsumer.seek(partition, Math.max(0, latestOffset));
        
        final var polled = kafkaConsumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS));

        moreRecords = false;
        var records = polled.records(partition);
        if (!records.isEmpty()) {
          rawRecords.get(partition).addAll(records);
          moreRecords = records.get(records.size() - 1).offset() < latestOffsets.get(partition) - 1;
          readRecords += records.size();
          totalRead += readRecords;
        } else if (++emptyPolls == maxEmptyPolls) {
          break;
        }
      }
    }
    

    return rawRecords
        .values()
        .stream()
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingLong(ConsumerRecord::offset))
        .map(rec -> new ConsumerRecord<>(rec.topic(),
            rec.partition(),
            rec.offset(),
            rec.timestamp(),
            rec.timestampType(),
            0L,
            rec.serializedKeySize(),
            rec.serializedValueSize(),
            rec.key(),
            deserialize(deserializer, rec.value()),
            rec.headers(),
            rec.leaderEpoch()))
        .collect(Collectors.toList())
        .subList(0, Math.min(count, totalRead));
  }

  private static String deserialize(MessageDeserializer deserializer, byte[] bytes) {
    return bytes != null ? deserializer.deserializeMessage(ByteBuffer.wrap(bytes)) : "empty";
  }

  synchronized Map<String, TopicVO> getTopicInfos(String[] topics) {
    initializeClient();
    final var topicSet = kafkaConsumer.listTopics().keySet();
    if (topics.length == 0) {
      topics = Arrays.copyOf(topicSet.toArray(), topicSet.size(), String[].class);
    }
    final var topicVos = new HashMap<String, TopicVO>(topics.length, 1f);

    for (var topic : topics) {
      if (topicSet.contains(topic)) {
        topicVos.put(topic, getTopicInfo(topic));
      }
    }

    return topicVos;
  }

  private TopicVO getTopicInfo(String topic) {
    final var partitionInfoList = kafkaConsumer.partitionsFor(topic);
    final var topicVo = new TopicVO(topic);
    final var partitions = new TreeMap<Integer, TopicPartitionVO>();
    long topicSize = 0;

    for (var partitionInfo : partitionInfoList) {
      final var topicPartitionVo = new TopicPartitionVO(partitionInfo.partition());
      final var inSyncReplicaIds = Arrays.stream(partitionInfo.inSyncReplicas()).map(Node::id).collect(Collectors.toSet());
      final var offlineReplicaIds = Arrays.stream(partitionInfo.offlineReplicas()).map(Node::id).collect(Collectors.toSet());
      
      for (var node : partitionInfo.replicas()) {
        final var isInSync = inSyncReplicaIds.contains(node.id());
        final var isOffline = offlineReplicaIds.contains(node.id());
        topicPartitionVo.addReplica(new TopicPartitionVO.PartitionReplica(node.id(), isInSync, false, isOffline));
      }

      final var leader = partitionInfo.leader();
      if (leader != null) {
        topicPartitionVo.addReplica(new TopicPartitionVO.PartitionReplica(leader.id(), true, true, false));
      }
      
      topicSize += topicPartitionVo.getSize();
      
      partitions.put(partitionInfo.partition(), topicPartitionVo);
    }
    
    final var defaultPartitionVo = new TopicPartitionVO(-1);
    defaultPartitionVo.setFirstOffset(-1);
    defaultPartitionVo.setSize(topicSize);
    partitions.put(-1, defaultPartitionVo);

    topicVo.setPartitions(partitions);
    return topicVo;
  }
}
