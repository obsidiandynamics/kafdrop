package kafdrop.util;

import java.nio.*;

public class DefaultMessageDeserializer implements MessageDeserializer {
  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    return ByteUtils.readString(buffer);
  }
}
