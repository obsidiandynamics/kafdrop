package com.homeadvisor.kafdrop.model;

public class ConsumerPartitionVO
{
   private final String groupId;
   private final String topic;
   private final int partitionId;
   private long offset;
   private long size;
   private long firstOffset;
   private String owner;

   public ConsumerPartitionVO(String groupId, String topic, int partitionId)
   {
      this.groupId = groupId;
      this.topic = topic;
      this.partitionId = partitionId;
   }

   public String getGroupId()
   {
      return groupId;
   }

   public String getTopic()
   {
      return topic;
   }

   public int getPartitionId()
   {
      return partitionId;
   }

   public void setOffset(long offset)
   {
      this.offset = offset;
   }

   public long getSize()
   {
      return size;
   }

   public void setSize(long size)
   {
      this.size = size;
   }

   public long getFirstOffset()
   {
      return firstOffset;
   }

   public void setFirstOffset(long firstOffset)
   {
      this.firstOffset = firstOffset;
   }

   public long getLag()
   {
      if (size < 0 || firstOffset < 0)
      {
         return 0;
      }
      else if (offset < firstOffset)
      {
         return size - firstOffset;
      }
      else
      {
         return size - offset;
      }
   }

   public void setOwner(String owner)
   {
      this.owner = owner;
   }

   public long getOffset()
   {
      return offset;
   }

   public String getOwner()
   {
      return owner;
   }
}
