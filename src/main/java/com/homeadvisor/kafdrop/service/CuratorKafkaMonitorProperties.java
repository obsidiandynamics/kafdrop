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

package com.homeadvisor.kafdrop.service;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "kafdrop.monitor")
public class CuratorKafkaMonitorProperties
{
   private String clientId = "Kafdrop";

   @Min(1)
   private int threadPoolSize = 10;

   private RetryProperties retry = new RetryProperties();

   @NotBlank
   private String kafkaVersion = "0.8.2.2";

   public String getKafkaVersion()
   {
      return kafkaVersion.toString();
   }

   public void setKafkaVersion(String kafkaVersion)
   {
      this.kafkaVersion = kafkaVersion;
   }

   public String getClientId()
   {
      return clientId;
   }

   public void setClientId(String clientId)
   {
      this.clientId = clientId;
   }

   public int getThreadPoolSize()
   {
      return threadPoolSize;
   }

   public void setThreadPoolSize(int threadPoolSize)
   {
      this.threadPoolSize = threadPoolSize;
   }

   public RetryProperties getRetry()
   {
      return retry;
   }

   public void setRetry(RetryProperties retry)
   {
      this.retry = retry;
   }

   public static class RetryProperties
   {
      @Min(1)
      private int maxAttempts = 3;

      @Min(0)
      private long backoffMillis = 1000;

      public int getMaxAttempts()
      {
         return maxAttempts;
      }

      public void setMaxAttempts(int maxAttempts)
      {
         this.maxAttempts = maxAttempts;
      }

      public long getBackoffMillis()
      {
         return backoffMillis;
      }

      public void setBackoffMillis(long backoffMillis)
      {
         this.backoffMillis = backoffMillis;
      }
   }
}
