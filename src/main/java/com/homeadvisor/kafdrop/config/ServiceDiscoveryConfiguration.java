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

import com.homeadvisor.kafdrop.util.JmxUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.EnsurePath;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@ConditionalOnProperty("curator.discovery.enabled")
public class ServiceDiscoveryConfiguration
{
   @Autowired
   private Environment environment;

   @Autowired
   private InfoEndpoint infoEndpoint;

   @Value("${spring.jmx.default_domain}")
   private String jmxDomain;

   @Bean(initMethod = "start", destroyMethod = "close")
   public ServiceDiscovery curatorServiceDiscovery(
      CuratorFramework curatorFramework,
      @Value("${curator.discovery.basePath:/homeadvisor/services}") String basePath) throws Exception
   {
      final Class payloadClass = Object.class;
      curatorFramework.createContainers(basePath);
      return ServiceDiscoveryBuilder.builder(payloadClass)
         .client(curatorFramework)
         .basePath(basePath)
         .serializer(new JsonInstanceSerializer(payloadClass))
         .build();
   }


   public Map<String, Object> serviceDetails(Integer serverPort)
   {
      Map<String, Object> details = new LinkedHashMap<>();

      Optional.ofNullable(infoEndpoint.invoke())
         .ifPresent(infoMap -> Optional.ofNullable((Map<String, Object>) infoMap.get("build"))
            .ifPresent(buildInfo -> {
               details.put("serviceName", buildInfo.get("artifact"));
               details.put("serviceDescription", buildInfo.get("description"));
               details.put("serviceVersion", buildInfo.get("version"));
            }));

      final String name = (String) details.getOrDefault("serviceName", "kafdrop");

      String host = null;
      try
      {
         host = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e)
      {
         host = "<unknown>";
      }

      details.put("id", Stream.of(name, host, UUID.randomUUID().toString()).collect(Collectors.joining("_")));
      details.put("name", name);
      details.put("host", host);
      details.put("jmxPort", JmxUtils.getJmxPort(environment));
      details.put("jmxHealthMBean",
                  jmxDomain + ":name=" + healthCheckBeanName() + ",type=" + ClassUtils.getShortName(HealthCheckConfiguration.HealthCheck.class));
      details.put("port", serverPort);

      return details;
   }

   private String healthCheckBeanName()
   {
      String shortClassName = ClassUtils.getShortName(HealthCheckConfiguration.HealthCheck.class);
      return Introspector.decapitalize(shortClassName);
   }

   @Bean
   @ConditionalOnMissingBean
   @ConditionalOnWebApplication
   public ServiceInstance serviceInstance(EmbeddedWebApplicationContext webContext,
                                          ServiceDiscovery serviceDiscovery)
      throws Exception
   {
      final Map<String, Object> details = serviceDetails(getServicePort(webContext));

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

   private Integer getServicePort(EmbeddedWebApplicationContext webContext)
   {
      Integer port = null;
      if (webContext != null)
      {
         final EmbeddedServletContainer container = webContext.getEmbeddedServletContainer();

         if (container instanceof TomcatEmbeddedServletContainer)
         {
            // Work around an issue with the tomcat container, which uses the local port
            // as the port (-1) instead of the registered port
            port = ((TomcatEmbeddedServletContainer) container).getTomcat().getConnector().getPort();
         }
         else
         {
            port = container.getPort();
         }
      }
      return port;
   }
}
