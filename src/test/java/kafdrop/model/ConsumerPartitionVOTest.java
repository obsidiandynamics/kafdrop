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

import static org.junit.jupiter.api.Assertions.*;

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
}