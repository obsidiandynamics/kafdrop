#!/usr/bin/env bash

# Variables for single point of customization
BOOTSTRAP_SERVER="localhost:9092"
VERSION="4.1.0"
LISTEN_PORT=8080

# Running from JAR
exec java --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    -jar kafdrop-"${VERSION}".jar \
    --kafka.brokerConnect="${BOOTSTRAP_SERVER}" \
    --server.port=${LISTEN_PORT} --management.server.port=${LISTEN_PORT}
