FROM openjdk:14-slim

ADD kafdrop.sh /
ADD kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]
