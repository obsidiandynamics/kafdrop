package kafdrop.service;

import kafdrop.config.*;
import org.apache.kafka.clients.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.config.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Service
public final class KafkaHighLevelAdminClient {
  private final KafkaConfiguration kafkaConfiguration;

  private AdminClient adminClient;

  public KafkaHighLevelAdminClient(KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
  }

  @PostConstruct
  public void init() {
    final var props = new Properties();
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBrokerConnect());
    if (kafkaConfiguration.getIsSecured()) {
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaConfiguration.getSecurityProtocol());
        props.put(SaslConfigs.SASL_MECHANISM, kafkaConfiguration.getSaslMechanism());
    }
    adminClient = AdminClient.create(props);
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

  Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsets(String groupId) {
    final var offsets = adminClient.listConsumerGroupOffsets(groupId);
    try {
      return offsets.partitionsToOffsetAndMetadata().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new KafkaAdminClientException(e);
    }
  }
}
