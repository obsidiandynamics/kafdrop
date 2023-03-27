package kafdrop.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class GlueSchemaRegistryConfiguration {
    @Component
    @ConfigurationProperties(prefix = "schemaregistry.glue")
    public static final class GlueSchemaRegistryProperties {
        private String registryName;
        private String region;

        private String awsEndpoint;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getRegistryName() {
            return registryName;
        }

        public void setRegistryName(String registryName) {
            this.registryName = registryName;
        }

        public String getAwsEndpoint() {
            return awsEndpoint;
        }

        public void setAwsEndpoint(String awsEndpoint) {
            this.awsEndpoint = awsEndpoint;
        }

        public boolean isConfigured() {
            return (StringUtils.isNotEmpty(region) && StringUtils.isNotEmpty(registryName));

        }
    }
}
