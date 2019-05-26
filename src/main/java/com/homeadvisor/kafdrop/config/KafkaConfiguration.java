package com.homeadvisor.kafdrop.config;

import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

/**
 * Created by Satendra Sahu on 9/26/18
 */
@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaConfiguration {
  private String env = "local";
  private String brokerConnect;
  private Boolean isSecured = false;
  private String keyDeserializer;
  private String valueDeserializer;
  private String saslMechanism;
  private String securityProtocol;
}
