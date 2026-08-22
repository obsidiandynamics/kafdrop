package kafdrop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.TimeZone;

@Configuration
public class ObjectMapperConfig {

  @Bean
  public JsonMapper objectMapper(JsonMapper.Builder builder) {
    return builder
      .defaultTimeZone(TimeZone.getDefault())
      .build();
  }
}
