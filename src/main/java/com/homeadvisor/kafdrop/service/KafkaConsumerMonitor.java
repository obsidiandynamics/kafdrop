package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.model.*;
import org.slf4j.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
public final class KafkaConsumerMonitor implements ConsumerMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerMonitor.class);

  private final KafkaHighLevelConsumer highLevelConsumer;

  private final KafkaHighLevelAdminClient highLevelAdminClient;

  public KafkaConsumerMonitor(KafkaHighLevelConsumer highLevelConsumer,
                              KafkaHighLevelAdminClient highLevelAdminClient) {
    this.highLevelConsumer = highLevelConsumer;
    this.highLevelAdminClient = highLevelAdminClient;
  }

  @Override
  public List<ConsumerVO> getConsumers(TopicVO topic) {
    final var consumerGroups = highLevelAdminClient.getConsumerGroups();

    return null;//TODO
  }
}
