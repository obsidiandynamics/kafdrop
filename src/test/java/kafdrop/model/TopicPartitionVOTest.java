package kafdrop.model;

import org.junit.jupiter.api.Test;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

class TopicPartitionVOTest {
  @Test
  void getReplicas_isUnmodifiable() {
    TopicPartitionVO tp = new TopicPartitionVO(0);
    tp.addReplica(new TopicPartitionVO.PartitionReplica(1, true, true, false));

    Collection<TopicPartitionVO.PartitionReplica> reps = tp.getReplicas();
    assertEquals(1, reps.size());

    assertThrows(UnsupportedOperationException.class, () -> reps.remove(reps.iterator().next()));
    assertEquals(1, tp.getReplicas().size(), "Internal state must not be mutated by callers");
  }
}

