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
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.Message;
import kafka.message.MessageAndOffset;

import org.apache.avro.generic.GenericRecord;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
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
           @Nullable String schemaRegistryUrl)
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
                  .map(m -> createMessage(m, topicName, schemaRegistryUrl))
                  .forEach(messages::add);
               currentOffset += messages.size() - oldSize;
            }
            return messages;
         })
         .orElseGet(Collections::emptyList);
   }

   private MessageVO createMessage(Message message, String topicName, @Nullable String schemaRegistryUrl)
   {
      MessageVO vo = new MessageVO();
      if (message.hasKey())
      {
         vo.setKey(readString(message.key()));
      }
      if (!message.isNull())
      {
         final String messageString;
         if (schemaRegistryUrl == null) {
            messageString = readString(message.payload());
         } else {
            messageString = deserializeMessage(message.payload(), topicName, schemaRegistryUrl);
         }
         vo.setMessage(messageString);
      }

      vo.setValid(message.isValid());
      vo.setCompressionCodec(message.compressionCodec().name());
      vo.setChecksum(message.checksum());
      vo.setComputedChecksum(message.computeChecksum());

      return vo;
   }

   // https://www.programcreek.com/java-api-examples/index.php?api=io.confluent.kafka.serializers.KafkaAvroDeserializer
   private String deserializeMessage(ByteBuffer buffer, String topicName, String schemaRegistryUrl)
   {
	  KafkaAvroDeserializer deserializer = getDeserializer(schemaRegistryUrl);

	  // Convert byte buffer to byte array
	  byte[] bytes = convertToBytes(buffer);

	  // TODO: use an actual JSON formatter to render results:
	  String result = deserializer
			  .deserialize(topicName, bytes)
			  .toString()
			  .replaceAll(",", ",\n");

	  return result;
   }

   private KafkaAvroDeserializer getDeserializer(String schemaRegistryUrl) {
	  Map<String, Object> config = new HashMap<>();
	  // TODO: parameterize schema registry URL
	  config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
              //schemaRegistryUrl"http://localhost:8081"); //<----- Run Schema Registry on 8081

	  KafkaAvroDeserializer kafkaAvroDeserializer = new KafkaAvroDeserializer();
	  kafkaAvroDeserializer.configure(config, false);
	  return kafkaAvroDeserializer;
   }

   private byte[] convertToBytes(ByteBuffer buffer)
   {
	  byte[] bytes = new byte[buffer.remaining()];
	  buffer.get(bytes, 0, bytes.length);
	  return bytes;
   }

   private String readString(ByteBuffer buffer)
   {
      try
      {
         return new String(readBytes(buffer), "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         return "<unsupported encoding>";
      }
   }

   private byte[] readBytes(ByteBuffer buffer)
   {
      return readBytes(buffer, 0, buffer.limit());
   }

   private byte[] readBytes(ByteBuffer buffer, int offset, int size)
   {
      byte[] dest = new byte[size];
      if (buffer.hasArray())
      {
         System.arraycopy(buffer.array(), buffer.arrayOffset() + offset, dest, 0, size);
      }
      else
      {
         buffer.mark();
         buffer.get(dest);
         buffer.reset();
      }
      return dest;
   }

}
