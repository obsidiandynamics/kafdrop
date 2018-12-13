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

import com.homeadvisor.kafdrop.model.MessageVO;
import com.homeadvisor.kafdrop.model.TopicPartitionVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import com.homeadvisor.kafdrop.util.BrokerChannel;
import com.homeadvisor.kafdrop.util.ByteUtils;
import com.homeadvisor.kafdrop.util.MessageDeserializer;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;


@Service
public class MessageInspector
{
   private final Logger LOG = LoggerFactory.getLogger(getClass());

   @Autowired
   private KafkaMonitor kafkaMonitor;

   public List<MessageVO> getMessages(
           String topicName,
           int partitionId,
           long offset,
           long count,
           MessageDeserializer deserializer)
   {
      final TopicVO topic = kafkaMonitor.getTopic(topicName).orElseThrow(TopicNotFoundException::new);
      final TopicPartitionVO partition = topic.getPartition(partitionId).orElseThrow(PartitionNotFoundException::new);

      return kafkaMonitor.getBroker(partition.getLeader().getId())
         .map(broker -> {
            SimpleConsumer consumer = new SimpleConsumer(broker.getHost(), broker.getPort(), 10000, 100000, "");

            final FetchRequestBuilder fetchRequestBuilder = new FetchRequestBuilder()
               .clientId("KafDrop")
               .maxWait(5000) // todo: make configurable
               .minBytes(1);

            List<MessageVO> messages = new ArrayList<>();
            long currentOffset = offset;
            while (messages.size() < count)
            {
               final FetchRequest fetchRequest =
                  fetchRequestBuilder
                     .addFetch(topicName, partitionId, currentOffset, 1024 * 1024)
                     .build();

               FetchResponse fetchResponse = consumer.fetch(fetchRequest);

               final ByteBufferMessageSet messageSet = fetchResponse.messageSet(topicName, partitionId);
               if (messageSet.validBytes() <= 0) break;


               int oldSize = messages.size();
               StreamSupport.stream(messageSet.spliterator(), false)
                  .limit(count - messages.size())
                  .map(MessageAndOffset::message)
                  .map(m -> createMessage(m, deserializer))
                  .forEach(messages::add);
               currentOffset += messages.size() - oldSize;
            }
            return messages;
         })
         .orElseGet(Collections::emptyList);
   }

   private MessageVO createMessage(Message message, MessageDeserializer deserializer)
   {
      MessageVO vo = new MessageVO();
      if (message.hasKey())
      {
         vo.setKey(ByteUtils.readString(message.key()));
      }
      if (!message.isNull())
      {
         final String messageString = deserializer.deserializeMessage(message.payload());
         vo.setMessage(messageString);
      }

      vo.setValid(message.isValid());
      vo.setCompressionCodec(message.compressionCodec().name());
      vo.setChecksum(message.checksum());
      vo.setComputedChecksum(message.computeChecksum());

      return vo;
   }

}
