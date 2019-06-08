package com.homeadvisor.kafdrop.config;

import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaConfiguration {
  private String env = "local";
  private String brokerConnect;
  private Boolean isSecured = false;
  private String saslMechanism;
  private String securityProtocol;
}
