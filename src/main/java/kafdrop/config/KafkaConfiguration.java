package kafdrop.config;

import lombok.Data;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    final var propertiesFile = new File(this.propertiesFile);
    if (propertiesFile.isFile()) {
      LOG.info("Loading properties from {}", this.propertiesFile);
      final var propertyOverrides = new Properties();
      try (var propsReader = new BufferedReader(new FileReader(propertiesFile))) {
        propertyOverrides.load(propsReader);
      } catch (IOException e) {
        throw new KafkaConfigurationException(e);
      }
      properties.putAll(propertyOverrides);
    }
  }
}
