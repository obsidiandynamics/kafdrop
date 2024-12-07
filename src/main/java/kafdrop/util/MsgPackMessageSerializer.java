package kafdrop.util;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgPackMessageSerializer implements MessageSerializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MsgPackMessageSerializer.class);

  @Override
  public byte[] serializeMessage(String value) {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packString(value);
      return packer.toByteArray();
    } catch (IOException e) {
      final String errorMsg = "Unable to pack msgpack message";
      LOGGER.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    }
  }

}
