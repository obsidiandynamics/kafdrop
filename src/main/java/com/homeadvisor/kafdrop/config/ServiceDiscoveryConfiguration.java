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

package com.homeadvisor.kafdrop.config;

import com.homeadvisor.kafdrop.util.*;
import org.apache.curator.framework.*;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.web.servlet.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.util.*;

import java.beans.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

@Configuration
@ConditionalOnProperty("curator.discovery.enabled")
public class ServiceDiscoveryConfiguration {
  private final InfoEndpoint infoEndpoint;

  @Value("${spring.jmx.default_domain}")
  private String jmxDomain;

  public ServiceDiscoveryConfiguration(InfoEndpoint infoEndpoint) {
    this.infoEndpoint = infoEndpoint;
  }

  @Bean(initMethod = "start", destroyMethod = "close")
  public ServiceDiscovery curatorServiceDiscovery(
      CuratorFramework curatorFramework,
      @Value("${curator.discovery.basePath:/homeadvisor/services}") String basePath) throws Exception {
    final Class<Object> payloadClass = Object.class;
    curatorFramework.createContainers(basePath);
    return ServiceDiscoveryBuilder.builder(payloadClass)
        .client(curatorFramework)
        .basePath(basePath)
        .serializer(new JsonInstanceSerializer<>(payloadClass))
        .build();
  }

  private Map<String, Object> serviceDetails(Integer serverPort) {
    Map<String, Object> details = new LinkedHashMap<>();

    Optional.ofNullable(infoEndpoint.info())
        .ifPresent(infoMap -> Optional.ofNullable((Map<String, Object>) infoMap.get("build"))
            .ifPresent(buildInfo -> {
              details.put("serviceName", buildInfo.get("artifact"));
              details.put("serviceDescription", buildInfo.get("description"));
              details.put("serviceVersion", buildInfo.get("version"));
            }));

    final String name = (String) details.getOrDefault("serviceName", "kafdrop");

    String host;
    try {
      host = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      host = "<unknown>";
    }

    details.put("id", String.join("_", name, host, UUID.randomUUID().toString()));
    details.put("name", name);
    details.put("host", host);
    details.put("jmxPort", JmxUtils.getJmxPort());
    details.put("jmxHealthMBean",
                jmxDomain + ":name=" + healthCheckBeanName() + ",type=" + ClassUtils.getShortName(HealthCheckConfiguration.HealthCheck.class));
    details.put("port", serverPort);

    return details;
  }

  private String healthCheckBeanName() {
    final String shortClassName = ClassUtils.getShortName(HealthCheckConfiguration.HealthCheck.class);
    return Introspector.decapitalize(shortClassName);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnWebApplication
  public ServiceInstance serviceInstance(ServletWebServerApplicationContext webContext,
                                         ServiceDiscovery<Map<String, Object>> serviceDiscovery)
  throws Exception {
    final Map<String, Object> details = serviceDetails(webContext.getWebServer().getPort());

    final ServiceInstanceBuilder<Map<String, Object>> builder = ServiceInstance.builder();
    Optional.ofNullable(details.get("port")).ifPresent(port -> builder.port((Integer) port));

    final ServiceInstance<Map<String, Object>> serviceInstance = builder
        .id((String) details.get("id"))
        .name((String) details.get("name"))
        .payload(details)
        .uriSpec(new UriSpec("http://{address}:{port}"))
        .build();

    serviceDiscovery.registerService(serviceInstance);

    return serviceInstance;
  }
}
