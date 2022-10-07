package kafdrop.util;

import io.confluent.kafka.serializers.*;

import java.nio.*;
import java.util.*;
import java.util.stream.Collectors;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  public AvroMessageDeserializer(String topicName, String schemaRegistryUrl, String schemaRegistryAuth) {
    this.topicName = topicName;
    this.deserializer = getDeserializer(schemaRegistryUrl, schemaRegistryAuth, topicName);
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    return deserializer.deserialize(topicName, bytes).toString();
  }

  private static KafkaAvroDeserializer getDeserializer(String schemaRegistryUrl, String schemaRegistryAuth, String topicName) {
    final var config = new HashMap<String, Object>();
    config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    if (schemaRegistryAuth != null) {
      config.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }
    setConfigFromEnvIfAvailable(topicName, AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, config);
    final var kafkaAvroDeserializer = new KafkaAvroDeserializer();
    kafkaAvroDeserializer.configure(config, false);
    return kafkaAvroDeserializer;
  }

  private static void setConfigFromEnvIfAvailable(String topicName, String configPath, Map<String,Object> config){
    String configPrefix = "SCHEMA_REGISTRY";
    String topicScopedEnvPath = Arrays.stream(new String[]{configPrefix, configPath.replaceAll("\\.", "_"), topicName.replaceAll("-", "_") } )
            .map(String::toUpperCase).collect(Collectors.joining("_"));

    String noTopicScopedEnvPath = Arrays.stream(new String[]{ configPrefix, configPath.replaceAll("\\.", "_") })
            .map(String::toUpperCase).collect(Collectors.joining("_"));

    for(String envPath : new String[]{topicScopedEnvPath, noTopicScopedEnvPath}) {

      String namingStrategyValue = System.getenv(envPath);
      if (namingStrategyValue != null) {
        config.put(configPath, namingStrategyValue);
      }
    }
  }
}
