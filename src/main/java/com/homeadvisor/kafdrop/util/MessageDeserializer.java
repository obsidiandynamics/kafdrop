package com.homeadvisor.kafdrop.util;

import java.nio.*;


public interface MessageDeserializer {

  String deserializeMessage(ByteBuffer buffer);

}
