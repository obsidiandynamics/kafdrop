package com.homeadvisor.kafdrop.config;

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
   public static class SchemaRegistryProperties
   {

      public static final Pattern CONNECT_SEPARATOR = Pattern.compile("\\s*,\\s*");

      private String connect;

      public String getConnect()
      {
         return connect;
      }

      public void setConnect(String connect)
      {
         this.connect = connect;
      }

      public List<String> getConnectList()
      {
         return CONNECT_SEPARATOR.splitAsStream(this.connect)
               .map(String::trim)
               .filter(s -> s.length() > 0)
               .collect(Collectors.toList());
      }

   }

}
