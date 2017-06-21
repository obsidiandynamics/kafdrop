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

package com.homeadvisor.kafdrop.util;

import com.homeadvisor.kafdrop.model.BrokerVO;
import kafka.network.BlockingChannel;

import java.util.function.Supplier;

public final class BrokerChannel
{
   final BlockingChannel channel;

   private BrokerChannel(BlockingChannel channel)
   {
      this.channel = channel;
   }

   public static BrokerChannel forBroker(String host, int port)
   {
      BlockingChannel channel = new BlockingChannel(host, port,
                                                    BlockingChannel.UseDefaultBufferSize(),
                                                    BlockingChannel.UseDefaultBufferSize(),
                                                    5000); // todo: make this configurable
      return new BrokerChannel(channel);
   }

   public <T> T execute(Callback<T> callback)
   {
      try
      {
         if (!channel.isConnected())
         {
            channel.connect();
         }
         return callback.doWithChannel(channel);
      }
      finally
      {
         if (channel.isConnected())
         {
            channel.disconnect();
         }
      }
   }

   @FunctionalInterface
   public interface Callback<T>
   {
      T doWithChannel(BlockingChannel channel);
   }
}
