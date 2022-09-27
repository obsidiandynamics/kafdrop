FROM eclipse-temurin:18.0.2_9-jdk

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]
