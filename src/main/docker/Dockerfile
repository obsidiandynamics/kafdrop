FROM eclipse-temurin:17.0.7_7-jdk

COPY kafdrop*.tar.gz /tmp/kafdrop.tar.gz

RUN mkdir /opt/kafdrop && \
    tar \
      -xz \
      --strip-components=1 \
      --no-same-owner \
      -C /opt/kafdrop \
      -f /tmp/kafdrop.tar.gz && \
    rm /tmp/kafdrop.tar.gz && \
    groupadd -g 1000 kafdrop && \
    useradd -u 1000 -g kafdrop kafdrop && \
    chown kafdrop:kafdrop /opt/kafdrop

COPY --chmod=755 kafdrop.sh /opt/kafdrop

USER kafdrop

ENV PATH="/opt/java/openjdk/bin:$PATH"

EXPOSE 9000

WORKDIR /opt/kafdrop

ENTRYPOINT ["/opt/kafdrop/kafdrop.sh"]
