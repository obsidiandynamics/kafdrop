package kafdrop.util;

import java.nio.*;
import java.nio.charset.*;

final class ByteUtils {
  private ByteUtils() {
    // no instance allowed, static utility class
  }
  static String readString(ByteBuffer buffer) {
    return new String(readBytes(buffer), StandardCharsets.UTF_8);
  }

  private static byte[] readBytes(ByteBuffer buffer) {
    return readBytes(buffer, buffer.limit());
  }

  private static byte[] readBytes(ByteBuffer buffer, int size) {
    final var dest = new byte[size];
    if (buffer.hasArray()) {
      System.arraycopy(buffer.array(), buffer.arrayOffset(), dest, 0, size);
    } else {
      buffer.mark();
      buffer.get(dest);
      buffer.reset();
    }
    return dest;
  }

  static byte[] convertToByteArray(ByteBuffer buffer) {
    final var bytes = new byte[buffer.remaining()];
    buffer.get(bytes, 0, bytes.length);
    return bytes;
  }
}
