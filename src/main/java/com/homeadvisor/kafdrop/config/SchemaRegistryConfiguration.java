package com.homeadvisor.kafdrop.config;

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
  public static class SchemaRegistryProperties {

    public static final Pattern CONNECT_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private String connect;

    public String getConnect() {
      return connect;
    }

    public void setConnect(String connect) {
      this.connect = connect;
    }

    public List<String> getConnectList() {
      return CONNECT_SEPARATOR.splitAsStream(this.connect)
          .map(String::trim)
          .filter(s -> s.length() > 0)
          .collect(Collectors.toList());
    }

  }

}
