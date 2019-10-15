package kafdrop.config;

import java.io.*;
import java.util.*;
import lombok.*;
import org.apache.kafka.clients.*;
import org.apache.kafka.common.config.*;
import org.slf4j.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;


@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public final class KafkaConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

  private String brokerConnect;
  private Boolean isSecured = false;
  private String saslMechanism;
  private String securityProtocol;
  private String truststoreLocation;
  private String propertiesFileLocation;
  private String keystoreLocation;



  public void applyCommon(Properties properties) {
    properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerConnect);
    if (isSecured) {
      LOG.warn("The 'isSecured' property is deprecated; consult README.md on the preferred way to configure security");
      properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
      properties.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
    }

    if (new File(truststoreLocation).isFile()) {
      LOG.info("Assigning truststore location to {}", truststoreLocation);
      properties.put("ssl.truststore.location", truststoreLocation);
    }

    if (new File(keystoreLocation).isFile()) {
      LOG.info("Assigning keystore location to {}", keystoreLocation);
      properties.put("ssl.keystore.location", keystoreLocation);
    }

    final var propertiesFile = new File(propertiesFileLocation);
    if (propertiesFile.isFile()) {
      LOG.info("Loading properties from {}", propertiesFileLocation);
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