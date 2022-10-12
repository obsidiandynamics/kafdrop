<img src="https://raw.githubusercontent.com/wiki/obsidiandynamics/kafdrop/images/kafdrop-logo.png" width="90px" alt="logo"/> Kafdrop – Kafka Web UI &nbsp; [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fobsidiandynamics%2Fkafdrop&text=Get%20Kafdrop%20%E2%80%94%20a%20web-based%20UI%20for%20viewing%20%23ApacheKafka%20topics%20and%20browsing%20consumers%20)
===

[![Price](https://img.shields.io/badge/price-FREE-0098f7.svg)](https://github.com/obsidiandynamics/kafdrop/blob/master/LICENSE)
[![Release with mvn](https://github.com/obsidiandynamics/kafdrop/actions/workflows/master.yml/badge.svg)](https://github.com/obsidiandynamics/kafdrop/actions/workflows/master.yml)
[![Docker](https://img.shields.io/docker/pulls/obsidiandynamics/kafdrop.svg)](https://hub.docker.com/r/obsidiandynamics/kafdrop)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/obsidiandynamics/kafdrop.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/obsidiandynamics/kafdrop/context:java)


<em>Kafdrop is a web UI for viewing Kafka topics and browsing consumer groups.</em> The tool displays information such as brokers, topics, partitions, consumers, and lets you view messages. 

![Overview Screenshot](docs/images/overview.png?raw=true)

This project is a reboot of Kafdrop 2.x, dragged kicking and screaming into the world of JDK 11+, Kafka 2.x, Helm and Kubernetes. It's a lightweight application that runs on Spring Boot and is dead-easy to configure, supporting SASL and TLS-secured brokers.

# Features
* **View Kafka brokers** — topic and partition assignments, and controller status
* **View topics** — partition count, replication status, and custom configuration
* **Browse messages** — JSON, plain text, Avro and Protobuf encoding
* **View consumer groups** — per-partition parked offsets, combined and per-partition lag
* **Create new topics**
* **View ACLs**
* **Support for Azure Event Hubs**

# Requirements

* Java 11 or newer
* Kafka (version 0.11.0 or newer) or Azure Event Hubs

Optional, additional integration:

* Schema Registry

# Getting Started
You can run the Kafdrop JAR directly, via Docker, or in Kubernetes.

## Running from JAR
```sh
java --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    -jar target/kafdrop-<version>.jar \
    --kafka.brokerConnect=<host:port,host:port>,...
```

If unspecified, `kafka.brokerConnect` defaults to `localhost:9092`.

**Note:** As of Kafdrop 3.10.0, a ZooKeeper connection is no longer required. All necessary cluster information is retrieved via the Kafka admin API.

Open a browser and navigate to [http://localhost:9000](http://localhost:9000). The port can be overridden by adding the following config:

```
--server.port=<port> --management.server.port=<port>
```

Optionally, configure a schema registry connection with:
```
--schemaregistry.connect=http://localhost:8081
```
and if you also require basic auth for your schema registry connection you should add:
```
--schemaregistry.auth=username:password
```

Finally, a default message and key format (e.g. to deserialize Avro messages or keys) can optionally be configured as follows:
```
--message.format=AVRO
--message.keyFormat=DEFAULT
```
Valid format values are `DEFAULT`, `AVRO`, `PROTOBUF`. This can also be configured at the topic level via dropdown when viewing messages. 
If key format is unspecified, message format will be used for key too.

## Configure Protobuf message type
### Option 1: Using Protobuf Descriptor 
In case of protobuf message type, the definition of a message could be compiled and transmitted using a descriptor file. 
Thus, in order for kafdrop to recognize the message, the application will need to access to the descriptor file(s). 
Kafdrop will allow user to select descriptor and well as specifying name of one of the message type provided by the descriptor at runtime. 

To configure a folder with protobuf descriptor file(s) (.desc), follow:
```
--protobufdesc.directory=/var/protobuf_desc
```

### Option 2 : Using Schema Registry
In case of no protobuf descriptor file being supplied the implementation will attempt to create the protobuf deserializer using the schema registry instead.

### Defaulting to Protobuf
If preferred the message type could be set to default as follows:
```
--message.format=PROTOBUF
```

## Running with Docker
Images are hosted at [hub.docker.com/r/obsidiandynamics/kafdrop](https://hub.docker.com/r/obsidiandynamics/kafdrop).

Launch container in background:
```sh
docker run -d --rm -p 9000:9000 \
    -e KAFKA_BROKERCONNECT=<host:port,host:port> \
    -e JVM_OPTS="-Xms32M -Xmx64M" \
    -e SERVER_SERVLET_CONTEXTPATH="/" \
    obsidiandynamics/kafdrop
```

Launch container in background with protobuff definitions:
```sh
docker run -d --rm -v <path_to_protobuff_descriptor_files>:/var/protobuf_desc -p 9000:9000 \
    -e KAFKA_BROKERCONNECT=<host:port,host:port> \
    -e JVM_OPTS="-Xms32M -Xmx64M" \
    -e SERVER_SERVLET_CONTEXTPATH="/" \
    -e CMD_ARGS="--message.format=PROTOBUF --protobufdesc.directory=/var/protobuf_desc" \
    obsidiandynamics/kafdrop
```

Then access the web UI at [http://localhost:9000](http://localhost:9000).

> **Hey there!** We hope you really like Kafdrop! Please take a moment to [⭐](https://github.com/obsidiandynamics/kafdrop)the repo or [Tweet](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fobsidiandynamics%2Fkafdrop&text=Get%20Kafdrop%20%E2%80%94%20a%20web-based%20UI%20for%20viewing%20%23ApacheKafka%20topics%20and%20browsing%20consumers%20) about it.

## Running in Kubernetes (using a Helm Chart)
Clone the repository (if necessary):
```sh
git clone https://github.com/obsidiandynamics/kafdrop && cd kafdrop
```

Apply the chart:
```sh
helm upgrade -i kafdrop chart --set image.tag=3.x.x \
    --set kafka.brokerConnect=<host:port,host:port> \
    --set server.servlet.contextPath="/" \
    --set cmdArgs="--message.format=AVRO --schemaregistry.connect=http://localhost:8080" \ #optional
    --set jvm.opts="-Xms32M -Xmx64M"
```

For all Helm configuration options, have a peek into [chart/values.yaml](chart/values.yaml).

Replace `3.x.x` with the image tag of [obsidiandynamics/kafdrop](https://hub.docker.com/r/obsidiandynamics/kafdrop). Services will be bound on port 9000 by default (node port 30900).

**Note:** The context path _must_ begin with a slash.

Proxy to the Kubernetes cluster:
```sh
kubectl proxy
```

Navigate to [http://localhost:8001/api/v1/namespaces/default/services/http:kafdrop:9000/proxy](http://localhost:8001/api/v1/namespaces/default/services/http:kafdrop:9000/proxy).


### Protobuf support via helm chart:
To install with protobuf support, a "facility" option is provided for the deployment, to mount the descriptor files folder, as well as passing the required CMD arguments, via option _mountProtoDesc_.
Example:

```sh
helm upgrade -i kafdrop chart --set image.tag=3.x.x \
    --set kafka.brokerConnect=<host:port,host:port> \
    --set server.servlet.contextPath="/" \
    --set mountProtoDesc.enabled=true \
    --set mountProtoDesc.hostPath="<path/to/desc/folder>" \
    --set jvm.opts="-Xms32M -Xmx64M"
```

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

## Topic Configuration
By default, you could delete a topic. If you don't want this feature, you could disable it with:

```
--topic.deleteEnabled=false
```

By default, you could create a topic. If you don't want this feature, you could disable it with:

```
--topic.createEnabled=false
```

## Actuator
Health and info endpoints are available at the following path: `/actuator`

This can be overridden with the following configuration:
```
management.endpoints.web.base-path=<path>
```

# Guides
## Connecting to a Secure Broker
Kafdrop supports TLS (SSL) and SASL connections for [encryption and authentication](http://kafka.apache.org/090/documentation.html#security). This can be configured by providing a combination of the following files (placed into the Kafka root directory):

* `kafka.truststore.jks`: specifying the certificate for authenticating brokers, if TLS is enabled.
* `kafka.keystore.jks`: specifying the private key to authenticate the client to the broker, if mutual TLS authentication is required.
* `kafka.properties`: specifying the necessary configuration, including key/truststore passwords, cipher suites, enabled TLS protocol versions, username/password pairs, etc. When supplying the truststore and/or keystore files, the `ssl.truststore.location` and `ssl.keystore.location` properties will be assigned automatically.

### Using Docker
The three files above can be supplied to a Docker instance in base-64-encoded form via environment variables:

```sh
docker run -d --rm -p 9000:9000 \
    -e KAFKA_BROKERCONNECT=<host:port,host:port> \
    -e KAFKA_PROPERTIES="$(cat kafka.properties | base64)" \
    -e KAFKA_TRUSTSTORE="$(cat kafka.truststore.jks | base64)" \   # optional
    -e KAFKA_KEYSTORE="$(cat kafka.keystore.jks | base64)" \       # optional
    obsidiandynamics/kafdrop
```
#### Environment Variables
##### Basic configuration
|Name                   |Description
|-----------------------|-------------------------------
|`KAFKA_BROKERCONNECT`  |Bootstrap list of Kafka host/port pairs. Defaults to `localhost:9092`.
|`KAFKA_PROPERTIES`     |Additional properties to configure the broker connection (base-64 encoded).
|`KAFKA_TRUSTSTORE`     |Certificate for broker authentication (base-64 encoded). Required for TLS/SSL.
|`KAFKA_KEYSTORE`       |Private key for mutual TLS authentication (base-64 encoded).
|`SERVER_SERVLET_CONTEXTPATH`|The context path to serve requests on (must end with a `/`). Defaults to `/`.
|`SERVER_PORT`          |The web server port to listen on. Defaults to `9000`.
|`SCHEMAREGISTRY_CONNECT `|The endpoint of Schema Registry for Avro or Protobuf message
|`SCHEMAREGISTRY_AUTH`  |Optional basic auth credentials in the form `username:password`.
|`CMD_ARGS`             |Command line arguments to Kafdrop, e.g. `--message.format` or `--protobufdesc.directory` or `--server.port`. 

##### Advanced configuration
| Name                     |Description
|--------------------------|-------------------------------
| `JVM_OPTS`               |JVM options.
| `JMX_PORT`               |Port to use for JMX. No default; if unspecified, JMX will not be exposed.
| `HOST`                   |The hostname to report for the RMI registry (used for JMX). Defaults to `localhost`.
| `KAFKA_PROPERTIES_FILE`  |Internal location where the Kafka properties file will be written to (if `KAFKA_PROPERTIES` is set). Defaults to `kafka.properties`.
| `KAFKA_TRUSTSTORE_FILE`  |Internal location where the truststore file will be written to (if `KAFKA_TRUSTSTORE` is set). Defaults to `kafka.truststore.jks`.
| `KAFKA_KEYSTORE_FILE`    |Internal location where the keystore file will be written to (if `KAFKA_KEYSTORE` is set). Defaults to `kafka.keystore.jks`.
| `SSL_ENABLED`            | Enabling HTTPS (SSL) for Kafdrop server. Default is `false`
| `SSL_KEY_STORE_TYPE`     | Type of SSL keystore. Default is `PKCS12`
| `SSL_KEY_STORE`          | Path to keystore file
| `SSL_KEY_STORE_PASSWORD` | Keystore password
| `SSL_KEY_ALIAS`          | Key alias

### Using Helm
Like in the Docker example, supply the files in base-64 form:

```sh
helm upgrade -i kafdrop chart --set image.tag=3.x.x \
    --set kafka.brokerConnect=<host:port,host:port> \
    --set kafka.properties="$(cat kafka.properties | base64)" \
    --set kafka.truststore="$(cat kafka.truststore.jks | base64)" \
    --set kafka.keystore="$(cat kafka.keystore.jks | base64)"
```

## Updating the Bootstrap theme
Edit the `.scss` files in the `theme` directory, then run `theme/install.sh`. This will overwrite `src/main/resources/static/css/bootstrap.min.css`. Then build as usual. (Requires `npm`.)

## Securing the Kafdrop UI
Kafdrop doesn't (yet) natively implement an authentication mechanism to restrict user access. Here's a quick workaround using NGINX using Basic Auth. The instructions below are for macOS and Homebrew.

### Requirements
* NGINX: install using `which nginx > /dev/null || brew install nginx`
* Apache HTTP utilities: `which htpasswd > /dev/null || brew install httpd`

### Setup
Set the admin password (you will be prompted):
```sh
htpasswd -c /usr/local/etc/nginx/.htpasswd admin
```

Add a logout page in `/usr/local/opt/nginx/html/401.html`:
```html
<!DOCTYPE html>
<p>Not authorized. <a href="<!--# echo var="scheme" -->://<!--# echo var="http_host" -->/">Login</a>.</p>
```

Use the following snippet for `/usr/local/etc/nginx/nginx.conf`:
```
worker_processes 4;
  
events {
  worker_connections 1024;
}

http {
  upstream kafdrop {
    server 127.0.0.1:9000;
    keepalive 64;
  }

  server {
    listen *:8080;
    server_name _;
    access_log /usr/local/var/log/nginx/nginx.access.log;
    error_log /usr/local/var/log/nginx/nginx.error.log;
    auth_basic "Restricted Area";
    auth_basic_user_file /usr/local/etc/nginx/.htpasswd;

    location / {
      proxy_pass http://kafdrop;
    }

    location /logout {
      return 401;
    }

    error_page 401 /errors/401.html;

    location /errors {
      auth_basic off;
      ssi        on;
      alias /usr/local/opt/nginx/html;
    }
  }
}
```

Run NGINX:
```sh
nginx
```

Or reload its configuration if already running:
```sh
nginx -s reload
```

To logout, browse to [/logout](http://localhost:8080/logout).

> **Hey there!** We hope you really like Kafdrop! Please take a moment to [⭐](https://github.com/obsidiandynamics/kafdrop)the repo or [Tweet](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fobsidiandynamics%2Fkafdrop&text=Get%20Kafdrop%20%E2%80%94%20a%20web-based%20UI%20for%20viewing%20%23ApacheKafka%20topics%20and%20browsing%20consumers%20) about it.

# Contributing Guidelines

All contributions are more than welcomed. Contributions may close an issue, fix a bug (reported or not reported), add new design blocks, improve the existing code, add new feature, and so on. In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone.

## Release workflow

To cut an official release, these are the steps:

1. Commit a new version on master that has the `-SNAPSHOT` suffix stripped (see `pom.xml`). Once the commit is merged, the CI will treat it as a release build, and will end up publishing more artifacts than the regular (non-release/snapshot) build. One of those will be a dockerhub push to the specific version and "latest" tags. (The regular build doesn't update "latest"). 

2. You can then edit the release description in GitHub to describe what went into the release.

3. After the release goes through successfully, you need to prepare the repo for the next version, which requires committing the next snapshot version on master again. So we should increment the minor version and add again the `-SNAPSHOT` suffix.
