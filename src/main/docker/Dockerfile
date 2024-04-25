FROM eclipse-temurin:17.0.11_9-jdk

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
