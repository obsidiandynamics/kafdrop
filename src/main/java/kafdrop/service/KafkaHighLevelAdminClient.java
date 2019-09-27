package kafdrop.service;

import kafdrop.config.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Service
public final class KafkaHighLevelAdminClient {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaHighLevelAdminClient.class);

  private final KafkaConfiguration kafkaConfiguration;

  private AdminClient adminClient;

  public KafkaHighLevelAdminClient(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  public void init() {
    final var properties = new Properties();
    kafkaConfiguration.applyCommon(properties);
    adminClient = AdminClient.create(properties);
  }

  final class ClusterDescription {
    final Collection<Node> nodes;
    final Node controller;
    final String clusterId;

    ClusterDescription(Collection<Node> nodes, Node controller, String clusterId) {
      this.nodes = nodes;
      this.controller = controller;
      this.clusterId = clusterId;
    }
  }

  ClusterDescription describeCluster() {
    final var result = adminClient.describeCluster();
    final Collection<Node> nodes;
    final Node controller;
    final String clusterId;
    try {
      nodes = result.nodes().get();
      controller = result.controller().get();
      clusterId = result.clusterId().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new KafkaAdminClientException(e);
    }

    return new ClusterDescription(nodes, controller, clusterId);
  }

  Set<String> listConsumerGroups() {
    final Collection<ConsumerGroupListing> groupListing;
    try {
      groupListing = adminClient.listConsumerGroups().all().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new KafkaAdminClientException(e);
    }
    return groupListing.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toSet());
  }

  Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsetsIfAuthorized(String groupId) {
    final var offsets = adminClient.listConsumerGroupOffsets(groupId);
    try {
      return offsets.partitionsToOffsetAndMetadata().get();
    } catch (InterruptedException | ExecutionException e) {
      if (e.getCause() instanceof GroupAuthorizationException) {
        LOG.info("Not authorized to view consumer group {}; skipping", groupId);
        return Collections.emptyMap();
      } else {
        throw new KafkaAdminClientException(e);
      }
    }
  }
}
