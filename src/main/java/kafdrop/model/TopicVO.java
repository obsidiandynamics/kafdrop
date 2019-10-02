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

import java.util.*;
import java.util.stream.*;

public final class TopicVO implements Comparable<TopicVO> {
  private final String name;

  private Map<Integer, TopicPartitionVO> partitions = new TreeMap<>();

  private Map<String, String> config = Collections.emptyMap();

  public TopicVO(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  public Map<Integer, TopicPartitionVO> getPartitionMap() {
    return Collections.unmodifiableMap(partitions);
  }

  public Collection<TopicPartitionVO> getPartitions() {
    return Collections.unmodifiableCollection(partitions.values());
  }

  public void setPartitions(Map<Integer, TopicPartitionVO> partitions) {
    this.partitions = partitions;
  }

  public Optional<TopicPartitionVO> getPartition(int partitionId) {
    return Optional.ofNullable(partitions.get(partitionId));
  }

  public Collection<TopicPartitionVO> getLeaderPartitions(int brokerId) {
    return partitions.values().stream()
        .filter(tp -> tp.getLeader() != null && tp.getLeader().getId() == brokerId)
        .collect(Collectors.toList());
  }

  public Collection<TopicPartitionVO> getUnderReplicatedPartitions() {
    return partitions.values().stream()
        .filter(TopicPartitionVO::isUnderReplicated)
        .collect(Collectors.toList());
  }

  /**
   * Returns the total number of messages published to the topic, ever
   * @return
   */
  public long getTotalSize() {
    return partitions.values().stream()
        .map(TopicPartitionVO::getSize)
        .reduce(0L, Long::sum);
  }

  /**
   * Returns the total number of messages available to consume from the topic.
   * @return
   */
  public long getAvailableSize() {
    return partitions.values().stream()
        .map(p -> p.getSize() - p.getFirstOffset())
        .reduce(0L, Long::sum);
  }

  public double getPreferredReplicaPercent() {
    if (partitions.isEmpty()) {
      return 0;
    } else {
      final var preferredLeaderCount = partitions.values().stream()
          .filter(TopicPartitionVO::isLeaderPreferred)
          .count();
      return ((double) preferredLeaderCount) / ((double) partitions.size());
    }
  }

  @Override
  public int compareTo(TopicVO that) {
    return this.name.compareTo(that.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof TopicVO) {
      final var that = (TopicVO) o;
      return Objects.equals(name, that.name);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public String toString() {
    return TopicVO.class.getSimpleName() + " [name=" + name +", partitions=" + partitions + "]";
  }
}
