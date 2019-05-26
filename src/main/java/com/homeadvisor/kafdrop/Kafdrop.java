/*
 * Copyright 2017 HomeAdvisor, Inc.
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

package com.homeadvisor.kafdrop;

import com.google.common.base.*;
import com.homeadvisor.kafdrop.config.ini.*;
import joptsimple.internal.Strings;
import org.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.actuate.autoconfigure.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.core.env.*;
import org.springframework.web.servlet.config.annotation.*;

import java.io.*;
import java.nio.charset.*;
import java.util.Objects;
import java.util.stream.*;

@SpringBootApplication(exclude = MetricFilterAutoConfiguration.class)
public class Kafdrop {
  private final static Logger LOG = LoggerFactory.getLogger(Kafdrop.class);

  public static void main(String[] args) {
    new SpringApplicationBuilder(Kafdrop.class)
        .bannerMode(Banner.Mode.OFF)
        .listeners(new EnvironmentSetupListener(),
                   new LoggingConfigurationListener())
        .run(args);
  }

  @Bean
  public WebMvcConfigurerAdapter webConfig() {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        super.configureContentNegotiation(configurer);
        configurer.favorPathExtension(false);
      }
    };
  }

  private static class LoggingConfigurationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    private static final String PROP_LOGGING_CONFIG = "logging.config";
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
      Environment environment = event.getEnvironment();
      final String loggingFile = environment.getProperty(PROP_LOGGING_FILE);
      if (loggingFile != null) {
        System.setProperty(PROP_LOGGER, "FILE");
        try {
          System.setProperty("logging.dir", new File(loggingFile).getParent());
        } catch (Exception ex) {
          System.err.println("Unable to set up logging.dir from logging.file " + loggingFile + ": " +
                                 Throwables.getStackTraceAsString(ex));
        }
      }
      if (environment.containsProperty("debug") &&
          !"false".equalsIgnoreCase(environment.getProperty("debug", String.class))) {
        System.setProperty(PROP_SPRING_BOOT_LOG_LEVEL, "DEBUG");
      }
    }
  }

  private static class EnvironmentSetupListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    private static final String SM_CONFIG_DIR = "sm.config.dir";
    private static final String CONFIG_SUFFIX = "-config.ini";

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
      final ConfigurableEnvironment environment = event.getEnvironment();

      LOG.info("Initializing jaas config");
      String env = environment.getProperty("kafka.env");
      Boolean isSecured = environment.getProperty("kafka.isSecured", Boolean.class);
      LOG.info("env: {} .isSecured kafka: {}", env, isSecured);
      if (isSecured && Strings.isNullOrEmpty(env)) {
        throw new RuntimeException("'env' cannot be null if connecting to secured kafka.");
      }

      LOG.info("ENV: {}", env);
      String path;

      if (isSecured) {
        if ((env.equalsIgnoreCase("stage") || env.equalsIgnoreCase("prod") || env.equalsIgnoreCase("local"))) {
          path = environment.getProperty("user.dir") + "/kaas_" + env.toLowerCase() + "_jaas.conf";
          LOG.info("PATH: {}", path);
          System.setProperty("java.security.auth.login.config", path);
        } else {
          throw new RuntimeException("unable to identify env. set 'evn' variable either to 'stage' or 'prod' or local");
        }
      }

      if (environment.containsProperty(SM_CONFIG_DIR)) {
        Stream.of("kafdrop", "global")
            .map(name -> readProperties(environment, name))
            .filter(Objects::nonNull)
            .forEach(iniPropSource -> environment.getPropertySources()
                .addBefore("applicationConfigurationProperties", iniPropSource));
      }
    }

    private IniFilePropertySource readProperties(Environment environment, String name) {
      final File file = new File(environment.getProperty(SM_CONFIG_DIR), name + CONFIG_SUFFIX);
      if (file.exists() && file.canRead()) {
        try (InputStream in = new FileInputStream(file);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
          return new IniFilePropertySource(name, new IniFileReader().read(reader), environment.getActiveProfiles());
        } catch (IOException ex) {
          LOG.error("Unable to read configuration file {}", file, ex);
        }
      }
      return null;
    }
  }
}
