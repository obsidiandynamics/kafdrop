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

import com.homeadvisor.kafdrop.model.*;
import com.homeadvisor.kafdrop.util.*;
import kafka.api.FetchRequest;
import kafka.api.*;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.*;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.*;
import org.apache.kafka.common.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

@Service
public class MessageInspector {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Autowired
  private KafkaMonitor kafkaMonitor;

  public List<MessageVO> getMessages(String topicName, int partitionId, long offset, long count) {
    if (kafkaMonitor.getKafkaVersion().compareTo(new Version(0, 8, 2)) > 0) {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
          .orElseThrow(TopicNotFoundException::new);
      final TopicPartitionVO partition = topic.getPartition(partitionId)
          .orElseThrow(PartitionNotFoundException::new);

      TopicPartition topicPartition = new TopicPartition(topicName, partitionId);
      return kafkaMonitor.getMessages(topicPartition, offset, count);
    } else {
      final TopicVO topic = kafkaMonitor.getTopic(topicName)
          .orElseThrow(TopicNotFoundException::new);
      final TopicPartitionVO partition = topic.getPartition(partitionId)
          .orElseThrow(PartitionNotFoundException::new);

      return kafkaMonitor.getBroker(partition.getLeader().getId())
          .map(broker -> {
            SimpleConsumer consumer = new SimpleConsumer(broker.getHost(), broker.getPort(), 10000,
                                                         100000, "");

            final FetchRequestBuilder fetchRequestBuilder = new FetchRequestBuilder()
                .clientId("KafDrop")
                .maxWait(5000) // todo: make configurable
                .minBytes(1);

            List<MessageVO> messages = new ArrayList<>();
            long currentOffset = offset;
            while (messages.size() < count) {
              final FetchRequest fetchRequest =
                  fetchRequestBuilder
                      .addFetch(topicName, partitionId, currentOffset, 1024 * 1024)
                      .build();

              FetchResponse fetchResponse = consumer.fetch(fetchRequest);

              final ByteBufferMessageSet messageSet = fetchResponse
                  .messageSet(topicName, partitionId);
              if (messageSet.validBytes() <= 0) {
                break;
              }

              int oldSize = messages.size();
              StreamSupport.stream(messageSet.spliterator(), false)
                  .limit(count - messages.size())
                  .map(MessageAndOffset::message)
                  .map(this::createMessage)
                  .forEach(messages::add);
              currentOffset += messages.size() - oldSize;
            }
            return messages;
          })
          .orElseGet(Collections::emptyList);
    }
  }

  private MessageVO createMessage(Message message) {
    MessageVO vo = new MessageVO();
    if (message.hasKey()) {
      vo.setKey(readString(message.key()));
    }
    if (!message.isNull()) {
      vo.setMessage(readString(message.payload()));
    }

    return vo;
  }

  private String readString(ByteBuffer buffer) {
    return new String(readBytes(buffer), StandardCharsets.UTF_8);
  }

  private byte[] readBytes(ByteBuffer buffer) {
    return readBytes(buffer, 0, buffer.limit());
  }

  private byte[] readBytes(ByteBuffer buffer, int offset, int size) {
    return ByteUtils.readBytes(buffer, offset, size);
  }
}
