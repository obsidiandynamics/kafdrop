/*
 * Copyright 2017 Kafdrop contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package kafdrop.config;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import springfox.documentation.*;
import springfox.documentation.builders.*;
import springfox.documentation.spi.*;
import springfox.documentation.spring.web.plugins.*;
import springfox.documentation.swagger2.annotations.*;

import java.util.function.Predicate;

/**
 *  Auto configuration for Swagger. Can be disabled by setting {@code swagger.enabled=false}.
 */
@Configuration
@EnableSwagger2
@ConditionalOnProperty(value = "swagger.enabled", matchIfMissing = true)
public class SwaggerConfiguration {
  @Bean
  public Docket swagger() {
    return new Docket(DocumentationType.SWAGGER_2)
        .useDefaultResponseMessages(false)
        .apiInfo(new ApiInfoBuilder()
                     .title("Kafdrop API")
                     .description("JSON APIs for Kafdrop")
                     .build())
        .select()
        .apis(new JsonRequestHandlerPredicate())
        .paths(new IgnoreDebugPathPredicate())
        .build();
  }

  /**
   *  Swagger Predicate for only selecting JSON endpoints.
   */
  public static final class JsonRequestHandlerPredicate implements Predicate<RequestHandler> {
    @Override
    public boolean test(RequestHandler input) {
      return input.produces().contains(MediaType.APPLICATION_JSON);
    }
  }

  /**
   *  Swagger Predicate for ignoring {@code /actuator} endpoints.
   */
  public static final class IgnoreDebugPathPredicate implements Predicate<String> {
    @Override
    public boolean test(String input) {
      return !input.startsWith("/actuator");
    }
  }
}
