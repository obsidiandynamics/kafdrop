FROM ${docker.base.image}

ADD kafdrop.sh /
ADD ${project.build.finalName} /${project.build.finalName}

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]
