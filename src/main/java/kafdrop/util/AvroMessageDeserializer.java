package kafdrop.util;

import io.confluent.kafka.serializers.*;
import kafdrop.config.KafkaConfigurationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.*;
import java.util.*;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  public AvroMessageDeserializer(
          String topicName,
          String schemaRegistryUrl,
          String schemaRegistryAuth,
          String schemaProperty) {
    this.topicName = topicName;
    this.deserializer = getDeserializer(schemaRegistryUrl, schemaRegistryAuth, schemaProperty);
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    return deserializer.deserialize(topicName, bytes).toString();
  }

  private static KafkaAvroDeserializer getDeserializer(
          String schemaRegistryUrl,
          String schemaRegistryAuth,
          String schemaPropertyFile) {
    final var config = new HashMap<String, Object>();
    config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

    //Add TlS properties if connection is secured
    if(schemaRegistryUrl.toLowerCase().contains("https")){
      final var propertiesFile = new File(schemaPropertyFile);
      if (propertiesFile.isFile()) {
        final var propertyOverrides = new Properties();
        try (var propsReader = new BufferedReader(new FileReader(propertiesFile))) {
          propertyOverrides.load(propsReader);
        } catch (IOException e) {
          throw new KafkaConfigurationException(e);
        }
        for (final String name : propertyOverrides.stringPropertyNames()){
          config.put(name, propertyOverrides.getProperty(name));
        }
      }
    }

    if (schemaRegistryAuth != null) {
      config.put(AbstractKafkaAvroSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(AbstractKafkaAvroSerDeConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }
    final var kafkaAvroDeserializer = new KafkaAvroDeserializer();
    kafkaAvroDeserializer.configure(config, false);
    return kafkaAvroDeserializer;
  }
}
