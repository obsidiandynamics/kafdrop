#!/bin/bash

# Set marathon ports to 0:0 to have marathon assign and pass random port
if [ $PORT0 ]; then
    JMX_PORT=$PORT0;
fi

# Marathon passes "HOST" variable
if [ -z $HOST ]; then
    HOST=localhost;
fi

# Marathon passes memory limit
if [ $MARATHON_APP_RESOURCE_MEM ]; then
    HEAP_ARGS="-Xms${MARATHON_APP_RESOURCE_MEM%.*}m -Xmx${MARATHON_APP_RESOURCE_MEM%.*}m"
fi

if [ $JMX_PORT ]; then
    JMX_ARGS="-Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
    -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=$HOST"
fi

ARGS="--add-exports=jdk.management.agent/jdk.internal.agent=ALL-UNNAMED \
     -Xss256K \
     $JMX_ARGS \
     $HEAP_ARGS \
     $JVM_ARGS"

java $ARGS -jar /kafdrop*/kafdrop*jar

