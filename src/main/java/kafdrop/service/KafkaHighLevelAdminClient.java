package kafdrop.service;

import jakarta.annotation.PostConstruct;
import kafdrop.config.KafkaConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafdrop-admin");
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
      groupListing = adminClient.listConsumerGroups().valid().get();
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

  Map<String, Config> describeTopicConfigs(Set<String> topicNames) {
    final var resources = topicNames.stream()
      .map(topic -> new ConfigResource(Type.TOPIC, topic))
      .collect(Collectors.toList());
    final var result = adminClient.describeConfigs(resources);
    final Map<String, Config> configsByTopic;
    try {
      final var allConfigs = result.all().get();
      configsByTopic = new HashMap<>(allConfigs.size(), 1f);
      for (var entry : allConfigs.entrySet()) {
        configsByTopic.put(entry.getKey().name(), entry.getValue());
      }
    } catch (InterruptedException | ExecutionException e) {
      if (e.getCause() instanceof UnsupportedVersionException) {
        return Map.of();
      } else if (e.getCause() instanceof TopicAuthorizationException) {
        printAcls();
        return Map.of();
      }
      throw new KafkaAdminClientException(e);
    }
    return configsByTopic;
  }

  /**
   * Create topic or throw ${@code KafkaAdminClientException}
   *
   * @param newTopic topic to create
   * @throws KafkaAdminClientException if computation threw an Exception
   */
  void createTopic(NewTopic newTopic) {
    final var creationResult = adminClient.createTopics(List.of(newTopic));
    try {
      creationResult.all().get();
      LOG.info("Topic {} successfully created", newTopic.name());
    } catch (InterruptedException | ExecutionException e) {
      LOG.error("Error while creating topic", e);
      throw new KafkaAdminClientException(e);
    }
  }

  /**
   * Delete topic or throw ${@code KafkaAdminClientException}
   *
   * @param topic name of the topic to delete
   * @throws KafkaAdminClientException if computation threw an Exception
   */
  void deleteTopic(String topic) {
    DeleteTopicsOptions options = new DeleteTopicsOptions();
    options.timeoutMs(5000); // timeout after 5 seconds
    final var deleteTopicsResult = adminClient.deleteTopics(List.of(topic), options);
    try {
      deleteTopicsResult.all().get();
      LOG.info("Topic {} successfully deleted", topic);
    } catch (InterruptedException | ExecutionException e) {
      LOG.error("Error while deleting topic", e);
      throw new KafkaAdminClientException(e);
    }
  }

  Collection<AclBinding> listAcls() {
    final Collection<AclBinding> aclsBindings;
    try {
      aclsBindings = adminClient.describeAcls(new AclBindingFilter(ResourcePatternFilter.ANY,
          AccessControlEntryFilter.ANY))
        .values().get();
    } catch (InterruptedException | ExecutionException e) {
      if (e.getCause() instanceof SecurityDisabledException) {
        return Collections.emptyList();
      } else {
        throw new KafkaAdminClientException(e);
      }
    }
    return aclsBindings;
  }

  private void printAcls() {
    try {
      final var acls = adminClient.describeAcls(new AclBindingFilter(ResourcePatternFilter.ANY,
        AccessControlEntryFilter.ANY)).values().get();
      final var newlineDelimitedAcls = new StringBuilder();
      for (var acl : acls) {
        newlineDelimitedAcls.append('\n').append(acl);
      }
      LOG.info("ACLs: {}", newlineDelimitedAcls);
    } catch (InterruptedException | ExecutionException e) {
      LOG.error("Error describing ACLs", e);
    }
  }
}
