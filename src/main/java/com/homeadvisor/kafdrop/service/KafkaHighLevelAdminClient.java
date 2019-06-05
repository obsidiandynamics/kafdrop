package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.config.*;
import org.apache.kafka.clients.*;
import org.apache.kafka.clients.admin.*;
import org.slf4j.*;
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
    final var props = new Properties();
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.getBrokerConnect());
    adminClient = AdminClient.create(props);
  }

  public Set<String> getConsumerGroups() {
    try {
      final var groupListing = adminClient.listConsumerGroups().all().get();
      return groupListing.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toSet());
    } catch (InterruptedException | ExecutionException e) {
      LOG.warn("Error fetching consumer groups", e);
      return Collections.emptySet();
    }
  }
}
