package kafdrop.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public final class KafkaConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

  private String brokerConnect;
  private String saslMechanism;
  private String securityProtocol;
  private String truststoreFile;
  private String propertiesFile;
  private String keystoreFile;

  public void applyCommon(Properties properties) {
    properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerConnect);

    if (securityProtocol.equals("SSL")) {
      properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
    }

    LOG.info("Checking truststore file {}", truststoreFile);
    if (new File(truststoreFile).isFile()) {
      LOG.info("Assigning truststore location to {}", truststoreFile);
      properties.put("ssl.truststore.location", truststoreFile);
    }

    LOG.info("Checking keystore file {}", keystoreFile);
    if (new File(keystoreFile).isFile()) {
      LOG.info("Assigning keystore location to {}", keystoreFile);
      properties.put("ssl.keystore.location", keystoreFile);
    }

    LOG.info("Checking properties file {}", propertiesFile);
    if (StringUtils.isNotBlank(propertiesFile)) {
      InputStream inputStream;
      try {
        Resource fileSystemResource = new FileSystemResource(this.propertiesFile);
        Resource classPathResource = new ClassPathResource(this.propertiesFile);
        if (fileSystemResource.isReadable()) {
          inputStream = fileSystemResource.getInputStream();
        } else if (classPathResource.isReadable()) {
          inputStream = classPathResource.getInputStream();
        } else {
          throw new KafkaConfigurationException(new IllegalArgumentException(
            "Kafka properties file %s is not valid or can not be found".formatted(this.propertiesFile)));
        }
        LOG.info("Loading properties from {}", this.propertiesFile);
        final var propertyOverrides = new Properties();
        propertyOverrides.load(inputStream);
        properties.putAll(propertyOverrides);
      } catch (IOException e) {
        throw new KafkaConfigurationException(e);
      }
    }
  }
}
