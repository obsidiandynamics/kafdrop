package kafdrop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.*;


@Configuration
public class SchemaRegistryConfiguration {
  @Component
  @ConfigurationProperties(prefix = "schemaregistry")
  public static final class SchemaRegistryProperties {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryConfiguration.class);
    static final Pattern CONNECT_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private String connect;
    private String auth;
    private String propertiesFile;

    public String getConnect() {
      return connect;
    }

    public void setConnect(String connect) {
      this.connect = connect;
    }

    public String getAuth() { return auth; }

    public void setAuth(String auth) { this.auth = auth; }

    public List<String> getConnectList() {
      return CONNECT_SEPARATOR.splitAsStream(this.connect)
          .map(String::trim)
          .filter(s -> s.length() > 0)
          .collect(Collectors.toList());
    }

    public String getPropertiesFile(){
      LOG.debug("Returning property path: {}", propertiesFile);
      return propertiesFile;
    }

    public void setPropertiesFile(String propertiesFile) {this.propertiesFile = propertiesFile;}

  }
}
