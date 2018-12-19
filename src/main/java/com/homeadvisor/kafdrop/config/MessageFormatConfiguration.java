package com.homeadvisor.kafdrop.config;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.homeadvisor.kafdrop.util.MessageFormat;


@Configuration
public class MessageFormatConfiguration {

   @Component
   @ConfigurationProperties(prefix = "message")
   public static class MessageFormatProperties
   {

      private MessageFormat format;

      @PostConstruct
      public void init() {
         // Set a default message format if not configured.
         if (format == null) {
            format = MessageFormat.DEFAULT;
         }
      }

      public MessageFormat getFormat()
      {
         return format;
      }

      public void setFormat(MessageFormat format)
      {
         this.format = format;
      }

   }

}
