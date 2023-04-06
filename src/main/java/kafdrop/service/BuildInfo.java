package kafdrop.service;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnResource(resources = "${spring.info.build.location:classpath:META-INF/build-info.properties}")
public final class BuildInfo {
  private final BuildProperties buildProperties;

  public BuildInfo(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
    final var log = LoggerFactory.getLogger(BuildInfo.class);
    log.info("Kafdrop version: {}, build time: {}", buildProperties.getVersion(), buildProperties.getTime());
  }

  public BuildProperties getBuildProperties() {
    return buildProperties;
  }
}
