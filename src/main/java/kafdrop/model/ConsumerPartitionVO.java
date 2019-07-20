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

public final class ConsumerPartitionVO {
  private final String groupId;
  private final String topic;
  private final int partitionId;
  private long offset;
  private long size;
  private long firstOffset;

  public ConsumerPartitionVO(String groupId, String topic, int partitionId) {
    this.groupId = groupId;
    this.topic = topic;
    this.partitionId = partitionId;
  }

  public String getTopic() {
    return topic;
  }

  public int getPartitionId() {
    return partitionId;
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

  public long getLag() {
    if (size < 0 || firstOffset < 0) {
      return 0;
    } else if (offset < firstOffset) {
      return size - firstOffset;
    } else {
      return size - offset;
    }
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  @Override
  public String toString() {
    return ConsumerPartitionVO.class.getSimpleName() + " [groupId=" + groupId +
        ", topic=" + topic + ", partitionId=" + partitionId + ", offset=" + offset +
        ", size=" + size + ", firstOffset=" + firstOffset + "]";
  }
}
