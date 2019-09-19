package kafdrop.service;

final class KafkaAdminClientException extends RuntimeException {
  KafkaAdminClientException(Throwable cause) {
    super(cause);
  }
}
