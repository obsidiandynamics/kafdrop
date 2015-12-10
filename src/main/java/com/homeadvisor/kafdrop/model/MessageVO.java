package com.homeadvisor.kafdrop.model;

public class MessageVO
{
   private String message;
   private String key;
   private boolean valid;
   private long checksum;
   private long computedChecksum;
   private String compressionCodec;

   public boolean isValid()
   {
      return valid;
   }

   public void setValid(boolean valid)
   {
      this.valid = valid;
   }

   public String getMessage()
   {
      return message;
   }

   public void setMessage(String message)
   {
      this.message = message;
   }

   public String getKey()
   {
      return key;
   }

   public void setKey(String key)
   {
      this.key = key;
   }

   public long getChecksum()
   {
      return checksum;
   }

   public void setChecksum(long checksum)
   {
      this.checksum = checksum;
   }

   public long getComputedChecksum()
   {
      return computedChecksum;
   }

   public void setComputedChecksum(long computedChecksum)
   {
      this.computedChecksum = computedChecksum;
   }

   public String getCompressionCodec()
   {
      return compressionCodec;
   }

   public void setCompressionCodec(String compressionCodec)
   {
      this.compressionCodec = compressionCodec;
   }
}
