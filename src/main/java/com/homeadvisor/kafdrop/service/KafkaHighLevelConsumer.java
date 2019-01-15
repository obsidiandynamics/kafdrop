package com.homeadvisor.kafdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.homeadvisor.kafdrop.config.KafkaConfiguration;
import com.homeadvisor.kafdrop.model.TopicPartitionVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by Satendra Sahu on 9/20/18
 */
@Service
public class KafkaHighLevelConsumer
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    @Autowired
     private ObjectMapper objectMapper;
    private KafkaConsumer<String, String> kafkaConsumer;

    @Autowired
    private KafkaConfiguration kafkaConfiguration;

    public KafkaHighLevelConsumer() {}

    @PostConstruct
    private void initializeClient()
    {
        if (kafkaConsumer == null) {

            Properties properties = new Properties();
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-drop-consumer-group");
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConfiguration.getKeyDeserializer());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConfiguration.getValueDeserializer());
            properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-drop-client");
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBrokerConnect());

            if (kafkaConfiguration.getIsSecured() == true) {
                properties.put(SaslConfigs.SASL_MECHANISM, kafkaConfiguration.getSaslMechanism());
                properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaConfiguration.getSecurityProtocol());
            }

            kafkaConsumer = new KafkaConsumer<String, String>(properties);
        }
    }

    public synchronized Map<Integer, TopicPartitionVO> getPartitionSize(String topic)
    {
        initializeClient();

        List<PartitionInfo> partitionInfoSet = kafkaConsumer.partitionsFor(topic);
        kafkaConsumer.assign(partitionInfoSet.stream().map(partitionInfo -> {
                    return new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
                }).collect(Collectors.toList())
        );

        kafkaConsumer.poll(0);
        Set<TopicPartition> assignedPartitionList = kafkaConsumer.assignment();
        TopicVO topicVO = getTopicInfo(topic);
        Map<Integer, TopicPartitionVO> partitionsVo = topicVO.getPartitionMap();

        kafkaConsumer.seekToBeginning(assignedPartitionList);
        assignedPartitionList.stream().forEach(topicPartition -> {
            TopicPartitionVO topicPartitionVO = partitionsVo.get(topicPartition.partition());
            long startOffset = kafkaConsumer.position(topicPartition);
            LOG.debug("topic: {}, partition: {}, startOffset: {}", topicPartition.topic(), topicPartition.partition(), startOffset);
            topicPartitionVO.setFirstOffset(startOffset);
        });

        kafkaConsumer.seekToEnd(assignedPartitionList);
        assignedPartitionList.stream().forEach(topicPartition -> {
            long latestOffset = kafkaConsumer.position(topicPartition);
            LOG.debug("topic: {}, partition: {}, latestOffset: {}", topicPartition.topic(), topicPartition.partition(), latestOffset);
            TopicPartitionVO partitionVO = partitionsVo.get(topicPartition.partition());
            partitionVO.setSize(latestOffset);
        });
        return partitionsVo;
    }

    public synchronized List<ConsumerRecord<String, String>> getLatestRecords(TopicPartition topicPartition, long offset, Long count)
    {
        initializeClient();
        kafkaConsumer.assign(Arrays.asList(topicPartition));
        kafkaConsumer.seek(topicPartition, offset);

        ConsumerRecords records = null;

        records = kafkaConsumer.poll(10);
        if (records.count() > 0) {
            return records.records(topicPartition).subList(0, count.intValue());
        }
        return null;
    }

    public synchronized Map<String, TopicVO> getTopicsInfo(String[] topics)
    {
        initializeClient();
        if (topics.length == 0) {
            Set<String> topicSet = kafkaConsumer.listTopics().keySet();
            topics = Arrays.copyOf(topicSet.toArray(), topicSet.size(), String[].class);
        }
        Map<String, TopicVO> topicVOMap = Maps.newHashMap();

        for (String topic : topics) {
            topicVOMap.put(topic, getTopicInfo(topic));
        }

        return topicVOMap;
    }

    private TopicVO getTopicInfo(String topic)
    {
        List<PartitionInfo> partitionInfoList = kafkaConsumer.partitionsFor(topic);
        TopicVO topicVO = new TopicVO(topic);
        Map<Integer, TopicPartitionVO> partitions = new TreeMap<>();

        for (PartitionInfo partitionInfo : partitionInfoList) {
            TopicPartitionVO topicPartitionVO = new TopicPartitionVO(partitionInfo.partition());

            Node leader = partitionInfo.leader();
            topicPartitionVO.addReplica(new TopicPartitionVO.PartitionReplica(leader.id(), true, true));

            for (Node node : partitionInfo.replicas()) {
                topicPartitionVO.addReplica(new TopicPartitionVO.PartitionReplica(node.id(), true, false));
            }
            partitions.put(partitionInfo.partition(), topicPartitionVO);
        }

        topicVO.setPartitions(partitions);
        return topicVO;
    }
}
