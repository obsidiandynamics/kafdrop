package com.homeadvisor.kafdrop.model;

import java.util.*;
import java.util.stream.Collectors;

public class TopicPartitionVO
{
   private int id;
   private Map<Integer, PartitionReplica> replicas = new LinkedHashMap<>();
   private Integer leaderId;
   private Integer preferredLeaderId;
   private long size = -1;
   private long firstOffset = -1;

   public TopicPartitionVO(int id)
   {
      this.id = id;
   }

   public int getId()
   {
      return id;
   }

   public void setId(int id)
   {
      this.id = id;
   }

   public Collection<PartitionReplica> getReplicas()
   {
      return replicas.values();
   }

   public void addReplica(PartitionReplica replica)
   {
      replicas.put(replica.getId(), replica);
      if (replica.isLeader())
      {
         leaderId = replica.getId();
      }
      if (preferredLeaderId == null)
      {
         preferredLeaderId = replica.getId();
      }
   }

   public PartitionReplica getLeader()
   {
      return replicas.get(leaderId);
   }
   public PartitionReplica getPreferredLeader()
   {
      return replicas.get(preferredLeaderId);
   }

   public boolean isLeaderPreferred()
   {
      return leaderId == preferredLeaderId;
   }

   public List<PartitionReplica> getInSyncReplicas()
   {
      return replicas.values().stream()
         .filter(PartitionReplica::isInService)
         .sorted(Comparator.comparingInt(PartitionReplica::getId))
         .collect(Collectors.toList());
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

   public static class PartitionReplica
   {
      private Integer id;
      private boolean inService;
      private boolean leader;

      public Integer getId()
      {
         return id;
      }

      public void setId(Integer id)
      {
         this.id = id;
      }

      public boolean isInService()
      {
         return inService;
      }

      public void setInService(boolean inService)
      {
         this.inService = inService;
      }

      public boolean isLeader()
      {
         return leader;
      }

      public void setLeader(boolean leader)
      {
         this.leader = leader;
      }
   }
}
