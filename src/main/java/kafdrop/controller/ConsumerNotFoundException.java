package kafdrop.controller;

public final class ConsumerNotFoundException extends Exception {
  public ConsumerNotFoundException(String groupId) {
    super("No consumer for group " + groupId);
  }
}
