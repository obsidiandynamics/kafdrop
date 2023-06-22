FROM eclipse-temurin:17.0.7_7-jdk

COPY kafdrop.sh /
COPY kafdrop*tar.gz /

RUN chmod +x /kafdrop.sh && \
    groupadd -g 1000 kafdrop && \
    useradd -u 1000 -g kafdrop kafdrop

USER kafdrop

ENV PATH="/opt/java/openjdk/bin:$PATH"

EXPOSE 9000

ENTRYPOINT ["/kafdrop.sh"]
