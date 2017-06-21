/*
 * Copyright 2017 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.homeadvisor.kafdrop.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
      return inSyncReplicaStream()
         .sorted(Comparator.comparingInt(PartitionReplica::getId))
         .collect(Collectors.toList());
   }

   private Stream<PartitionReplica> inSyncReplicaStream()
   {
      return replicas.values().stream()
         .filter(PartitionReplica::isInService);
   }

   public boolean isUnderReplicated()
   {
      return inSyncReplicaStream().count() < replicas.size();
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

      public PartitionReplica()
      {
      }

      public PartitionReplica(Integer id, boolean inService, boolean leader)
      {
         this.id = id;
         this.inService = inService;
         this.leader = leader;
      }

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
