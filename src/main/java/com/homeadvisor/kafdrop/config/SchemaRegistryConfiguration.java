package com.homeadvisor.kafdrop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Configuration
public class SchemaRegistryConfiguration {

    @Component
    @ConfigurationProperties(prefix = "schemaregistry")
    public static class SchemaRegistryProperties
    {
        private String connect;

        public String getConnect()
        {
            return connect;
        }

        public void setConnect(String connect)
        {
            this.connect = connect;
        }

    }

}
