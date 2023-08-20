package kafdrop.service;

import kafdrop.model.BrokerVO;
import kafdrop.model.ClusterDescriptionVO;
import kafdrop.model.ClusterSummaryVO;
import kafdrop.model.TopicVO;
import org.apache.kafka.common.Node;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class KafkaMonitorImplTest {

  private KafkaMonitor kafkaMonitor;
  private KafkaHighLevelConsumer highLevelConsumer = mock(KafkaHighLevelConsumer.class);
  private KafkaHighLevelAdminClient highLevelAdminClient = mock(KafkaHighLevelAdminClient.class);

  @BeforeEach
  void setUp() {
    kafkaMonitor = new KafkaMonitorImpl(
      highLevelConsumer,
      highLevelAdminClient
    );
  }

  @Test
  void cluster_should_return_brokers() {
    mockThreeAdminNodes();

    var brokers = kafkaMonitor.getBrokers();

    assertThat(brokers).containsExactlyInAnyOrder(
      new BrokerVO(1, "host1", 1, "rack", false),
      new BrokerVO(2, "host2", 1, "rack", false),
      new BrokerVO(3, "host3", 1, "rack", true)
    );
  }


  @Test
  void getClusterSummmary_returns_expected_summary_for_cluster() {
    var topics = Instancio.createSet(TopicVO.class);
    int topicCount = calculateTopicCount(topics);
    int partitionCount = calculatePartitions(topics);
    int underReplicatedCount = calculateReplicas(topics);
    double preferredReplicaPercent = calculatePreferredReplicaRepcent(topics);

    var clusterSummary = kafkaMonitor.getClusterSummary(topics);

    assertThat(clusterSummary)
      .returns(partitionCount, ClusterSummaryVO::getPartitionCount)
      .returns(topicCount, ClusterSummaryVO::getTopicCount)
      .returns(underReplicatedCount, ClusterSummaryVO::getUnderReplicatedCount)
      .returns(preferredReplicaPercent, ClusterSummaryVO::getPreferredReplicaPercent);
  }

  private double calculatePreferredReplicaRepcent(Set<TopicVO> set) {
    return set.stream().map(TopicVO::getPreferredReplicaPercent)
      .mapToDouble(it -> it)
      .sum();
  }

  private int calculateReplicas(Set<TopicVO> set) {
    return set.stream().map(TopicVO::getUnderReplicatedPartitions)
      .map(Collection::size)
      .mapToInt(it -> it)
      .sum();
  }

  private int calculatePartitions(Set<TopicVO> set) {
    return set.stream().map(TopicVO::getPartitions)
      .map(Collection::size)
      .mapToInt(it -> it)
      .sum();
  }

  private int calculateTopicCount(Set<TopicVO> set) {
    return set.size();
  }

  private void mockThreeAdminNodes() {
    var controller = new Node(3, "host3", 1, "rack");
    var nodes = List.of(
      new Node(1, "host1", 1, "rack"),
      new Node(2, "host2", 1, "rack"),
      controller
    );
    var clusterId = new ClusterDescriptionVO(nodes, controller, "clusterId");
    given(highLevelAdminClient.describeCluster()).willReturn(clusterId);
  }
}
