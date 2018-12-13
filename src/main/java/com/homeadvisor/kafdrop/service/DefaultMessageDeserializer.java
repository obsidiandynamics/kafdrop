package com.homeadvisor.kafdrop.service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.homeadvisor.kafdrop.util.ByteUtils;

public class DefaultMessageDeserializer implements MessageDeserializer {

   @Override
   public String deserializeMessage(ByteBuffer buffer) {
      return ByteUtils.readString(buffer);
   }

}
