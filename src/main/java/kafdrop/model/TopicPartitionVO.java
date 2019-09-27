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

public final class TopicPartitionVO {
  private final int id;
  private final Map<Integer, PartitionReplica> replicas = new LinkedHashMap<>();
  private Integer leaderId;
  private Integer preferredLeaderId;
  private long size = -1;
  private long firstOffset = -1;

  public TopicPartitionVO(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public Collection<PartitionReplica> getReplicas() {
    return replicas.values();
  }

  public void addReplica(PartitionReplica replica) {
    replicas.put(replica.getId(), replica);
    if (replica.isLeader()) {
      leaderId = replica.getId();
    }
    if (preferredLeaderId == null) {
      preferredLeaderId = replica.getId();
    }
  }

  public PartitionReplica getLeader() {
    return replicas.get(leaderId);
  }

  public PartitionReplica getPreferredLeader() {
    return replicas.get(preferredLeaderId);
  }

  public boolean isLeaderPreferred() {
    return Objects.equals(leaderId, preferredLeaderId);
  }

  public List<PartitionReplica> getInSyncReplicas() {
    return inSyncReplicaStream()
        .sorted(Comparator.comparingInt(PartitionReplica::getId))
        .collect(Collectors.toList());
  }

  private Stream<PartitionReplica> inSyncReplicaStream() {
    return replicas.values().stream()
        .filter(PartitionReplica::isInSync);
  }

  public List<PartitionReplica> getOfflineReplicas() {
    return offlineReplicasStream()
        .sorted(Comparator.comparingInt(PartitionReplica::getId))
        .collect(Collectors.toList());
  }

  private Stream<PartitionReplica> offlineReplicasStream() {
    return replicas.values().stream()
        .filter(PartitionReplica::isOffline);
  }

  public boolean isUnderReplicated() {
    return inSyncReplicaStream().count() < replicas.size();
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public long getFirstOffset() {
    return firstOffset;
  }

  public void setFirstOffset(long firstOffset) {
    this.firstOffset = firstOffset;
  }

  public static final class PartitionReplica {
    private final Integer id;
    private final boolean inSync;
    private final boolean leader;
    private final boolean offline;

    public PartitionReplica(Integer id, boolean inSync, boolean leader, boolean offline) {
      this.id = id;
      this.inSync = inSync;
      this.leader = leader;
      this.offline = offline;
    }

    public Integer getId() {
      return id;
    }

    boolean isInSync() {
      return inSync;
    }

    boolean isLeader() {
      return leader;
    }

    boolean isOffline() {
      return offline;
    }
  }

  @Override
  public String toString() {
    return TopicPartitionVO.class.getSimpleName() + " [id=" + id +", firstOffset=" + firstOffset + ", size=" + size + "]";
  }
}
