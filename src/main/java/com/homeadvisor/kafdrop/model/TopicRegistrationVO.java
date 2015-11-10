package com.homeadvisor.kafdrop.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class TopicRegistrationVO
{
   private int version;
   private Map<Integer, List<Integer>> replicas;

   public Map<Integer, List<Integer>> getReplicas()
   {
      return replicas;
   }

   @JsonProperty("partitions")
   public void setReplicas(Map<Integer, List<Integer>> replicas)
   {
      this.replicas = replicas;
   }

   public int getVersion()
   {
      return version;
   }

   public void setVersion(int version)
   {
      this.version = version;
   }
}
