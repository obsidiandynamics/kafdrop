package kafdrop.util;

public class DefaultMessageSerializer implements MessageSerializer {

  @Override
  public byte[] serializeMessage(String value) {
    return value.getBytes();
  }

}
