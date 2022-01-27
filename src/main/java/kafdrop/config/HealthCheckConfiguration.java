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

import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.*;
import org.springframework.jmx.export.annotation.*;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.stream.*;

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
      final var health = (Health) healthEndpoint.health();
      final var healthMap = new LinkedHashMap<String, Object>();
      healthMap.put("status", getStatus(health));
      healthMap.put("detail", getDetails(health.getDetails()));
      return healthMap;
    }

    private Map<String, Object> getDetails(Map<String, Object> details) {
      return details.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> {
                                      final var health = (Health) e.getValue();
                                      final var detail = new LinkedHashMap<String, Object>();
                                      final var healthy = Status.UP.equals(health.getStatus());
                                      detail.put("healthy", healthy);
                                      detail.put("message", health.getDetails().toString());
                                      return detail;
                                    }));
    }

    private String getStatus(Health health) {
      final var status = health.getStatus();
      if (Status.UP.equals(status) || Status.DOWN.equals(status)) {
        return status.toString();
      } else {
        return "ERROR";
      }
    }
  }
}
