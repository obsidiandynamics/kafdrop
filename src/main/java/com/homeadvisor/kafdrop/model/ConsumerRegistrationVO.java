package com.homeadvisor.kafdrop.model;

import java.util.Map;

public class ConsumerRegistrationVO
{
   private String id;
   private Map<String, Integer> subscriptions;

   public ConsumerRegistrationVO(String id)
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
   }

   public Map<String, Integer> getSubscriptions()
   {
      return subscriptions;
   }

   public void setSubscriptions(Map<String, Integer> subscriptions)
   {
      this.subscriptions = subscriptions;
   }
}
