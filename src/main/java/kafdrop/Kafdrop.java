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

package kafdrop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kafdrop.config.ini.IniFilePropertySource;
import kafdrop.config.ini.IniFileReader;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

@SpringBootApplication
public class Kafdrop {
  private static final Logger LOG = LoggerFactory.getLogger(Kafdrop.class);

  public static void main(String[] args) {
    createApplicationBuilder()
      .run(args);
  }

  public static SpringApplicationBuilder createApplicationBuilder() {
    return new SpringApplicationBuilder(Kafdrop.class)
      .bannerMode(Mode.OFF)
      .listeners(new EnvironmentSetupListener(),
        new LoggingConfigurationListener());
  }

  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> blockTrackFilter() {
    FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setFilter(new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        if ("TRACK".equals(method)) {
          response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "TRACK method is not allowed");
          return;
        }
        filterChain.doFilter(request, response);
      }
    });
    return registration;
  }

  private static final class LoggingConfigurationListener
    implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    private static final String PROP_LOGGING_FILE = "logging.file";
    private static final String PROP_LOGGER = "LOGGER";
    private static final String PROP_SPRING_BOOT_LOG_LEVEL = "logging.level.org.springframework.boot";

    @Override
    public int getOrder() {
      // LoggingApplicationListener runs at HIGHEST_PRECEDENCE + 11.  This needs to run before that.
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
      final var environment = event.getEnvironment();
      final var loggingFile = environment.getProperty(PROP_LOGGING_FILE);
      if (loggingFile != null) {
        System.setProperty(PROP_LOGGER, "FILE");
        try {
          System.setProperty("logging.dir", new File(loggingFile).getParent());
        } catch (Exception ex) {
          LOG.error("Unable to set up logging.dir from logging.file {}", loggingFile, ex);
        }
      }
      if (environment.containsProperty("debug") &&
        !"false".equalsIgnoreCase(environment.getProperty("debug", String.class))) {
        System.setProperty(PROP_SPRING_BOOT_LOG_LEVEL, "DEBUG");
      }
    }
  }

  private static final class EnvironmentSetupListener
    implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    private static final String SM_CONFIG_DIR = "sm.config.dir";
    private static final String CONFIG_SUFFIX = "-config.ini";

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
      final var environment = event.getEnvironment();

      LOG.info("Initializing JAAS config");
      final String env = environment.getProperty("kafka.env");
      LOG.info("Env: {}", env);
      String path;

      if (environment.containsProperty(SM_CONFIG_DIR)) {
        Stream.of("kafdrop", "global")
          .map(name -> readProperties(environment, name))
          .filter(Objects::nonNull)
          .forEach(iniPropSource -> environment.getPropertySources()
            .addBefore("applicationConfigurationProperties", iniPropSource));
      }
    }

    private static IniFilePropertySource readProperties(Environment environment, String name) {
      final var file = new File(environment.getProperty(SM_CONFIG_DIR), name + CONFIG_SUFFIX);
      if (file.exists() && file.canRead()) {
        try (var in = new FileInputStream(file);
             var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
          return new IniFilePropertySource(name, new IniFileReader().read(reader), environment.getActiveProfiles());
        } catch (IOException ex) {
          LOG.error("Unable to read configuration file {}: {}", file, ex);
        }
      }
      return null;
    }
  }
}
