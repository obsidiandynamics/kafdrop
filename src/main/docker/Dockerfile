FROM eclipse-temurin:25.0.4_7-jdk

ADD kafdrop.sh /
ADD maven/ /kafdrop/

RUN chmod +x /kafdrop.sh

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
