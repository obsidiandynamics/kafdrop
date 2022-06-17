package kafdrop.util;

import io.confluent.kafka.serializers.*;

import java.nio.*;
import java.util.*;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  public AvroMessageDeserializer(String topicName, String schemaRegistryUrl, String schemaRegistryAuth) {
    this.topicName = topicName;
    this.deserializer = getDeserializer(schemaRegistryUrl, schemaRegistryAuth);
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    return deserializer.deserialize(topicName, bytes).toString();
  }

  private static KafkaAvroDeserializer getDeserializer(String schemaRegistryUrl, String schemaRegistryAuth) {
    final var config = new HashMap<String, Object>();
    config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    if (schemaRegistryAuth != null) {
      config.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }
    final var kafkaAvroDeserializer = new KafkaAvroDeserializer();
    kafkaAvroDeserializer.configure(config, false);
    return kafkaAvroDeserializer;
  }
}
