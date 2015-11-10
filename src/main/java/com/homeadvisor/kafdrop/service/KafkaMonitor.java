package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.model.BrokerVO;
import com.homeadvisor.kafdrop.model.ConsumerVO;
import com.homeadvisor.kafdrop.model.TopicVO;

import java.util.List;
import java.util.Optional;

public interface KafkaMonitor
{
   List<BrokerVO> getBrokers();

   Optional<BrokerVO> getBroker(int id);

   List<TopicVO> getTopics();

   Optional<TopicVO> getTopic(String topic);

   List<ConsumerVO> getConsumers();

   List<ConsumerVO> getConsumers(String topic);

   Optional<ConsumerVO> getConsumer(String groupId);

   Optional<ConsumerVO> getConsumer(String groupId, Optional<String> topic);
}
