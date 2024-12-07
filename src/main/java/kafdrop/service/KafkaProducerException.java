package kafdrop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KafkaProducerException extends RuntimeException {

  public KafkaProducerException(Throwable exception) {
    super(exception);
  }

  public KafkaProducerException(String message) {
    super(message);
  }
}
