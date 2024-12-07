package kafdrop.service;

import java.util.Properties;
import java.util.concurrent.Future;


import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kafdrop.config.KafkaConfiguration;
import kafdrop.model.CreateMessageVO;
import kafdrop.util.Serializers;

@Service
public final class KafkaHighLevelProducer {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaHighLevelProducer.class);
  private final KafkaConfiguration kafkaConfiguration;
  private KafkaProducer<byte[], byte[]> kafkaProducer;

  public KafkaHighLevelProducer(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  private void initializeClient() {
    if (kafkaProducer == null) {
      final var properties = new Properties();
      properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
      properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
      properties.put(ProducerConfig.ACKS_CONFIG, "all");
      properties.put(ProducerConfig.RETRIES_CONFIG, 0);
      properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
      properties.put(ProducerConfig.CLIENT_ID_CONFIG, "kafdrop-producer");
      kafkaConfiguration.applyCommon(properties);

      kafkaProducer = new KafkaProducer<>(properties);
    }
  }

  public RecordMetadata publishMessage(CreateMessageVO message, Serializers serializers) {
    initializeClient();

    final ProducerRecord<byte[], byte[]> record = new ProducerRecord<byte[], byte[]>(message.getTopic(),
      message.getTopicPartition(), serializers.getKeySerializer().serializeMessage(message.getKey()),
      serializers.getValueSerializer().serializeMessage(message.getValue()));

    Future<RecordMetadata> result = kafkaProducer.send(record);
    try {
      RecordMetadata recordMetadata = result.get();
      LOG.info("Record published successfully [{}]", recordMetadata);
      return recordMetadata;
    } catch (Exception e) {
      LOG.error("Failed to publish message", e);
      throw new KafkaProducerException(e);
    }
  }
}
