package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.model.BrokerVO;
import com.homeadvisor.kafdrop.model.ConsumerVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import kafka.javaapi.consumer.SimpleConsumer;

import java.util.List;
import java.util.Optional;

public interface KafkaMonitor
{
   SimpleConsumer getSimpleConsumer(int brokerId) throws BrokerNotFoundException;

   List<BrokerVO> getBrokers();

   Optional<BrokerVO> getBroker(int id);

   List<TopicVO> getTopics();

   Optional<TopicVO> getTopic(String topic);

   List<ConsumerVO> getConsumers();

   List<ConsumerVO> getConsumers(TopicVO topic);

   List<ConsumerVO> getConsumers(String topic);

   Optional<ConsumerVO> getConsumer(String groupId);

   Optional<ConsumerVO> getConsumerByTopicName(String groupId, Optional<String> topic);

   Optional<ConsumerVO> getConsumerByTopic(String groupId, Optional<TopicVO> topic);
}
