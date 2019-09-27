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

package kafdrop.model;

import org.apache.commons.lang3.*;

import java.util.*;

public final class ConsumerVO implements Comparable<ConsumerVO> {
  private final String groupId;
  private final Map<String, ConsumerTopicVO> topics = new TreeMap<>();

  public ConsumerVO(String groupId) {
    Validate.notEmpty("groupId is required");
    this.groupId = groupId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void addTopic(ConsumerTopicVO topic) {
    topics.put(topic.getTopic(), topic);
  }

  public ConsumerTopicVO getTopic(String topic) {
    return topics.get(topic);
  }

  public Collection<ConsumerTopicVO> getTopics() {
    return topics.values();
  }

  @Override
  public int compareTo(ConsumerVO that) {
    return this.groupId.compareTo(that.groupId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ConsumerVO) {
      final var that = (ConsumerVO) o;
      return Objects.equals(groupId, that.groupId);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(groupId);
  }
}
