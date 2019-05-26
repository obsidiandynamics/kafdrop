Kafdrop 3
===

Kafdrop 3 is a UI for monitoring Apache Kafka clusters. The tool displays information such as brokers, topics, partitions, and lets you view messages. 

The project is a continuation of the original [HomeAdvisor/Kafdrop](https://github.com/HomeAdvisor/Kafdrop), which has now been dragged kicking and screaming into the JDK 11 world. It's a lightweight application that runs on Spring Boot and requires very little configuration.

# Requirements

* Java 11 or newer
* Kafka + ZooKeeper cluster

Optional, additional integration:

* Schema Registry

# Getting Started
## Maven build
After cloning the repository, building should just be a matter of running a standard Maven build:

```sh
$ mvn clean package
```

## Running from JAR
```sh
java --add-exports=jdk.management.agent/jdk.internal.agent=ALL-UNNAMED \
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
Valid format values are `DEFAULT` and `AVRO`. This setting can also be configured at the topic level via dropdown when viewing messages.

## Running with Docker
The following maven command will generate a Docker image:

```sh
mvn clean package assembly:single docker:build
```

Once the build finishes you can launch the image:

```sh
docker run -d -p 9000:9000 -e ZOOKEEPER_CONNECT=<host:port,host:port> obsidiandynamics/kafdrop
```

And access the UI at [http://localhost:9000](http://localhost:9000).

# APIs
## JSON endpoints
Starting with version 2.0.0, Kafdrop offers a set of Kafka APIs that mirror the existing HTML views. Any existing endpoint can be returned as JSON by simply setting the `Accept: application/json` header. There are also two endpoints that are JSON only:

* `/topic`: Returns array of all topic names.
* `/topic/{topicName}/{consumerId}`: Returns partition offset and lag details for a specific topic and consumer.

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