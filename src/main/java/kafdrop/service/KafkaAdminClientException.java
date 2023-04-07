package kafdrop.service;

public final class KafkaAdminClientException extends RuntimeException {
  KafkaAdminClientException(Throwable cause) {
    super(cause);
  }
}
