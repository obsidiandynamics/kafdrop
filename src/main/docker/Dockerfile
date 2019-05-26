FROM openjdk:12.0.1-jdk-oraclelinux7
MAINTAINER homeadvisor

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]
