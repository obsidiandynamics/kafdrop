package kafdrop.util;

import java.nio.ByteBuffer;

public class IntMessageDeserializer implements MessageDeserializer {
  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    return ByteUtils.readInt(buffer);
  }


}
