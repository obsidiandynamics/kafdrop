package com.homeadvisor.kafdrop.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
public class CuratorConfiguration
{
   @Bean(initMethod = "start", destroyMethod = "close")
   public CuratorFramework curatorFramework(ZookeeperProperties props)
   {
      return CuratorFrameworkFactory.builder()
         .connectString(props.getConnect())
         .connectionTimeoutMs(props.getConnectTimeoutMillis())
         .sessionTimeoutMs(props.getSessionTimeoutMillis())
         .retryPolicy(new RetryNTimes(props.getMaxRetries(), props.getRetryMillis()))
         .build();
   }

   @Component
   @ConfigurationProperties(prefix = "zookeeper")
   public static class ZookeeperProperties
   {
      public static final Pattern CONNECT_SEPARATOR = Pattern.compile("\\s*,\\s*");
      @NotBlank
      private String connect;

      private int sessionTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(5);
      private int connectTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(15);

      private int retryMillis = (int) TimeUnit.SECONDS.toMillis(5);
      private int maxRetries = Integer.MAX_VALUE;


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

      public int getRetryMillis()
      {
         return retryMillis;
      }

      public void setRetryMillis(int retryMillis)
      {
         this.retryMillis = retryMillis;
      }

      public int getMaxRetries()
      {
         return maxRetries;
      }

      public void setMaxRetries(int maxRetries)
      {
         this.maxRetries = maxRetries;
      }

      public int getSessionTimeoutMillis()
      {
         return sessionTimeoutMillis;
      }

      public void setSessionTimeoutMillis(int sessionTimeoutMillis)
      {
         this.sessionTimeoutMillis = sessionTimeoutMillis;
      }

      public int getConnectTimeoutMillis()
      {
         return connectTimeoutMillis;
      }

      public void setConnectTimeoutMillis(int connectTimeoutMillis)
      {
         this.connectTimeoutMillis = connectTimeoutMillis;
      }
   }

   @Component(value = "curatorConnection")
   private static class CuratorHealthIndicator extends AbstractHealthIndicator
   {
      private final CuratorFramework framework;

      @Autowired
      public CuratorHealthIndicator(CuratorFramework framework)
      {
         this.framework = framework;
      }

      @Override
      protected void doHealthCheck(Health.Builder builder) throws Exception
      {
         if (framework.getZookeeperClient().isConnected())
         {
            builder.up();
         }
         else
         {
            builder.down();
         }
      }
   }
}
