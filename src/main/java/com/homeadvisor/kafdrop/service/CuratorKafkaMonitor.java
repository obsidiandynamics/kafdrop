/*
 * Copyright 2017 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.homeadvisor.kafdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.homeadvisor.kafdrop.model.BrokerVO;
import com.homeadvisor.kafdrop.model.ConsumerPartitionVO;
import com.homeadvisor.kafdrop.model.ConsumerRegistrationVO;
import com.homeadvisor.kafdrop.model.ConsumerTopicVO;
import com.homeadvisor.kafdrop.model.ConsumerVO;
import com.homeadvisor.kafdrop.model.MessageVO;
import com.homeadvisor.kafdrop.model.TopicPartitionStateVO;
import com.homeadvisor.kafdrop.model.TopicPartitionVO;
import com.homeadvisor.kafdrop.model.TopicRegistrationVO;
import com.homeadvisor.kafdrop.model.TopicVO;
import com.homeadvisor.kafdrop.util.BrokerChannel;
import com.homeadvisor.kafdrop.util.Version;
import kafka.api.ConsumerMetadataRequest;
import kafka.cluster.Broker;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.javaapi.ConsumerMetadataResponse;
import kafka.javaapi.OffsetFetchRequest;
import kafka.javaapi.OffsetFetchResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.network.BlockingChannel;
import kafka.utils.ZKGroupDirs;
import kafka.utils.ZKGroupTopicDirs;
import kafka.utils.ZkUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import com.homeadvisor.kafdrop.config.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CuratorKafkaMonitor
        implements KafkaMonitor
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private CuratorFramework curatorFramework;


    @Autowired
    private ObjectMapper objectMapper;

    private PathChildrenCache brokerPathCache;
    private PathChildrenCache topicConfigPathCache;
    private TreeCache topicTreeCache;
    private TreeCache consumerTreeCache;
    private NodeCache controllerNodeCache;

    private int controllerId = -1;

    private Map<Integer, BrokerVO> brokerCache = new TreeMap<>();

    private AtomicInteger cacheInitCounter = new AtomicInteger();

    private ForkJoinPool threadPool;

    @Autowired
    private CuratorKafkaMonitorProperties properties;
    private Version kafkaVersion;

    private RetryTemplate retryTemplate;
    @Autowired

    private KafkaHighLevelConsumer kafkaHighLevelConsumer;

    @PostConstruct
    public void start()
            throws Exception
    {
        try {
            kafkaVersion = new Version(properties.getKafkaVersion());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Invalid kafka version: " + properties.getKafkaVersion(), ex);
        }

        threadPool = new ForkJoinPool(properties.getThreadPoolSize());

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(properties.getRetry().getBackoffMillis());

        final SimpleRetryPolicy retryPolicy =
                new SimpleRetryPolicy(properties.getRetry().getMaxAttempts(),
                        ImmutableMap.of(InterruptedException.class, false,
                                Exception.class, true));

        retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        cacheInitCounter.set(4);

        brokerPathCache = new PathChildrenCache(curatorFramework, ZkUtils.BrokerIdsPath(), true);
        brokerPathCache.getListenable().addListener(new BrokerListener());
        brokerPathCache.getListenable().addListener((f, e) -> {
            if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
                cacheInitCounter.decrementAndGet();
                LOG.info("Broker cache initialized");
            }
        });
        brokerPathCache.start(StartMode.POST_INITIALIZED_EVENT);

        topicConfigPathCache = new PathChildrenCache(curatorFramework, ZkUtils.TopicConfigPath(), true);
        topicConfigPathCache.getListenable().addListener((f, e) -> {
            if (e.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
                cacheInitCounter.decrementAndGet();
                LOG.info("Topic configuration cache initialized");
            }
        });
        topicConfigPathCache.start(StartMode.POST_INITIALIZED_EVENT);

        topicTreeCache = new TreeCache(curatorFramework, ZkUtils.BrokerTopicsPath());
        topicTreeCache.getListenable().addListener((client, event) -> {
            if (event.getType() == TreeCacheEvent.Type.INITIALIZED) {
                cacheInitCounter.decrementAndGet();
                LOG.info("Topic tree cache initialized");
            }
        });
        topicTreeCache.start();

        consumerTreeCache = new TreeCache(curatorFramework, ZkUtils.ConsumersPath());
        consumerTreeCache.getListenable().addListener((client, event) -> {
            if (event.getType() == TreeCacheEvent.Type.INITIALIZED) {
                cacheInitCounter.decrementAndGet();
                LOG.info("Consumer tree cache initialized");
            }
        });
        consumerTreeCache.start();

        controllerNodeCache = new NodeCache(curatorFramework, ZkUtils.ControllerPath());
        controllerNodeCache.getListenable().addListener(this::updateController);
        controllerNodeCache.start(true);
        updateController();
    }

    private String clientId()
    {
        return properties.getClientId();
    }

    private void updateController()
    {
        Optional.ofNullable(controllerNodeCache.getCurrentData())
                .map(data -> {
                    try {
                        Map controllerData = objectMapper.reader(Map.class).readValue(data.getData());
                        return (Integer) controllerData.get("brokerid");
                    }
                    catch (IOException e) {
                        LOG.error("Unable to read controller data", e);
                        return null;
                    }
                })
                .ifPresent(this::updateController);
    }

    private void updateController(int brokerId)
    {
        brokerCache.values()
                .forEach(broker -> broker.setController(broker.getId() == brokerId));
    }

    private void validateInitialized()
    {
        if (cacheInitCounter.get() > 0) {
            throw new NotInitializedException();
        }
    }

    @PreDestroy
    public void stop()
            throws IOException
    {
        consumerTreeCache.close();
        topicConfigPathCache.close();
        brokerPathCache.close();
        controllerNodeCache.close();
    }

    private int brokerId(ChildData input)
    {
        return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
    }

    private BrokerVO addBroker(BrokerVO broker)
    {
        final BrokerVO oldBroker = brokerCache.put(broker.getId(), broker);
        LOG.info("Kafka broker {} was {}", broker.getId(), oldBroker == null ? "added" : "updated");
        return oldBroker;
    }

    private BrokerVO removeBroker(int brokerId)
    {
        final BrokerVO broker = brokerCache.remove(brokerId);
        LOG.info("Kafka broker {} was removed", broker.getId());
        return broker;
    }

    @Override
    public List<BrokerVO> getBrokers()
    {
        validateInitialized();
        List<BrokerVO> vo = brokerCache.values().stream().collect(Collectors.toList());
        return brokerCache.values().stream().collect(Collectors.toList());
    }

    @Override
    public Optional<BrokerVO> getBroker(int id)
    {
        validateInitialized();
        return Optional.ofNullable(brokerCache.get(id));
    }

    private BrokerChannel brokerChannel(Integer brokerId)
    {
        if (brokerId == null) {
            brokerId = randomBroker();
            if (brokerId == null) {
                throw new BrokerNotFoundException("No brokers available to select from");
            }
        }

        Integer finalBrokerId = brokerId;
        BrokerVO broker = getBroker(brokerId)
                .orElseThrow(() -> new BrokerNotFoundException("Broker " + finalBrokerId + " is not available"));

        return BrokerChannel.forBroker(broker.getHost(), broker.getPort());
    }

    private Integer randomBroker()
    {
        if (brokerCache.size() > 0) {
            List<Integer> brokerIds = brokerCache.keySet().stream().collect(Collectors.toList());
            Collections.shuffle(brokerIds);
            return brokerIds.get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public List<TopicVO> getTopics()
    {
        validateInitialized();
        return getTopicMetadata().values().stream()
                .sorted(Comparator.comparing(TopicVO::getName))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TopicVO>
    getTopic(String topic)
    {
        validateInitialized();
        Map<String, TopicVO> topicVoMap = getTopicMetadata(topic);
        TopicVO topicVO = null;
        if (topicVoMap.containsKey(topic)) {
            topicVO = topicVoMap.get(topic);
            topicVO.setPartitions(getTopicPartitionSizes(topicVO));
        }

        return Optional.of(topicVO);
    }

    private Map<String, TopicVO> getTopicMetadata(String... topics)
    {
        if (kafkaVersion.compareTo(new Version(0, 9, 0)) >= 0) {
            return retryTemplate.execute(
                    context -> brokerChannel(null)
                            .execute(channel -> getTopicMetadata(channel, topics)));
        }
        else {
            Stream<String> topicStream;
            if (topics == null || topics.length == 0) {
                topicStream =
                        Optional.ofNullable(
                                topicTreeCache.getCurrentChildren(ZkUtils.BrokerTopicsPath()))
                                .map(Map::keySet)
                                .map(Collection::stream)
                                .orElse(Stream.empty());
            }
            else {
                topicStream = Stream.of(topics);
            }

            return topicStream
                    .map(this::getTopicZkData)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(TopicVO::getName, topic -> topic));
        }
    }

    private TopicVO getTopicZkData(String topic)
    {
        return Optional.ofNullable(topicTreeCache.getCurrentData(ZkUtils.getTopicPath(topic)))
                .map(this::parseZkTopic)
                .orElse(null);
    }

    public TopicVO parseZkTopic(ChildData input)
    {
        try {
            final TopicVO topic = new TopicVO(StringUtils.substringAfterLast(input.getPath(), "/"));

            final TopicRegistrationVO topicRegistration =
                    objectMapper.reader(TopicRegistrationVO.class).readValue(input.getData());

            topic.setConfig(
                    Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
                            .map(this::readTopicConfig)
                            .orElse(Collections.emptyMap()));

            for (Map.Entry<Integer, List<Integer>> entry : topicRegistration.getReplicas().entrySet()) {
                final int partitionId = entry.getKey();
                try {
                    final List<Integer> partitionBrokerIds = entry.getValue();

                    final TopicPartitionVO partition = new TopicPartitionVO(partitionId);

                    final TopicPartitionStateVO partitionState = partitionState(topic.getName(), partition.getId());

                    partitionBrokerIds.stream()
                            .map(brokerId -> {
                                TopicPartitionVO.PartitionReplica replica = new TopicPartitionVO.PartitionReplica();
                                replica.setId(brokerId);
                                replica.setInService(partitionState.getIsr().contains(brokerId));
                                replica.setLeader(brokerId == partitionState.getLeader());
                                return replica;
                            })
                            .forEach(partition::addReplica);

                    topic.addPartition(partition);
                }
                catch (NullPointerException e) {
                    LOG.error("Unable to get partitionState for partitionId: {} and topic: {}", partitionId, topic.getName());
                }
            }

            // todo: get partition sizes here as single bulk request?

            return topic;
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Map<String, TopicVO> getTopicMetadata(BlockingChannel channel, String... topics)
    {

        return kafkaHighLevelConsumer.getTopicsInfo(topics);


        /*final TopicMetadataRequest request =
                new TopicMetadataRequest((short) 0, 0, clientId(), Arrays.asList(topics));

        LOG.debug("Sending topic metadata request: {}", request);

        channel.send(request);
        final kafka.api.TopicMetadataResponse underlyingResponse =
                kafka.api.TopicMetadataResponse.readFrom(channel.receive().buffer());

        LOG.debug("Received topic metadata response: {}", underlyingResponse);

        TopicMetadataResponse response = new TopicMetadataResponse(underlyingResponse);
        return response.topicsMetadata().stream()
                .filter(tmd -> tmd.errorCode() == ErrorMapping.NoError())
                .map(this::processTopicMetadata)
                .collect(Collectors.toMap(TopicVO::getName, t -> t));*/
    }

    private TopicVO processTopicMetadata(TopicMetadata tmd)
    {
        TopicVO topic = new TopicVO(tmd.topic());

        topic.setConfig(
                Optional.ofNullable(topicConfigPathCache.getCurrentData(ZkUtils.TopicConfigPath() + "/" + topic.getName()))
                        .map(this::readTopicConfig)
                        .orElse(Collections.emptyMap()));

        topic.setPartitions(
                tmd.partitionsMetadata().stream()
                        .map((pmd) -> parsePartitionMetadata(tmd.topic(), pmd))
                        .collect(Collectors.toMap(TopicPartitionVO::getId, p -> p))
        );
        return topic;
    }

    private TopicPartitionVO parsePartitionMetadata(String topic, PartitionMetadata pmd)
    {
        TopicPartitionVO partition = new TopicPartitionVO(pmd.partitionId());
        if (pmd.leader() != null) {
            partition.addReplica(new TopicPartitionVO.PartitionReplica(pmd.leader().id(), true, true));
        }

        final List<Integer> isr = getIsr(topic, pmd);
        pmd.replicas().stream()
                .map(replica -> new TopicPartitionVO.PartitionReplica(replica.id(), isr.contains(replica.id()), false))
                .forEach(partition::addReplica);
        return partition;
    }

    private List<Integer> getIsr(String topic, PartitionMetadata pmd)
    {
        return pmd.isr().stream().map(Broker::id).collect(Collectors.toList());
    }

    private Map<String, Object> readTopicConfig(ChildData d)
    {
        try {
            final Map<String, Object> configData = objectMapper.reader(Map.class).readValue(d.getData());
            return (Map<String, Object>) configData.get("config");
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private TopicPartitionStateVO partitionState(String topicName, int partitionId)
            throws IOException
    {
        return objectMapper.reader(TopicPartitionStateVO.class).readValue(
                topicTreeCache.getCurrentData(
                        ZkUtils.getTopicPartitionLeaderAndIsrPath(topicName, partitionId))
                        .getData());
    }

    @Override
    public List<ConsumerVO> getConsumers()
    {
        validateInitialized();
        return getConsumerStream(null).collect(Collectors.toList());
    }

    @Override
    public List<ConsumerVO> getConsumers(final TopicVO topic)
    {
        validateInitialized();
        return getConsumerStream(topic)
                .filter(consumer -> consumer.getTopic(topic.getName()) != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsumerVO> getConsumers(final String topic)
    {
        return getConsumers(getTopic(topic).get());
    }

    private Stream<ConsumerVO> getConsumerStream(TopicVO topic)
    {
        return consumerTreeCache.getCurrentChildren(ZkUtils.ConsumersPath()).keySet().stream()
                .map(g -> getConsumerByTopic(g, topic))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(ConsumerVO::getGroupId));
    }

    @Override
    public Optional<ConsumerVO> getConsumer(String groupId)
    {
        validateInitialized();
        return getConsumerByTopic(groupId, null);
    }

    @Override
    public Optional<ConsumerVO> getConsumerByTopicName(String groupId, String topicName)
    {
        return getConsumerByTopic(groupId, Optional.of(topicName).flatMap(this::getTopic).orElse(null));
    }

    @Override
    public Optional<ConsumerVO> getConsumerByTopic(String groupId, TopicVO topic)
    {
        final ConsumerVO consumer = new ConsumerVO(groupId);
        final ZKGroupDirs groupDirs = new ZKGroupDirs(groupId);

        if (consumerTreeCache.getCurrentData(groupDirs.consumerGroupDir()) == null) { return Optional.empty(); }

        // todo: get number of threads in each instance (subscription -> topic -> # threads)
        Optional.ofNullable(consumerTreeCache.getCurrentChildren(groupDirs.consumerRegistryDir()))
                .ifPresent(
                        children ->
                                children.keySet().stream()
                                        .map(id -> readConsumerRegistration(groupDirs, id))
                                        .forEach(consumer::addActiveInstance));

        Stream<String> topicStream = null;

        if (topic != null) {
            if (consumerTreeCache.getCurrentData(groupDirs.consumerGroupDir() + "/owners/" + topic.getName()) != null) {
                topicStream = Stream.of(topic.getName());
            }
            else {
                topicStream = Stream.empty();
            }
        }
        else {
            topicStream = Optional.ofNullable(
                    consumerTreeCache.getCurrentChildren(groupDirs.consumerGroupDir() + "/owners"))
                    .map(Map::keySet)
                    .map(Collection::stream)
                    .orElse(Stream.empty());
        }

        topicStream
                .map(ConsumerTopicVO::new)
                .forEach(consumerTopic -> {
                    getConsumerPartitionStream(groupId, consumerTopic.getTopic(), topic)
                            .forEach(consumerTopic::addOffset);
                    consumer.addTopic(consumerTopic);
                });

        return Optional.of(consumer);
    }

    @Override
    public List<MessageVO> getMessages(TopicPartition topicPartition, long offset,long count)
    {

        List<ConsumerRecord<String, String>> records = kafkaHighLevelConsumer.getLatestRecords(topicPartition, offset,count);
        List<MessageVO> messageVOS = Lists.newArrayList();
        for (ConsumerRecord<String, String> record : records) {
            MessageVO messageVo = new MessageVO();
            messageVo.setKey(record.key());
            messageVo.setMessage(record.value());
            messageVo.setChecksum(record.checksum());
            messageVo.setCompressionCodec(record.headers().toString());
            messageVo.setValid(true);

            messageVOS.add(messageVo);
        }
        return messageVOS;
    }

    private ConsumerRegistrationVO readConsumerRegistration(ZKGroupDirs groupDirs, String id)
    {
        try {
            ChildData data = consumerTreeCache.getCurrentData(groupDirs.consumerRegistryDir() + "/" + id);
            final Map<String, Object> consumerData = objectMapper.reader(Map.class).readValue(data.getData());
            Map<String, Integer> subscriptions = (Map<String, Integer>) consumerData.get("subscription");

            ConsumerRegistrationVO vo = new ConsumerRegistrationVO(id);
            vo.setSubscriptions(subscriptions);
            return vo;
        }
        catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private Stream<ConsumerPartitionVO> getConsumerPartitionStream(String groupId,
            String topicName,
            TopicVO topicOpt)
    {
        ZKGroupTopicDirs groupTopicDirs = new ZKGroupTopicDirs(groupId, topicName);

        if (topicOpt == null || topicOpt.getName().equals(topicName)) {
            topicOpt = getTopic(topicName).orElse(null);
        }

        if (topicOpt != null) {
            final TopicVO topic = topicOpt;

            Map<Integer, Long> consumerOffsets = getConsumerOffsets(groupId, topic);

            return topic.getPartitions().stream()
                    .map(partition -> {
                        int partitionId = partition.getId();

                        final ConsumerPartitionVO consumerPartition = new ConsumerPartitionVO(groupId, topicName, partitionId);
                        consumerPartition.setOwner(
                                Optional.ofNullable(
                                        consumerTreeCache.getCurrentData(groupTopicDirs.consumerOwnerDir() + "/" + partitionId))
                                        .map(data -> new String(data.getData()))
                                        .orElse(null));

                        consumerPartition.setOffset(consumerOffsets.getOrDefault(partitionId, -1L));

                        final Optional<TopicPartitionVO> topicPartition = topic.getPartition(partitionId);
                        consumerPartition.setSize(topicPartition.map(TopicPartitionVO::getSize).orElse(-1L));
                        consumerPartition.setFirstOffset(topicPartition.map(TopicPartitionVO::getFirstOffset).orElse(-1L));

                        return consumerPartition;
                    });
        }
        else {
            return Stream.empty();
        }
    }

    private Map<Integer, Long> getConsumerOffsets(String groupId, TopicVO topic)
    {
        try {
            // Kafka doesn't really give us an indication of whether a consumer is
            // using Kafka or Zookeeper based offset tracking. So look up the offsets
            // for both and assume that the largest offset is the correct one.

            ForkJoinTask<Map<Integer, Long>> kafkaTask =
                    threadPool.submit(() -> getConsumerOffsets(groupId, topic, false));

            ForkJoinTask<Map<Integer, Long>> zookeeperTask =
                    threadPool.submit(() -> getConsumerOffsets(groupId, topic, true));

            Map<Integer, Long> zookeeperOffsets = zookeeperTask.get();
            Map<Integer, Long> kafkaOffsets = kafkaTask.get();
            zookeeperOffsets.entrySet()
                    .forEach(entry -> kafkaOffsets.merge(entry.getKey(), entry.getValue(), Math::max));
            return kafkaOffsets;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(ex);
        }
        catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    private Map<Integer, Long> getConsumerOffsets(String groupId,
            TopicVO topic,
            boolean zookeeperOffsets)
    {
        return retryTemplate.execute(
                context -> brokerChannel(zookeeperOffsets ? null : offsetManagerBroker(groupId))
                        .execute(channel -> getConsumerOffsets(channel, groupId, topic, zookeeperOffsets)));
    }

    /**
     * Returns the map of partitionId to consumer offset for the given group and
     * topic. Uses the given blocking channel to execute the offset fetch request.
     *
     * @param channel The channel to send requests on
     * @param groupId Consumer group to use
     * @param topic Topic to query
     * @param zookeeperOffsets If true, use a version of the API that retrieves
     * offsets from Zookeeper. Otherwise use a version
     * that pulls the offsets from Kafka itself.
     * @return Map where the key is partitionId and the value is the consumer
     * offset for that partition.
     */
    private Map<Integer, Long> getConsumerOffsets(BlockingChannel channel,
            String groupId,
            TopicVO topic,
            boolean zookeeperOffsets)
    {

        final OffsetFetchRequest request = new OffsetFetchRequest(
                groupId,
                topic.getPartitions().stream()
                        .map(p -> new TopicAndPartition(topic.getName(), p.getId()))
                        .collect(Collectors.toList()),
                (short) (zookeeperOffsets ? 0 : 1), 0, // version 0 = zookeeper offsets, 1 = kafka offsets
                clientId());

        LOG.debug("Sending consumer offset request: {}", request);

        channel.send(request.underlying());

        final kafka.api.OffsetFetchResponse underlyingResponse =
                kafka.api.OffsetFetchResponse.readFrom(channel.receive().buffer());

        LOG.debug("Received consumer offset response: {}", underlyingResponse);

        OffsetFetchResponse response = new OffsetFetchResponse(underlyingResponse);

        return response.offsets().entrySet().stream()
                .filter(entry -> entry.getValue().error() == ErrorMapping.NoError())
                .collect(Collectors.toMap(entry -> entry.getKey().partition(), entry -> entry.getValue().offset()));
    }

    /**
     * Returns the broker Id that is the offset coordinator for the given group id. If not found, returns null
     */
    private Integer offsetManagerBroker(String groupId)
    {
        return retryTemplate.execute(
                context ->
                        brokerChannel(null)
                                .execute(channel -> offsetManagerBroker(channel, groupId))
        );
    }

    private Integer offsetManagerBroker(BlockingChannel channel, String groupId)
    {
        final ConsumerMetadataRequest request =
                new ConsumerMetadataRequest(groupId, (short) 0, 0, clientId());

        LOG.debug("Sending consumer metadata request: {}", request);

        channel.send(request);
        ConsumerMetadataResponse response =
                ConsumerMetadataResponse.readFrom(channel.receive().buffer());

        LOG.debug("Received consumer metadata response: {}", response);

        return (response.errorCode() == ErrorMapping.NoError()) ? response.coordinator().id() : null;
    }

    private Map<Integer, TopicPartitionVO> getTopicPartitionSizes(TopicVO topic)
    {
        return kafkaHighLevelConsumer.getPartitionSize(topic.getName());
    }



    /*private Map<Integer, TopicPartitionVO> sendOffsetRequest(Integer brokerId, TopicVO topic,
            PartitionOffsetRequestInfo requestInfo,
            List<TopicPartitionVO> brokerPartitions)
    {
        final OffsetRequest offsetRequest = new OffsetRequest(
                brokerPartitions.stream()
                        .collect(Collectors.toMap(
                                partition -> new TopicAndPartition(topic.getName(), partition.getId()),
                                partition -> requestInfo)),
                (short) 0, clientId());

        LOG.debug("Sending offset request: {}", offsetRequest);
        return kafkaHighLevelConsumer.getPartitionSize(topic.getName());
    }*/

    private class BrokerListener
            implements PathChildrenCacheListener
    {
        @Override
        public void childEvent(CuratorFramework framework, PathChildrenCacheEvent event)
                throws Exception
        {
            switch (event.getType()) {
                case CHILD_REMOVED: {
                    BrokerVO broker = removeBroker(brokerId(event.getData()));
                    break;
                }

                case CHILD_ADDED:
                case CHILD_UPDATED: {
                    addBroker(parseBroker(event.getData()));
                    break;
                }

                case INITIALIZED: {
                    brokerPathCache.getCurrentData().stream()
                            .map(BrokerListener.this::parseBroker)
                            .forEach(CuratorKafkaMonitor.this::addBroker);
                    break;
                }
            }
            updateController();
        }

        private int brokerId(ChildData input)
        {
            return Integer.parseInt(StringUtils.substringAfter(input.getPath(), ZkUtils.BrokerIdsPath() + "/"));
        }

        private BrokerVO parseBroker(ChildData input)
        {
            try {
                final BrokerVO broker = objectMapper.reader(BrokerVO.class).readValue(input.getData());
                broker.setId(brokerId(input));
                return broker;
            }
            catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
