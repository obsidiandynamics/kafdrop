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

import kafdrop.model.MessageVO;
import kafdrop.util.Deserializers;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.Topic;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public final class MessageSearcher {
  private final KafkaMonitor kafkaMonitor;

  /**
   * Searches messages from all partitions of a given topic.
   */
  public List<MessageVO> search(final String topicName, final String searchText, final Deserializers deserializers) {
    return kafkaMonitor.searchMessages(topicName, searchText, deserializers);
  }
}
