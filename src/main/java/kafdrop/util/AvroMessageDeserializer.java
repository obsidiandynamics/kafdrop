package kafdrop.util;

import com.amazonaws.services.schemaregistry.deserializers.avro.AWSKafkaAvroDeserializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import io.confluent.kafka.serializers.*;
import kafdrop.config.GlueSchemaRegistryConfig;
import kafdrop.config.SchemaRegistryConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Deserializer;
import software.amazon.awssdk.services.glue.model.DataFormat;

import java.nio.*;
import java.util.*;


public final class AvroMessageDeserializer implements MessageDeserializer {
  private final String topicName;

  private final Deserializer avroDeserializer;

  public AvroMessageDeserializer(String topicName,
                                 SchemaRegistryConfiguration.SchemaRegistryProperties schemaRegistryProperties,
                                 GlueSchemaRegistryConfig.GlueSchemaRegistryProperties glueSchemaRegistryProperties) {
    this.topicName = topicName;

    if(glueSchemaRegistryProperties.isConfigured()){
      this.avroDeserializer =  getAWSDeserializer(glueSchemaRegistryProperties.getRegion(),
        glueSchemaRegistryProperties.getRegistryName(),
        glueSchemaRegistryProperties.getAwsEndpoint());
    }
    else{
      this.avroDeserializer = getDeserializer(schemaRegistryProperties.getConnect(), schemaRegistryProperties.getAuth());
    }
  }

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    // Convert byte buffer to byte array
    final var bytes = ByteUtils.convertToByteArray(buffer);
    return avroDeserializer.deserialize(topicName, bytes).toString();
  }

  private static Deserializer getAWSDeserializer(String region, String registryName, String awsEndpoint) {
    final var config = new HashMap<String, Object>();
    config.put(AWSSchemaRegistryConstants.AWS_REGION, region);
    config.put(AWSSchemaRegistryConstants.REGISTRY_NAME, registryName);
    config.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());
    config.put(AWSSchemaRegistryConstants.DATA_FORMAT, DataFormat.AVRO.name());
    if(StringUtils.isNotEmpty(awsEndpoint))
      config.put(AWSSchemaRegistryConstants.AWS_ENDPOINT, awsEndpoint);
    final var awsKafkaAvroDeserializer = new AWSKafkaAvroDeserializer(config);
    return awsKafkaAvroDeserializer;
  }

  private static Deserializer getDeserializer(String schemaRegistryUrl, String schemaRegistryAuth) {
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
