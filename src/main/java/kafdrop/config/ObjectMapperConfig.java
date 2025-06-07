package kafdrop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

@Configuration
public class ObjectMapperConfig {

  @Bean
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder
      .timeZone(TimeZone.getDefault())
      .build();
  }
}
