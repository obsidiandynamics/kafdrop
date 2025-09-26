/*
 * Copyright 2016 Kafdrop contributors.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConsumerPartitionVOTest {
  private void doLagTest(long first, long last, long offset, long expectedLag) {
    final var partition = new ConsumerPartitionVO("test", "test", 0);
    partition.setFirstOffset(first);
    partition.setSize(last);
    partition.setOffset(offset);
    assertEquals(expectedLag, partition.getLag(), "Unexpected lag");
  }

  @Test
  void testGetLag() {
    doLagTest(0, 0, 0, 0);
    doLagTest(-1, -1, -1, 0);
    doLagTest(5, 10, 8, 2);
    doLagTest(5, 10, 2, 5);
    doLagTest(6, 6, 2, 0);
    doLagTest(5, 10, -1, 5);
  }

  @Test
  void constructor_nullGroupId_throwsNPE() {
    org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new ConsumerPartitionVO(null, "t", 0));
  }

  @Test
  void constructor_nullTopic_throwsNPE() {
    org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new ConsumerPartitionVO("g", null, 0));
  }

  @Test
  void getters_basic() {
    var p = new ConsumerPartitionVO("g", "t", 7);
    assertEquals("t", p.getTopic());
    assertEquals(7, p.getPartitionId());
  }

  @Test
  void setters_roundTrip() {
    var p = new ConsumerPartitionVO("g", "t", 0);
    p.setSize(123L);
    p.setFirstOffset(10L);
    p.setOffset(120L);
    assertEquals(123L, p.getSize());
    assertEquals(10L, p.getFirstOffset());
    assertEquals(120L, p.getOffset());
  }

  @Test
  void getLag_offsetEqualsFirstOffset() {
    doLagTest(10, 100, 10, 90);
  }

  @Test
  void toString_containsFields() {
    var p = new ConsumerPartitionVO("group-1", "topic-1", 2);
    p.setSize(50L);
    p.setFirstOffset(5L);
    p.setOffset(11L);
    var s = p.toString();
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("groupId=group-1"));
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("topic=topic-1"));
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("partitionId=2"));
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("offset=11"));
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("size=50"));
    org.junit.jupiter.api.Assertions.assertTrue(s.contains("firstOffset=5"));
  }

}
