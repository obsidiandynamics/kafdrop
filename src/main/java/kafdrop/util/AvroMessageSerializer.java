package kafdrop.util;

import java.util.HashMap;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

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
    config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    if (schemaRegistryAuth != null) {
      config.put(AbstractKafkaAvroSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(AbstractKafkaAvroSerDeConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }
    final var kafkaAvroSerializer = new KafkaAvroSerializer();
    kafkaAvroSerializer.configure(config, false);
    return kafkaAvroSerializer;
  }

}
