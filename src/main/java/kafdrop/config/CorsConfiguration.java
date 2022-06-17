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

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.core.annotation.*;
import org.springframework.http.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * Auto configuration for enabling CORS support. Can override behavior with
 * various configs:
 *
 * <ul>
 *    <li>cors.enabled</li>
 *    <li>cors.allowOrigins</li>
 *    <li>cors.allowMethods</li>
 *    <li>cors.maxAge</li>
 *    <li>cors.allowCredentials</li>
 *    <li>cors.allowHeaders</li>
 * </ul>
 * <br/>
 * To disable CORS entirely, set <b>cors.enabled=false</b>. All other configs are
 * just Strings that get used as-is to set the corresponding CORS header.
 */
@Configuration
@ConditionalOnProperty(value = "cors.enabled", matchIfMissing = true)
public class CorsConfiguration {
  @Value("${cors.allowOrigins:*}")
  private String corsAllowOrigins;

  @Value("${cors.allowMethods:GET,POST,PUT,DELETE}")
  private String corsAllowMethods;

  @Value("${cors.maxAge:3600}")
  private String corsMaxAge;

  @Value("${cors.allowCredentials:true}")
  private String corsAllowCredentials;

  @Value("${cors.allowHeaders:Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization}")
  private String corsAllowHeaders;

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public Filter corsFilter() {
    return new Filter() {
      @Override
      public void init(FilterConfig filterConfig) {
        // nothing to init
      }

      @Override
      public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        final var response = (HttpServletResponse) res;
        final var request = (HttpServletRequest) req;

        response.setHeader("Access-Control-Allow-Origin", corsAllowOrigins);
        response.setHeader("Access-Control-Allow-Methods", corsAllowMethods);
        response.setHeader("Access-Control-Max-Age", corsMaxAge);
        response.setHeader("Access-Control-Allow-Credentials", corsAllowCredentials);
        response.setHeader("Access-Control-Allow-Headers", corsAllowHeaders);

        if (request.getMethod().equals(HttpMethod.OPTIONS.name())) {
          response.setStatus(HttpStatus.NO_CONTENT.value());
        } else {
          chain.doFilter(req, res);
        }
      }

      @Override
      public void destroy() {
        // nothing to destroy
      }
    };
  }
}
