package kafdrop.service;

import kafdrop.config.KafkaConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
public final class KafkaHighLevelProducer {
  private static final int POLL_TIMEOUT_MS = 200;

  private static final Logger LOG = LoggerFactory.getLogger(KafkaHighLevelProducer.class);

  private Producer<String, String> producer;

  private final KafkaConfiguration kafkaConfiguration;

  public KafkaHighLevelProducer(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  private void initializeClient() {
    if (producer == null) {
      final var properties = new Properties();
      properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
      properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
      properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
      properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
      properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafdrop-consumer");
      properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
      kafkaConfiguration.applyCommon(properties);
      producer = new KafkaProducer(properties);
    }
  }

  public synchronized void publish(String producerTopic, String payload) {
    String key = "key1";
    ProducerRecord record = new ProducerRecord(producerTopic, key, payload);
    try {
      producer.send(record);
      LOG.debug("publish payload completed: {}", payload);
    } catch (Exception var12) {
      LOG.error("Error while publishing payload", var12);
      throw var12;
    } finally {
      //producer.close();
    }
  }
}
