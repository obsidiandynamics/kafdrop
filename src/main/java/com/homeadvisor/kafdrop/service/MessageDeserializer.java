package com.homeadvisor.kafdrop.service;

import java.nio.ByteBuffer;


public interface MessageDeserializer {

   public String deserializeMessage(ByteBuffer buffer);

}
