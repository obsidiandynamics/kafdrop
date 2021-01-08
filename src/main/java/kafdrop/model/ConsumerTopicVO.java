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

public final class ConsumerTopicVO {
  private final String topic;
  private final Map<Integer, ConsumerPartitionVO> offsets = new TreeMap<>();

  public ConsumerTopicVO(String topic) {
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }

  public void addOffset(ConsumerPartitionVO offset) {
    offsets.put(offset.getPartitionId(), offset);
  }

  public long getLag() {
    return offsets.values().stream()
        .map(ConsumerPartitionVO::getLag)
        .filter(lag -> lag >= 0)
        .reduce(0L, Long::sum);
  }

  public Collection<ConsumerPartitionVO> getPartitions() {
    return offsets.values();
  }
}
