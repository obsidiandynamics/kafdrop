package com.homeadvisor.kafdrop.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DefaultMessageDeserializer implements MessageDeserializer {

   @Override
   public String deserializeMessage(ByteBuffer buffer) {
      return ByteUtils.readString(buffer);
   }

}
