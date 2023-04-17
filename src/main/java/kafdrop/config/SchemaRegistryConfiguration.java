package kafdrop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Configuration
public class SchemaRegistryConfiguration {
  @Component
  @ConfigurationProperties(prefix = "schemaregistry")
  public static final class SchemaRegistryProperties {
    static final Pattern CONNECT_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private String connect;
    private String auth;

    public String getConnect() {
      return connect;
    }

    public void setConnect(String connect) {
      this.connect = connect;
    }

    public String getAuth() {
      return auth;
    }

    public void setAuth(String auth) {
      this.auth = auth;
    }

    public List<String> getConnectList() {
      return CONNECT_SEPARATOR.splitAsStream(this.connect)
        .map(String::trim)
        .filter(s -> s.length() > 0)
        .collect(Collectors.toList());
    }
  }
}
