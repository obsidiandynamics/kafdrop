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

package kafdrop.util;

import com.google.common.primitives.*;

@SuppressWarnings("UnstableApiUsage")
public final class JmxUtils {
  private static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";

  private JmxUtils() {
  }

  public static int getJmxPort() {
    final var managementProperties = jdk.internal.agent.Agent.getManagementProperties();
    if (managementProperties != null) {
      final var portProperty = managementProperties.getProperty(JMX_PORT_PROPERTY);
      if (portProperty != null) {
        return Ints.tryParse(portProperty);
      } else {
        return 0;
      }
    } else {
      return 0;
    }
  }
}
