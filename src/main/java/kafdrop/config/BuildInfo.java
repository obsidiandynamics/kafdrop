package kafdrop.config;

import org.slf4j.*;
import org.springframework.boot.info.*;
import org.springframework.stereotype.*;

@Component
public final class BuildInfo {
  public BuildInfo(BuildProperties buildProperties) {
    final var log = LoggerFactory.getLogger(BuildInfo.class);
    log.info("Kafdrop version: {}, build time: {}", buildProperties.getVersion(), buildProperties.getTime());
  }
}
