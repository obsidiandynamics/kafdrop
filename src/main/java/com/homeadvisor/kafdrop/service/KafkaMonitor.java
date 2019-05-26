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
import org.apache.kafka.common.*;

import java.util.*;

public interface KafkaMonitor {

  List<BrokerVO> getBrokers();

  Optional<BrokerVO> getBroker(int id);

  Version getKafkaVersion();

  List<TopicVO> getTopics();

  List<MessageVO> getMessages(TopicPartition topicPartition, long offset, long count);

  Optional<TopicVO> getTopic(String topic);

  ClusterSummaryVO getClusterSummary();

  ClusterSummaryVO getClusterSummary(Collection<TopicVO> topics);

  List<ConsumerVO> getConsumers();

  List<ConsumerVO> getConsumers(TopicVO topic);

  List<ConsumerVO> getConsumers(String topic);

  Optional<ConsumerVO> getConsumer(String groupId);

  Optional<ConsumerVO> getConsumerByTopicName(String groupId, String topic);

  Optional<ConsumerVO> getConsumerByTopic(String groupId, TopicVO topic);
}
