package kafdrop.config;

import lombok.*;
import org.apache.kafka.clients.*;
import org.apache.kafka.common.config.*;
import org.slf4j.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public final class KafkaConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

  private static final String KAFKA_PROPERTIES_FILE = "kafka.properties";
  private static final String KAFKA_TRUSTSTORE_FILE = "kafka.truststore.jks";
  private static final String KAFKA_KEYSTORE_FILE = "kafka.keystore.jks";

  private String brokerConnect;
  private Boolean isSecured = false;
  private String saslMechanism;
  private String securityProtocol;

  public void applyCommon(Properties properties) {
    properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerConnect);
    if (isSecured) {
      LOG.warn("The 'isSecured' property is deprecated; consult README.md on the preferred way to configure security");
      properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
      properties.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
    }

    if (new File(KAFKA_TRUSTSTORE_FILE).isFile()) {
      LOG.info("Assigning truststore location to {}", KAFKA_TRUSTSTORE_FILE);
      properties.put("ssl.truststore.location", KAFKA_TRUSTSTORE_FILE);
    }

    if (new File(KAFKA_KEYSTORE_FILE).isFile()) {
      LOG.info("Assigning keystore location to {}", KAFKA_KEYSTORE_FILE);
      properties.put("ssl.keystore.location", KAFKA_KEYSTORE_FILE);
    }

    final var propertiesFile = new File(KAFKA_PROPERTIES_FILE);
    if (propertiesFile.isFile()) {
      LOG.info("Loading properties from {}", KAFKA_PROPERTIES_FILE);
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
