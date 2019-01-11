package com.homeadvisor.kafdrop.util;

import java.nio.ByteBuffer;


public interface MessageDeserializer {

   public String deserializeMessage(ByteBuffer buffer);

}
