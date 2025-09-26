package kafdrop.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

final class TopicVOTest {

  @Test
  void constructor_nullName_throwsNPE() {
    Assertions.assertThrows(NullPointerException.class, () -> new TopicVO(null));
  }

  @Test
  void setConfig_null_becomesEmptyMap() {
    var topic = new TopicVO("test");
    topic.setConfig(null);
    Assertions.assertNotNull(topic.getConfig());
    Assertions.assertTrue(topic.getConfig().isEmpty());
  }

  @Test
  void setPartitions_null_throwsNPE() {
    var topic = new TopicVO("test");
    Assertions.assertThrows(NullPointerException.class, () -> topic.setPartitions(null));
  }

  @Test
  void totals_withRealPartitions_areComputed() {
    var topic = new TopicVO("test");

    TopicPartitionVO p0 = new TopicPartitionVO(0);
    p0.setSize(100L);
    p0.setFirstOffset(10L);

    TopicPartitionVO p1 = new TopicPartitionVO(1);
    p1.setSize(50L);
    p1.setFirstOffset(5L);

    Map<Integer, TopicPartitionVO> partitions = new HashMap<>();
    partitions.put(0, p0);
    partitions.put(1, p1);

    topic.setPartitions(partitions);

    Assertions.assertEquals(150L, topic.getTotalSize());
    Assertions.assertEquals(135L, topic.getAvailableSize());
  }
}
