package com.homeadvisor.kafdrop.model;

import java.util.List;

public class TopicPartitionStateVO
{
   private int version;
   private List<Integer> isr;
   private int leader;

   public int getVersion()
   {
      return version;
   }

   public void setVersion(int version)
   {
      this.version = version;
   }

   public List<Integer> getIsr()
   {
      return isr;
   }

   public void setIsr(List<Integer> isr)
   {
      this.isr = isr;
   }

   public int getLeader()
   {
      return leader;
   }

   public void setLeader(int leader)
   {
      this.leader = leader;
   }
}
