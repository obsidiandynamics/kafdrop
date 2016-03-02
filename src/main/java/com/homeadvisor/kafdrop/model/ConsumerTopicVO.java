package com.homeadvisor.kafdrop.model;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
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

   public long getMaxLag()
   {
      return offsets.values().stream()
         .map(ConsumerPartitionVO::getLag)
         .filter(lag -> lag >= 0)
         .reduce(0L, Long::max);
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

   public int getOwnerCount()
   {
      return (int)offsets.values().stream()
         .map(ConsumerPartitionVO::getOwner)
         .filter(Objects::nonNull)
         .distinct()
         .count();
   }
}
