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
