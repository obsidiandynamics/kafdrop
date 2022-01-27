package kafdrop.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

public class MsgPackMessageDeserializer implements MessageDeserializer {

  private static final Logger LOG = LoggerFactory.getLogger(MsgPackMessageDeserializer.class);

  @Override
  public String deserializeMessage(ByteBuffer buffer) {
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(buffer)) {
      return unpacker.unpackValue().toJson();
    } catch (IOException e) {
      final String errorMsg = "Unable to unpack msgpack message";
      LOG.error(errorMsg, e);
      throw new DeserializationException(errorMsg);
    }
  }
}
