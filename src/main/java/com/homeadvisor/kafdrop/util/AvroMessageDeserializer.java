package com.homeadvisor.kafdrop.util;

import com.google.gson.*;
import io.confluent.kafka.serializers.*;

import java.nio.*;
import java.util.*;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  public AvroMessageDeserializer(String topicName, String schemaRegistryUrl) {
    this.topicName = topicName;
    this.deserializer = getDeserializer(schemaRegistryUrl);
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    return formatJsonMessage(deserializer.deserialize(topicName, bytes).toString());
  }

  private static String formatJsonMessage(String jsonMessage) {
    final var gson = new GsonBuilder().setPrettyPrinting().create();
    final var parser = new JsonParser();
    final var element = parser.parse(jsonMessage);
    return gson.toJson(element);
  }

  private static KafkaAvroDeserializer getDeserializer(String schemaRegistryUrl) {
    final var config = new HashMap<String, Object>();
    config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    final var kafkaAvroDeserializer = new KafkaAvroDeserializer();
    kafkaAvroDeserializer.configure(config, false);
    return kafkaAvroDeserializer;
  }
}
