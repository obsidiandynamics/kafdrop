# Kafdrop

Kafdrop is a UI for monitoring Apache Kafka clusters. The tool displays information such as brokers, topics, partitions, and even lets you view messages. It is a light weight application that runs on Spring Boot and requires very little configuration.

# Running Kafdrop

Clone this repo and run the following commands from the top level:

    > mvn package
    > java -jar ./target/kafdrop-<VERSION>.jar --zookeeper.connect=<zookeeper host:port>

The open a browser and navigate to http://localhost:9000. The port can be overridden by adding the following config:

    --server.port=<port>
