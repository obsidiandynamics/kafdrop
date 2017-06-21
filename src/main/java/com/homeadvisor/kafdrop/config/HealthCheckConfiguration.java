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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class HealthCheckConfiguration
{
   @Component
   @ManagedResource
   public static class HealthCheck
   {
      @Autowired
      private HealthEndpoint healthEndpoint;

      @ManagedAttribute
      public Map getHealth()
      {
         Health health = healthEndpoint.invoke();
         Map healthMap = new LinkedHashMap();
         healthMap.put("status", getStatus(health));
         healthMap.put("detail", getDetails(health.getDetails()));
         return healthMap;
      }

      private Map getDetails(Map<String, Object> details)
      {
         return details.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                                      e -> {
                                         Health health = (Health) e.getValue();
                                         Map<String, Object> detail = new LinkedHashMap<>();
                                         final boolean healthy = Status.UP.equals(health.getStatus());
                                         detail.put("healthy", healthy);
                                         detail.put("message", health.getDetails().toString());
                                         return detail;
                                      }));
      }

      private String getStatus(Health health)
      {
         final Status status = health.getStatus();
         if (Status.UP.equals(status) || Status.DOWN.equals(status)) {
            return status.toString();
         }
         else
         {
            return "ERROR";
         }
      }
   }
}
