package kafdrop.util;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.serializers.*;
import kafdrop.config.KafkaConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.*;
import java.util.*;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  private static final Logger LOG = LoggerFactory.getLogger(AvroMessageDeserializer.class);

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
    final var sslConfig = new HashMap<String, Object>();
    config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

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
          LOG.debug("ssl Config Tag: {} - Value: {}",name, propertyOverrides.getProperty(name));
          sslConfig.put(name, propertyOverrides.getProperty(name));
        }
      }
    }

    if (schemaRegistryAuth != null) {
      config.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      config.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, schemaRegistryAuth);
    }


    RestService restService = new RestService(schemaRegistryUrl);

    SchemaRegistryClient schemaRegistry = new CachedSchemaRegistryClient(
            restService,
            1000,
            sslConfig,
            null
    );

    final var kafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistry, config);

    return kafkaAvroDeserializer;
  }
}
