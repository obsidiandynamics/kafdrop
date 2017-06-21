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

package com.homeadvisor.kafdrop.util;

import com.google.common.primitives.Ints;
import org.springframework.core.env.Environment;
import sun.management.Agent;

import java.util.Optional;
import java.util.Properties;

public abstract class JmxUtils
{
   public static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";

   public static int getJmxPort(final Environment environment)
   {
      Optional<Integer> jmxPort = Optional.empty();

      final Properties managementProperties = Agent.getManagementProperties();
      if (managementProperties != null)
      {
         final String portProperty = managementProperties.getProperty(JMX_PORT_PROPERTY);
         if (portProperty != null)
         {
            final Optional<Integer> port = Optional.ofNullable(Ints.tryParse(portProperty));
            jmxPort = port;
         }
      }
      return jmxPort.orElse(0);
   }
}
