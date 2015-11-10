package com.homeadvisor.kafdrop.model;

public class ConsumerPartitionVO
{
   private final String groupId;
   private final String topic;
   private final int partitionId;
   private long offset;
   private long size;
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

   public long getLag()
   {
      return size >= 0 && offset >= 0 ? size - offset : -1;
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
