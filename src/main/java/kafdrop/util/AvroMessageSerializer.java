package kafdrop.util;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.util.HashMap;

public class AvroMessageSerializer implements MessageSerializer {

  private final String topicName;
  private final KafkaAvroSerializer serializer;

  public AvroMessageSerializer(String topicName, String schemaRegistryUrl, String schemaRegistryAuth) {
    this.topicName = topicName;
    this.serializer = getSerializer(schemaRegistryUrl, schemaRegistryAuth);
  }

  @Override
  public byte[] serializeMessage(String value) {
    final var bytes = value.getBytes();
    return serializer.serialize(topicName, bytes);
  }

  private KafkaAvroSerializer getSerializer(String schemaRegistryUrl, String schemaRegistryAuth) {
    final var config = new HashMap<String, Object>();
    config.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    if (schemaRegistryAuth != null) {
      config.put(KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(KafkaAvroDeserializerConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }
    final var kafkaAvroSerializer = new KafkaAvroSerializer();
    kafkaAvroSerializer.configure(config, false);
    return kafkaAvroSerializer;
  }

}
