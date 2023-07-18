package kafdrop.service;

import jakarta.annotation.PostConstruct;
import kafdrop.config.KafkaConfiguration;
import kafdrop.model.TopicPartitionVO;
import kafdrop.model.TopicVO;
import kafdrop.service.SearchResults.CompletionReason;
import kafdrop.util.Deserializers;
import kafdrop.util.MessageDeserializer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public final class KafkaHighLevelConsumer {
  private static final int POLL_TIMEOUT_MS = 200;
  private static final int SEARCH_POLL_TIMEOUT_MS = 1000;
  private static final int SEARCH_MAX_MESSAGES_TO_SCAN = 100000;
  private static final Logger LOG = LoggerFactory.getLogger(KafkaHighLevelConsumer.class);

  private KafkaConsumer<byte[], byte[]> kafkaConsumer;

  private final KafkaConfiguration kafkaConfiguration;

  public KafkaHighLevelConsumer(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  private void initializeClient() {
    if (kafkaConsumer == null) {
      final var properties = new Properties();
      properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
      properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
      properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
      properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
      properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafdrop-consumer");
      properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
      kafkaConfiguration.applyCommon(properties);

      kafkaConsumer = new KafkaConsumer<>(properties);
    }
  }

  synchronized void setTopicPartitionSizes(List<TopicVO> topics) {
    initializeClient();

    Map<TopicVO, List<TopicPartition>> allTopics = topics.stream().map(topicVO -> {
      List<TopicPartition> topicPartitions = topicVO.getPartitions().stream().map(topicPartitionVO ->
        new TopicPartition(topicVO.getName(), topicPartitionVO.getId())
      ).toList();

      return Pair.of(topicVO, topicPartitions);
    }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    List<TopicPartition> allTopicPartitions = allTopics.values().stream()
      .flatMap(Collection::stream)
      .toList();

    kafkaConsumer.assign(allTopicPartitions);
    Map<TopicPartition, Long> beginningOffset = kafkaConsumer.beginningOffsets(allTopicPartitions);
    Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(allTopicPartitions);

    allTopics.forEach((topicVO, topicPartitions) -> topicPartitions.forEach(topicPartition -> {
      Optional<TopicPartitionVO> partition = topicVO.getPartition(topicPartition.partition());

      partition.ifPresent(p -> {
        Long startOffset = beginningOffset.get(topicPartition);
        Long endOffset = endOffsets.get(topicPartition);

        LOG.debug("topic: {}, partition: {}, startOffset: {}, endOffset: {}", topicPartition.topic(),
          topicPartition.partition(), startOffset, endOffset);
        p.setFirstOffset(startOffset);
        p.setSize(endOffset);
      });
    }));
  }

  /**
   * Retrieves latest records from the given offset.
   *
   * @param partition     Topic partition
   * @param offset        Offset to seek from
   * @param count         Maximum number of records returned
   * @param deserializers Key and Value deserializer
   * @return Latest records
   */
  synchronized List<ConsumerRecord<String, String>> getLatestRecords(TopicPartition partition, long offset, int count,
                                                                     Deserializers deserializers) {
    initializeClient();
    final var partitions = Collections.singletonList(partition);
    final var maxEmptyPolls = 3; // caps the number of empty polls
    kafkaConsumer.assign(partitions);
    kafkaConsumer.seek(partition, offset);

    final var rawRecords = new ArrayList<ConsumerRecord<byte[], byte[]>>(count);
    final var earliestOffset = kafkaConsumer.beginningOffsets(partitions).get(partition);
    final var latestOffset = kafkaConsumer.endOffsets(partitions).get(partition) - 1;
    if (earliestOffset > latestOffset) return Collections.emptyList();
    var currentOffset = offset - 1;

    // stop if got to count or get to the latest offset
    var emptyPolls = 0;
    while (rawRecords.size() < count && currentOffset < latestOffset) {
      final var polled = kafkaConsumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS)).records(partition);

      if (!polled.isEmpty()) {
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
      .map(rec -> createConsumerRecord(rec, deserializers))
      .toList();
  }

  /**
   * Gets records from all partitions of a given topic.
   *
   * @param count         The maximum number of records getting back.
   * @param deserializers Key and Value deserializers
   * @return A list of consumer records for a given topic.
   */
  synchronized List<ConsumerRecord<String, String>> getLatestRecords(String topic,
                                                                     int count,
                                                                     Deserializers deserializers) {
    initializeClient();
    final List<TopicPartition> partitions = determinePartitionsForTopic(topic);
    kafkaConsumer.assign(partitions);
    final var latestOffsets = kafkaConsumer.endOffsets(partitions);

    for (var partition : partitions) {
      final var latestOffset = Math.max(0, latestOffsets.get(partition) - 1);
      kafkaConsumer.seek(partition, Math.max(0, latestOffset - count));
    }

    final var totalCount = count * partitions.size();
    final Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> rawRecords
      = partitions.stream().collect(Collectors.toMap(p -> p, p -> new ArrayList<>(count)));

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
      .map(rec -> createConsumerRecord(rec, deserializers))
      .toList();
  }

  /**
   * Searches records from all partitions of a given topic containing a given text.
   *
   * @param topic          The topic
   * @param searchString   Searched text.
   * @param maximumCount   The maximum number of results to return
   * @param startTimestamp The message timestamp to search from
   * @param deserializers  Key and Value deserializers
   * @return A list of consumer records for a given topic.
   */
  synchronized SearchResults searchRecords(String topic,
                                           String searchString,
                                           Integer maximumCount,
                                           Date startTimestamp,
                                           Deserializers deserializers) {
    initializeClient();
    final List<TopicPartition> partitions = determinePartitionsForTopic(topic);
    kafkaConsumer.assign(partitions);
    seekToTimestamp(partitions, startTimestamp);

    return searchRecords(searchString, maximumCount, deserializers);
  }

  private void seekToTimestamp(List<TopicPartition> partitions, Date startTimestamp) {
    kafkaConsumer.offsetsForTimes(partitions.stream()
        .collect(Collectors.toMap(tp -> tp, tp -> startTimestamp.getTime())))
      .forEach(this::seekToOffset);
  }

  @NotNull
  private SearchResults searchRecords(String searchString, Integer maximumCount, Deserializers deserializers) {
    final List<ConsumerRecord<String, String>> foundRecords = new ArrayList<>();
    boolean moreRecords;
    var loweredSearchString = searchString.toLowerCase();
    var scanStatus = new ScanStatus(Long.MAX_VALUE, 0);

    do {
      final var polled = kafkaConsumer.poll(Duration.ofMillis(SEARCH_POLL_TIMEOUT_MS));
      scanStatus = searchPolledRecords(polled, deserializers, loweredSearchString, foundRecords, scanStatus);

      moreRecords = !polled.isEmpty();
    } while (foundRecords.size() < maximumCount
      && moreRecords
      && scanStatus.scannedCount() < SEARCH_MAX_MESSAGES_TO_SCAN);


    CompletionReason completionReason;
    if (!moreRecords) {
      completionReason = CompletionReason.NO_MORE_MESSAGES_IN_TOPIC;
    } else if (foundRecords.size() >= maximumCount) {
      completionReason = CompletionReason.FOUND_REQUESTED_NUMBER_OF_RESULTS;
    } else if (scanStatus.scannedCount() >= SEARCH_MAX_MESSAGES_TO_SCAN) {
      completionReason = CompletionReason.EXCEEDED_MAX_SCAN_COUNT;
    } else {
      throw new IllegalStateException("This situation is unexpected");
    }

    return new SearchResults(foundRecords, completionReason, new Date(scanStatus.endingTimestamp()),
      scanStatus.scannedCount());
  }

  private ScanStatus searchPolledRecords(ConsumerRecords<byte[], byte[]> polled, Deserializers deserializers,
                                         String loweredSearchString,
                                         List<ConsumerRecord<String, String>> foundRecords, ScanStatus scanStatus) {
    for (var partition : polled.partitions()) {
      scanStatus = searchPolledRecordsOfPartition(polled, partition, deserializers, loweredSearchString, foundRecords,
        scanStatus);
    }
    return scanStatus;
  }

  private ScanStatus searchPolledRecordsOfPartition(ConsumerRecords<byte[], byte[]> polled,
                                                    TopicPartition partition, Deserializers deserializers,
                                                    String loweredSearchString,
                                                    List<ConsumerRecord<String, String>> foundRecords,
                                                    ScanStatus scanStatus) {
    var records = polled.records(partition);
    if (!records.isEmpty()) {

      scanStatus = ScanStatus.createInstance(scanStatus, records.get(0).timestamp(), records.size());
      // Add to found records if it matches the search criteria
      foundRecords.addAll(records.stream()
        .filter(rec ->
          containsValue(deserializers.getKeyDeserializer(), rec.key(), loweredSearchString) ||
            containsValue(deserializers.getValueDeserializer(), rec.value(), loweredSearchString))
        .map(rec -> createConsumerRecord(rec, deserializers))
        .toList());
    }
    return scanStatus;
  }

  private record ScanStatus(long endingTimestamp, int scannedCount) {
    public static ScanStatus createInstance(ScanStatus previousScanStatus,
                                            long firstTimestampOfBLock,
                                            int sizeOfBlock) {
      // Keep track of the lowest timestamp among this batch of records.
      // This is what we will report to the user in the event that the search terminates
      // early, so that they can perform a new search with this new timestamp as a starting point
      return new ScanStatus((firstTimestampOfBLock < previousScanStatus.endingTimestamp) ?
        firstTimestampOfBLock :
        previousScanStatus.endingTimestamp(), previousScanStatus.scannedCount() + sizeOfBlock);
    }
  }

  @NotNull
  private List<TopicPartition> determinePartitionsForTopic(String topic) {
    return kafkaConsumer.partitionsFor(topic).stream()
      .map(partitionInfo -> new TopicPartition(partitionInfo.topic(), partitionInfo.partition()))
      .toList();
  }

  private void seekToOffset(TopicPartition partition, OffsetAndTimestamp offset) {
    // Old kafka message versions don't have timestamps so the offset would be null for that partition
    kafkaConsumer.seek(partition, (offset == null) ? 0 : offset.offset());
  }

  @NotNull
  private static ConsumerRecord<String, String> createConsumerRecord(ConsumerRecord<byte[], byte[]> rec,
                                                                     Deserializers deserializers) {
    return new ConsumerRecord<>(rec.topic(), rec.partition(), rec.offset(), rec.timestamp(), rec.timestampType(),
      rec.serializedKeySize(), rec.serializedValueSize(), deserialize(deserializers.getKeyDeserializer(), rec.key()),
      deserialize(deserializers.getValueDeserializer(), rec.value()), rec.headers(),
      rec.leaderEpoch());
  }

  private boolean containsValue(MessageDeserializer deserializer, byte[] value, String loweredSearchString) {
    return deserialize(deserializer, value).toLowerCase().contains(loweredSearchString);
  }


  private static String deserialize(MessageDeserializer deserializer, byte[] bytes) {
    return bytes != null ? deserializer.deserializeMessage(ByteBuffer.wrap(bytes)) : "empty";
  }

  synchronized Map<String, List<PartitionInfo>> getAllTopics() {
    initializeClient();

    return kafkaConsumer.listTopics();
  }

  synchronized Map<String, TopicVO> getTopicInfos(Map<String, List<PartitionInfo>> allTopicsMap, String[] topics) {
    initializeClient();

    final var topicSet = allTopicsMap.keySet();
    if (topics.length == 0) {
      topics = Arrays.copyOf(topicSet.toArray(), topicSet.size(), String[].class);
    }

    return Arrays.stream(topics)
      .filter(topicSet::contains)
      .map(topic -> Pair.of(topic, getTopicInfo(topic, allTopicsMap.get(topic))))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  private TopicVO getTopicInfo(String topic, List<PartitionInfo> partitionInfoList) {
    final var topicVo = new TopicVO(topic);
    final var partitions = new TreeMap<Integer, TopicPartitionVO>();

    for (var partitionInfo : partitionInfoList) {
      final var topicPartitionVo = new TopicPartitionVO(partitionInfo.partition());
      final var inSyncReplicaIds =
        Arrays.stream(partitionInfo.inSyncReplicas()).map(Node::id).collect(Collectors.toSet());
      final var offlineReplicaIds =
        Arrays.stream(partitionInfo.offlineReplicas()).map(Node::id).collect(Collectors.toSet());

      for (var node : partitionInfo.replicas()) {
        final var isInSync = inSyncReplicaIds.contains(node.id());
        final var isOffline = offlineReplicaIds.contains(node.id());
        topicPartitionVo.addReplica(new TopicPartitionVO.PartitionReplica(node.id(), isInSync, false, isOffline));
      }

      final var leader = partitionInfo.leader();
      if (leader != null) {
        topicPartitionVo.addReplica(new TopicPartitionVO.PartitionReplica(leader.id(), true, true, false));
      }
      partitions.put(partitionInfo.partition(), topicPartitionVo);
    }

    topicVo.setPartitions(partitions);
    return topicVo;
  }
}
