package kafdrop.util;

public class Serializers {

  private final MessageSerializer keySerializer;
  private final MessageSerializer valueSerializer;

  public Serializers(MessageSerializer keySerializer, MessageSerializer valueSerializer) {
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
  }

  public MessageSerializer getKeySerializer() {
    return keySerializer;
  }

  public MessageSerializer getValueSerializer() {
    return valueSerializer;
  }

}
