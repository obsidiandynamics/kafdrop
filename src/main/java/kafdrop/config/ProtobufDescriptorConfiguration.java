package kafdrop.config;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class ProtobufDescriptorConfiguration {
  @Component
  @ConfigurationProperties(prefix = "protobufdesc")
  public static final class ProtobufDescriptorProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ProtobufDescriptorProperties.class);
    // the idea is to let user specifying a directory stored all descriptor file
    // the program will load and .desc file and show as an option on the message
    // detail screen
    private String directory;

    private Boolean parseAnyProto = Boolean.FALSE;

    public String getDirectory() {
      return directory;
    }

    public void setDirectory(String directory) {
      this.directory = directory;
    }

    public Boolean getParseAnyProto() {
      return parseAnyProto;
    }

    public void setParseAnyProto(Boolean parseAnyProto) {
      this.parseAnyProto = parseAnyProto;
    }

    public List<String> getDescFilesList() {
      // getting file list
      if (directory == null || Files.notExists(Path.of(directory))) {
        LOG.info("No descriptor folder configured, skip the setting!!");
        return Collections.emptyList();
      }
      String[] pathnames;
      File path = new File(directory);

      // apply filter for listing only .desc file
      FilenameFilter filter = (dir, name) -> name.endsWith(".desc");

      pathnames = path.list(filter);
      return Arrays.asList(Objects.requireNonNull(pathnames));
    }
  }
}
