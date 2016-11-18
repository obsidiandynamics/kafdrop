# Kafdrop

Kafdrop is a UI for monitoring Apache Kafka clusters. The tool displays information such as brokers, topics, partitions, and even lets you view messages. It is a light weight application that runs on Spring Boot and requires very little configuration.

## Requirements

* Java 8
* Kafka (0.8.1 or 0.8.2 is known to work)
* Zookeeper (3.4.5 or later)

## Building

After cloning the repository, building should just be a matter of running a standard Maven build:

```
$ mvn clean package
```

## Running Stand Alone

The build process creates an executable JAR file.  

```
java -jar ./target/kafdrop-<version>.jar --zookeeper.connect=<host>:<port>,<host>:<port>,...
```

Then open a browser and navigate to http://localhost:9000. The port can be overridden by adding the following config:

```
    --server.port=<port>
```

## Running with Docker

The following maven command will generate a Docker image:

```
    mvn clean package assembly:single docker:build
```

Note for Mac Users: You need to convert newline formatting of the kafdrop.sh file *before* running this command:

```
    dos2unix src/main/docker/*
```

Once the build finishes you can launch the image as follows:

```
    docker run -d -p 9000:9000 -e ZOOKEEPER_CONNECT=<host:port,host:port> kafdrop
```

And access the UI at http://localhost:9000.
