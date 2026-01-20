package kafdrop.util;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import java.nio.ByteBuffer;
import java.util.HashMap;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;
  private final KafkaAvroDeserializer deserializer;

  private final AWSKafkaAvroDeserializer glueSchemaRegistryKafkaDeserializer;


  public AvroMessageDeserializer(String topicName, String schemaRegistryUrl, String schemaRegistryAuth) {
    this.topicName = topicName;
    if("awsGlue")
    {
      this.glueSchemaRegistryKafkaDeserializer=getAWSDeserializer(schemaName, schemaRegistryName, awsRegion);
 
    }else{
       this.deserializer = getDeserializer(schemaRegistryUrl, schemaRegistryAuth);
    }
   
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    if("awsGlue")
    return glueSchemaRegistryKafkaDeserializer.deserialize(topicName, bytes).toString();
    else
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

  private static AWSKafkaAvroDeserializer getAWSDeserializer(String schemaName, String schemaRegistryName, String awsRegion) {

    final var props = new HashMap<String, Object>();
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GlueSchemaRegistryKafkaDeserializer.class.getName());

    props.put(AWSSchemaRegistryConstants.AWS_REGION, awsRegion);
    props.put(AWSSchemaRegistryConstants.REGISTRY_NAME, schemaRegistryName);
    props.put(AWSSchemaRegistryConstants.SCHEMA_NAME, schemaName);
    props.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());

    final var deserializer = new AWSKafkaAvroDeserializer();
    deserializer.configure(props, false);
    return deserializer;
  }
}
