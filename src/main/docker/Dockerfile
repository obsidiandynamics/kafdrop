FROM eclipse-temurin:25.0.4.1_1-jdk

ADD kafdrop.sh /
ADD maven/ /kafdrop/

RUN chmod +x /kafdrop.sh

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
