package com.homeadvisor.kafdrop.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class ByteUtils {

   public static String readString(ByteBuffer buffer)
   {
      try
      {
         return new String(readBytes(buffer), "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         return "<unsupported encoding>";
      }
   }

   private static byte[] readBytes(ByteBuffer buffer)
   {
      return readBytes(buffer, 0, buffer.limit());
   }

   private static byte[] readBytes(ByteBuffer buffer, int offset, int size)
   {
      byte[] dest = new byte[size];
      if (buffer.hasArray())
      {
         System.arraycopy(buffer.array(), buffer.arrayOffset() + offset, dest, 0, size);
      }
      else
      {
         buffer.mark();
         buffer.get(dest);
         buffer.reset();
      }
      return dest;
   }

   public static byte[] convertToByteArray(ByteBuffer buffer)
   {
     byte[] bytes = new byte[buffer.remaining()];
     buffer.get(bytes, 0, bytes.length);
     return bytes;
   }

}
