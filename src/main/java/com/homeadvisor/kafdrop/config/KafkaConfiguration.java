package com.homeadvisor.kafdrop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by Satendra Sahu on 9/26/18
 */
@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaConfiguration
{
    private String env = "local";
    private String brokerConnect;
    private Boolean isSecured = false;
}
