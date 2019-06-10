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

import java.util.*;

public interface KafkaMonitor {
  List<BrokerVO> getBrokers();

  Optional<BrokerVO> getBroker(int id);

  List<TopicVO> getTopics();

  List<MessageVO> getMessages(TopicPartition topicPartition, long offset, long count,
                              MessageDeserializer deserializer);

  Optional<TopicVO> getTopic(String topic);

  ClusterSummaryVO getClusterSummary(Collection<TopicVO> topics);
}
