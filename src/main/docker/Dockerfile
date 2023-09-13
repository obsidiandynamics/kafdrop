FROM eclipse-temurin:19.0.2_7-jdk

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
