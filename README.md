Kafdrop 3
===
[![Download](https://api.bintray.com/packages/obsidiandynamics/kafdrop/main/images/download.svg)](https://bintray.com/obsidiandynamics/kafdrop/main/_latestVersion)
[![Build](https://travis-ci.org/obsidiandynamics/kafdrop.svg?branch=master)](https://travis-ci.org/obsidiandynamics/kafdrop#)


Kafdrop 3 is a UI for monitoring Apache Kafka clusters. The tool displays information such as brokers, topics, partitions, consumers and lets you view messages. 

This project is a reboot of [Kafdrop 2.x](https://github.com/HomeAdvisor/Kafdrop), dragged kicking and screaming into the world of JDK 11+, Kafka 2.x and Kubernetes. It's a lightweight application that runs on Spring Boot and requires very little configuration.

# Requirements

* Java 11 or newer
* Kafka (version 0.10.0 or newer)

Optional, additional integration:

* Schema Registry

# Getting Started
You can run the Kafdrop JAR directly, via Docker, or in Kubernetes.

## Running from JAR
```sh
java --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    -jar target/kafdrop-<version>.jar --zookeeper.connect=<host>:<port>,<host>:<port>,...
```

Open a browser and navigate to [http://localhost:9000](http://localhost:9000). The port can be overridden by adding the following config:

```
--server.port=<port>
```

Optionally, configure a schema registry connection with:
```
--schemaregistry.connect=http://localhost:8081
```

Finally, a default message format (e.g. to deserialize Avro messages) can optionally be configured as follows:
```
--message.format=AVRO
```
Valid format values are `DEFAULT` and `AVRO`. This can also be configured at the topic level via dropdown when viewing messages.

## Running with Docker
Images are hosted at [hub.docker.com/r/obsidiandynamics/kafdrop](https://hub.docker.com/r/obsidiandynamics/kafdrop).

Launch container in background:
```sh
docker run -d --rm -p 9000:9000 \
    -e ZOOKEEPER_CONNECT=<host:port,host:port> \
    -e KAFKA_BROKERCONNECT=<host:port,host:port> \
    -e JVM_OPTS="-Xms32M -Xmx64M" \
    -e SERVER_SERVLET_CONTEXTPATH="/" \
    obsidiandynamics/kafdrop
```

Then access the UI at [http://localhost:9000](http://localhost:9000).

## Running in Kubernetes (using a Helm Chart)
Clone the repository (if necessary):
```sh
git clone https://github.com/obsidiandynamics/kafdrop && cd kafdrop
```

Apply the chart:
```sh
helm upgrade -i kafdrop chart --set image.tag=3.0.0 \
    --set zkConnect=<host:port,host:port> \
    --set kafkaBrokerConnect=<host:port,host:port> \
    --set jvm.opts="-Xms32M -Xmx64M"
```

Replace `3.0.0` with the image tag of [obsidiandynamics/kafdrop](https://hub.docker.com/r/obsidiandynamics/kafdrop). Services will be bound on port 9000 by default (node port 30900).

Proxy to the Kubernetes cluster:
```sh
kubectl proxy
```

Navigate to [http://localhost:8001/api/v1/namespaces/default/services/http:kafdrop:9000/proxy](http://localhost:8001/api/v1/namespaces/default/services/http:kafdrop:9000/proxy).

## Building
After cloning the repository, building is just a matter of running a standard Maven build:
```sh
$ mvn clean package
```

The following command will generate a Docker image:
```sh
mvn assembly:single docker:build
```

## Docker Compose
There is a `docker-compose.yaml` file that bundles a Kafka/ZooKeeper instance with Kafdrop:
```sh
cd docker-compose/kafka-kafdrop
docker-compose up
```

# APIs
## JSON endpoints
Starting with version 2.0.0, Kafdrop offers a set of Kafka APIs that mirror the existing HTML views. Any existing endpoint can be returned as JSON by simply setting the `Accept: application/json` header. Some endpoints are JSON only:

* `/topic`: Returns a list of all topics.

## Swagger
To help document the Kafka APIs, Swagger has been included. The Swagger output is available by default at the following Kafdrop URL:
```
/v2/api-docs
```

This can be overridden with the following configuration:
```
springfox.documentation.swagger.v2.path=/new/swagger/path
```

Currently only the JSON endpoints are included in the Swagger output; the HTML views and Spring Boot debug endpoints are excluded.

You can disable Swagger output with the following configuration:
```
swagger.enabled=false
```

## CORS Headers
Starting in version 2.0.0, Kafdrop sets CORS headers for all endpoints. You can control the CORS header values with the following configurations:
```
cors.allowOrigins (default is *)
cors.allowMethods (default is GET,POST,PUT,DELETE)
cors.maxAge (default is 3600)
cors.allowCredentials (default is true)
cors.allowHeaders (default is Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization)
```

You can also disable CORS entirely with the following configuration:
```
cors.enabled=false
```

## Actuator
Health and info endpoints are available at the following path: /actuator

This can be overridden with the following configuration:
```
management.endpoints.web.base-path=<path>
```