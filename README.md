<img src="https://raw.githubusercontent.com/wiki/obsidiandynamics/kafdrop/images/kafdrop-logo.png" width="90px" alt="logo"/> Kafdrop 3 – Web UI for Kafka
===
[![Download](https://api.bintray.com/packages/obsidiandynamics/kafdrop/main/images/download.svg)](https://bintray.com/obsidiandynamics/kafdrop/main/_latestVersion)
[![Build](https://travis-ci.org/obsidiandynamics/kafdrop.svg?branch=master)](https://travis-ci.org/obsidiandynamics/kafdrop#)
[![Docker](https://img.shields.io/docker/pulls/obsidiandynamics/kafdrop.svg)](https://hub.docker.com/r/obsidiandynamics/kafdrop)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/obsidiandynamics/kafdrop.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/obsidiandynamics/kafdrop/context:java)


Kafdrop 3 is a web UI for browsing Kafka topics and consumer groups. The tool displays information such as brokers, topics, partitions, consumers and lets you view messages. 

![Overview Screenshot](docs/images/overview.png?raw=true)

This project is a reboot of [Kafdrop 2.x](https://github.com/HomeAdvisor/Kafdrop), dragged kicking and screaming into the world of JDK 11+, Kafka 2.x, Helm and Kubernetes. It's a lightweight application that runs on Spring Boot and is dead-easy to configure, supporting SASL and TLS-secured brokers.

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
    -e KAFKA_BROKERCONNECT=<host:port,host:port> \
    -e JVM_OPTS="-Xms32M -Xmx64M" \
    -e SERVER_SERVLET_CONTEXTPATH="/" \
    obsidiandynamics/kafdrop
```

Then access the web UI at [http://localhost:9000](http://localhost:9000).

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
    --set jvm.opts="-Xms32M -Xmx64M"
```

For all Helm configuration options, have a peek into [chart/values.yaml](chart/values.yaml).

Replace `3.x.x` with the image tag of [obsidiandynamics/kafdrop](https://hub.docker.com/r/obsidiandynamics/kafdrop). Services will be bound on port 9000 by default (node port 30900).

**Note:** The context path _must_ end with a slash.

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
    -e KAFKA_PROPERTIES=$(cat kafka.properties | base64) \
    -e KAFKA_TRUSTSTORE=$(cat kafka.truststore.jks | base64) \   # optional
    -e KAFKA_KEYSTORE=$(cat kafka.keystore.jks | base64) \       # optional
    obsidiandynamics/kafdrop
```
#### Environment Variables

|Name |Comment|
|----------------------|-------------------------------|
|KAFKA_PROPERTIES_FILE |Localtion where kafka.properties file will be written (if KAFKA_PROPERTIES is set), and read by the application|
|KAFKA_TRUSTSTORE_FILE|Location where the kafka.truststore.jks file will be written (if KAFKA_TRUSTSTORE is set) and read by the application |
|KAFKA_KEYSTORE_FILE|Location where the kafka.keystore.jks file will be written(if KAFKA_KEYSTORE is set) and read by the application  |
|KAFKA_BROKERCONNECT|The broker you wan to connect to. Optionnal if set in 'kafka.properties' or if you connect to 'localhost'|
|KAFKA_PROPERTIES|base64 encoded string of your properties file. Include the trustore/keystore password, locations, connection encryption, broker address etc. |
|KAFKA_TRUSTSTORE|base64 encoded string of your truststore file.|
|KAFKA_KEYSTORE|base64 encoded string of your keystore file.|
|HEAP_ARGS|Override Heap settings : Xms, Xmx|

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
