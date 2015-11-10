package com.homeadvisor.kafdrop.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class ConsumerTopicVO
{
   private final String topic;
   private final Map<Integer, ConsumerPartitionVO> offsets = new TreeMap<>();

   public ConsumerTopicVO(String topic)
   {
      this.topic = topic;
   }

   public String getTopic()
   {
      return topic;
   }

   public void addOffset(ConsumerPartitionVO offset)
   {
      offsets.put(offset.getPartitionId(), offset);
   }

   public long getLag()
   {
      return offsets.values().stream()
         .map(ConsumerPartitionVO::getLag)
         .filter(lag -> lag >= 0)
         .reduce(0L, Long::sum);
   }

   public Collection<ConsumerPartitionVO> getPartitions()
   {
      return offsets.values();
   }

   public double getCoveragePercent()
   {
      return ((double)getAssignedPartitionCount()) / offsets.size();
   }

   public int getAssignedPartitionCount()
   {
      return (int) offsets.values().stream()
         .filter(p -> p.getOwner() != null)
         .count();
   }
}
