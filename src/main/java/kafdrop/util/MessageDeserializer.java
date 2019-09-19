package kafdrop.util;

import java.nio.*;

@FunctionalInterface
public interface MessageDeserializer {
  String deserializeMessage(ByteBuffer buffer);
}
