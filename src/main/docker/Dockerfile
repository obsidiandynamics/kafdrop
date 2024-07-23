FROM eclipse-temurin:17.0.12_7-jdk

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
