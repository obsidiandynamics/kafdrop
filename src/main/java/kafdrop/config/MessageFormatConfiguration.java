package kafdrop.config;

import kafdrop.util.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import javax.annotation.*;


@Configuration
public class MessageFormatConfiguration {
  @Component
  @ConfigurationProperties(prefix = "message")
  public static final class MessageFormatProperties {
    private MessageFormat format;
    private MessageFormat keyFormat;

    @PostConstruct
    public void init() {
      // Set a default message format if not configured.
      if (format == null) {
        format = MessageFormat.DEFAULT;
      }
      if (keyFormat == null) {
        keyFormat = format; //fallback
      }
    }

    public MessageFormat getFormat() {
      return format;
    }

    public void setFormat(MessageFormat format) {
      this.format = format;
    }

    public MessageFormat getKeyFormat() {
      return keyFormat;
    }

    public void setKeyFormat(MessageFormat keyFormat) {
      this.keyFormat = keyFormat;
    }
  }
}
