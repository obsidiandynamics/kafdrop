/*
 * Copyright 2017 Kafdrop contributors.
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

package kafdrop.service;

import kafdrop.model.*;
import kafdrop.util.*;
import org.apache.kafka.common.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
public final class MessageInspector {
  private final KafkaMonitor kafkaMonitor;

  public MessageInspector(KafkaMonitor kafkaMonitor) {
    this.kafkaMonitor = kafkaMonitor;
  }

  /**
   * Gets messages for a given partition.
   */
  public List<MessageVO> getMessages(String topicName, int partitionId, long offset, int count,
                                     Deserializers deserializers) {
    final var topicPartition = new TopicPartition(topicName, partitionId);
    return kafkaMonitor.getMessages(topicPartition, offset, count, deserializers);
  }

  /**
   * Gets all messages from all partitions of a given topic.
   */
  public List<MessageVO> getMessages(String topicName, int count,
                                     Deserializers deserializers) {
    return kafkaMonitor.getMessages(topicName, count, deserializers);
  }
}
