FROM eclipse-temurin:11.0.17_8-jdk

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]
