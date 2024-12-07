package kafdrop.util;

@FunctionalInterface
public interface MessageSerializer {
  byte[] serializeMessage(String value);
}
