package kafdrop.config;

import lombok.*;
import org.apache.kafka.clients.*;
import org.apache.kafka.common.config.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public final class KafkaConfiguration {
  private String brokerConnect;
  private Boolean isSecured = false;
  private String saslMechanism;
  private String securityProtocol;

  /** Extra properties, base-64 encoded. Will override any prior properties. */
  private String props;

  public void applyCommon(Properties properties) {
    properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerConnect);
    if (isSecured) {
      properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
      properties.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
    }

    if (props != null) {
      final var propsDecoded = new String(Base64.getDecoder().decode(props));
      final var extraProps = new Properties();
      try (var propsReader = new StringReader(propsDecoded)) {
        extraProps.load(propsReader);
      } catch (IOException e) {
        throw new KafkaConfigurationException(e);
      }
      properties.putAll(extraProps);
    }
  }
}
