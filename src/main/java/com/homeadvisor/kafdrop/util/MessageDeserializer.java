package com.homeadvisor.kafdrop.util;

import java.nio.ByteBuffer;


public interface MessageDeserializer {

   String deserializeMessage(ByteBuffer buffer);

}
