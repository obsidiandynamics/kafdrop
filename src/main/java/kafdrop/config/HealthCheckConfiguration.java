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

import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class HealthCheckConfiguration {
  @Component
  @ManagedResource
  public static final class HealthCheck {
    private final HealthEndpoint healthEndpoint;

    public HealthCheck(HealthEndpoint healthEndpoint) {
      this.healthEndpoint = healthEndpoint;
    }

    @ManagedAttribute
    public Map<String, Object> getHealth() {
      final var health = healthEndpoint.health();
      final var healthMap = new LinkedHashMap<String, Object>();
      healthMap.put("status", getStatus(health.getStatus()));
      healthMap.put("detail", getDetails(health));
      return healthMap;
    }

    private Map<String, Object> getDetails(HealthDescriptor healthDescriptor) {
      if (healthDescriptor instanceof CompositeHealthDescriptor composite) {
        final var result = new LinkedHashMap<String, Object>();
        composite.getComponents().forEach((key, component) -> result.put(key, describeComponent(component)));
        return result;
      }
      if (healthDescriptor instanceof IndicatedHealthDescriptor indicated) {

        return indicated.getDetails();
      }
      return Map.of();
    }

    private Object describeComponent(HealthDescriptor component) {
      if (component instanceof CompositeHealthDescriptor composite) {
        return getDetails(composite);
      }
      final var detail = new LinkedHashMap<String, Object>();
      detail.put("healthy", Status.UP.equals(component.getStatus()));
      detail.put("message", component instanceof IndicatedHealthDescriptor indicated
        ? indicated.getDetails().toString()
        : component.getStatus().toString());
      return detail;
    }

    private String getStatus(Status status) {
      if (Status.UP.equals(status) || Status.DOWN.equals(status)) {
        return status.toString();
      } else {
        return "ERROR";
      }
    }
  }
}
