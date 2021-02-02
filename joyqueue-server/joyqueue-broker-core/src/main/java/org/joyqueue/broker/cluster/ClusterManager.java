/**
 * Copyright 2019 The JoyQueue Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.broker.cluster;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.cluster.config.ClusterConfig;
import org.joyqueue.broker.cluster.entry.ClusterNode;
import org.joyqueue.broker.cluster.event.CompensateEvent;
import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.broker.consumer.ConsumeConfigKey;
import org.joyqueue.domain.AppToken;
import org.joyqueue.domain.Broker;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Subscription;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.event.ConsumerEvent;
import org.joyqueue.event.EventType;
import org.joyqueue.event.MetaEvent;
import org.joyqueue.event.NameServerEvent;
import org.joyqueue.event.ProducerEvent;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.event.AddConsumerEvent;
import org.joyqueue.nsr.event.AddPartitionGroupEvent;
import org.joyqueue.nsr.event.AddProducerEvent;
import org.joyqueue.nsr.event.AddTopicEvent;
import org.joyqueue.nsr.event.RemoveConsumerEvent;
import org.joyqueue.nsr.event.RemovePartitionGroupEvent;
import org.joyqueue.nsr.event.RemoveProducerEvent;
import org.joyqueue.nsr.event.RemoveTopicEvent;
import org.joyqueue.nsr.event.UpdateBrokerEvent;
import org.joyqueue.nsr.event.UpdateConsumerEvent;
import org.joyqueue.nsr.event.UpdatePartitionGroupEvent;
import org.joyqueue.nsr.event.UpdateProducerEvent;
import org.joyqueue.nsr.event.UpdateTopicEvent;
import org.joyqueue.response.BooleanResponse;
import org.joyqueue.toolkit.concurrent.EventBus;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.lang.LifeCycle;
import org.joyqueue.toolkit.network.IpUtil;
import org.joyqueue.toolkit.service.Service;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群管理
 * <p>
 * clusterManager提供接口都是本broker上相关的数据，如果不存在则返回null
 *
 * @author chengzhiliang on 2018/8/22.
 */
public class ClusterManager extends Service {

    private final Logger logger = LoggerFactory.getLogger(ClusterManager.class);
    //broker ID 存储目录
    private File brokerIdFile;
    //broker config
    private volatile BrokerConfig brokerConfig;
    //private volatile Integer brokerId;
    private volatile Broker broker;
    // 元数据管理
    private NameService nameService;
    // 集群管理本地缓存
    private MetaDataLocalCache localCache;
    // 元数据事件
    private EventBus<MetaEvent> eventBus = new EventBus("joyqueue-cluster-eventBus");

    protected String NODE_NAME_ENV = "NODE_NAME";

    private BrokerContext brokerContext;
    private ClusterNameService clusterNameService;
    private ClusterConfig config;

    public ClusterManager(BrokerConfig brokerConfig, NameService nameService, ClusterNameService clusterNameService, BrokerContext brokerContext) {
        this.brokerConfig = brokerConfig;
        this.nameService = nameService;
        this.clusterNameService = clusterNameService;
        this.brokerContext = brokerContext;
        this.config = new ClusterConfig(brokerContext.getPropertySupplier());
    }


    @Override
    protected void validate() throws Exception {
        super.validate();
        Preconditions.checkArgument(brokerConfig != null, "brokerConfig can not be null.");
        Preconditions.checkArgument(nameService != null, "nameService can not be null.");

        if (brokerIdFile == null) {
            String path = brokerConfig.getBrokerIdFilePath();
            if (path != null) {
                brokerIdFile = new File(path);
            }
        }
        Preconditions.checkArgument(brokerIdFile != null, "broker ID file can not be null.");
        if (!brokerIdFile.exists()) {
            brokerIdFile.createNewFile();
        }
        if (localCache == null) {
            localCache = new MetaDataLocalCache(nameService);
        }
    }


    @Override
    protected void doStart() throws Exception {
        super.doStart();
        localCache.start();
        eventBus.start();
        register();
        clusterNameService.setBroker(broker);
        localCache.initCache();
        logger.info("clusterManager is started");
    }

    /**
     * 注册broker
     *
     * @return
     */
    private void register() throws Exception {
        String localHost = brokerConfig.getFrontendConfig().getHost();
        String localIp = IpUtil.getDefaultLocalIp();
        long port = brokerConfig.getFrontendConfig().getPort();
        Integer brokerId = readBroker();
        String node = IpUtil.getNode();
        System.getenv(NODE_NAME_ENV);
        broker = nameService.register(brokerId, localHost, localIp, (int) port, node);
        // brokerId
        if (broker == null) {
            logger.error("brokerId[{}] [{}:{}:{}] 注册失败", brokerId, localHost, localIp, port);
            throw new JoyQueueException(JoyQueueCode.CN_SERVICE_NOT_AVAILABLE);
        }
        brokerConfig.setBroker(broker);
        writeBroker(broker.getId());
    }

    /**
     * 添加元数据变更的事件监听
     *
     * @param listener 事件监听
     */
    public void addListener(EventListener<MetaEvent> listener) {
        eventBus.addListener(listener);
    }

    /**
     * 获取数据中心
     *
     * @param ipAddr
     * @return 数据中心
     */
    public DataCenter getDataCenterByIP(String ipAddr) {
        DataCenter ipInfo = nameService.getDataCenter(ipAddr);
        return ipInfo;
    }

    /**
     * 获取broker编号
     *
     * @return
     */
    public Integer getBrokerId() {
        return broker.getId();
    }

    /**
     * 获取当前broker实例config信息
     *
     * @return
     */
    public BrokerConfig getConfig() {
        return brokerConfig;
    }

    /**
     * 获取当前broker信息
     *
     * @return
     */
    public Broker getBroker() {
        return broker;
    }

    /**
     * 获取当前broker信息
     *
     * @return
     */
    public Broker getBrokerById(Integer brokerId) {
        if(brokerId.equals(getBrokerId())){
            return broker;
        }
        return nameService.getBroker(brokerId);
    }

    /**
     * 根据主题+分区查询对应的主master
     *
     * @param topic 主题
     * @param group group
     * @return
     */
    public PartitionGroup getPartitionGroupByGroup(TopicName topic, int group) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            return null;
        }
        return topicConfig.fetchPartitionGroupByGroup(group);
    }

    /**
     * 获取本地broker中topic信息(非该broker相关的topic不应该返回数据,实现需要根据broker.id和topic加起来获取nameservice的topicConfig)
     *
     * @param topic
     * @return
     */
    public TopicConfig getTopicConfig(TopicName topic) {
        return localCache.getTopicConfig(topic);
    }

    /**
     * 获取本地broker所有topic信息
     *
     * @return
     */
    public List<TopicConfig> getTopics() {
        Map<TopicName, TopicConfig> topics = nameService.getTopicConfigByBroker(getBrokerId());
        if (MapUtils.isEmpty(topics)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(topics.values());
    }

    /**
     * 获取本地broker上该topic对应的partitionGroup
     *
     * @param topic
     * @return
     */
    public List<PartitionGroup> getLocalPartitionGroups(TopicName topic) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (null == topicConfig) {
            return null;
        }
        return getLocalPartitionGroups(topicConfig);
    }

    public List<PartitionGroup> getLocalPartitionGroups(TopicConfig topicConfig) {
        List<PartitionGroup> result = Lists.newArrayListWithCapacity(topicConfig.getPartitionGroups().size());
        for (Map.Entry<Integer, PartitionGroup> entry : topicConfig.getPartitionGroups().entrySet()) {
            if (isLeader(entry.getValue())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public List<Short> getLocalPartitions(TopicName topic) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (null == topicConfig) {
            return null;
        }
        return getLocalPartitions(topicConfig);
    }

    public List<Short> getLocalPartitions(TopicConfig topicConfig) {
        try {
            return localCache.getPartitionCache().get(topicConfig, () -> {
                List<Short> result = Lists.newArrayListWithCapacity(topicConfig.getPartitions());
                for (Map.Entry<Integer, PartitionGroup> entry : topicConfig.getPartitionGroups().entrySet()) {
                    if (isLeader(entry.getValue())) {
                        result.addAll(entry.getValue().getPartitions());
                    }
                }
                return result;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取本地broker上该topic对应的partitionGroup 包含replica
     *
     * @param topic
     * @return
     */
    public List<PartitionGroup> getTopicPartitionGroups(TopicName topic) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (null == topicConfig) {
            return null;
        }
        return topicConfig.fetchTopicPartitionGroupsByBrokerId(broker.getId());
    }

    /**
     * 获取分区对应的分区组
     *
     * @param topic
     * @param partition
     * @return
     */
    public PartitionGroup getPartitionGroup(TopicName topic, short partition) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig != null) {
            return topicConfig.fetchPartitionGroupByPartition(partition);
        }
        return null;
    }

    /**
     * 获取分区对于分区组的ID
     *
     * @param topic
     * @param partition
     * @return
     */
    public Integer getPartitionGroupId(TopicName topic, short partition) {
        PartitionGroup partitionGroup = getPartitionGroup(topic, partition);
        Integer group = null;
        if (partitionGroup != null) {
            group = partitionGroup.getGroup();
        }
        return group;
    }

    /**
     * 获取本地broker上该topic对应的replicaGroup
     *
     * @param topic
     * @return
     */
    public List<Integer> getReplicaGroups(TopicName topic) {
        List<Integer> result = new ArrayList<>();
        List<PartitionGroup> partitionGroup = getLocalPartitionGroups(topic);
        if (partitionGroup != null) {
            partitionGroup.stream().forEach(item ->
                    result.addAll(item.getReplicas())
            );
        }
        return result;
    }

    public List<Short> getReplicaPartitions(TopicName topic) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            return Collections.emptyList();
        }
        List<Short> result = Lists.newArrayListWithCapacity(topicConfig.getPartitions());
        for (Map.Entry<Integer, PartitionGroup> entry : topicConfig.getPartitionGroups().entrySet()) {
            if (entry.getValue().getReplicas().contains(getBrokerId())) {
                result.addAll(entry.getValue().getPartitions());
            }
        }
        return result;
    }

    /**
     * 获取该topic对应的所有partition
     *
     * @param topic
     * @return
     */
    public List<Short> getPartitionList(TopicName topic) {
        TopicConfig config = getTopicConfig(topic);
        if (config != null) {
            return new ArrayList<>(config.fetchAllPartitions());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定主题在broker上高优先级分区集合
     *
     * @param topic 主题
     * @return 分区集合
     */
    public List<Short> getPriorityPartitionList(TopicName topic) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig != null) {
            Set<Short> priorityPartitions = topicConfig.getPriorityPartitions();
            if (priorityPartitions != null) {
                return new ArrayList<>(priorityPartitions);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 获取消费策略
     *
     * @param topic
     * @param app
     * @return
     */
    public Consumer.ConsumerPolicy getConsumerPolicy(TopicName topic, String app) throws JoyQueueException {
        Consumer consumer = localCache.getConsumerByTopicAndApp(topic, app);
        if (null == consumer) {
            if (StringUtils.equals(brokerConfig.getAdminUser(), app)) {
                return brokerContext.getConsumerPolicy();
            }
            throw new JoyQueueException(JoyQueueCode.FW_CONSUMER_NOT_EXISTS);
        }
        return getConsumerPolicyOrDefault(consumer);
    }

    public PropertySupplier getPropertySupplier() {
        return brokerContext.getPropertySupplier();
    }

    public int getRetryRandomBound(String topic, String app) {
        int topicRandomBound = doGetTopicRetryRandomBound(topic);
        if (topicRandomBound != -1) {
            return topicRandomBound;
        }
        int appRandomBound = doGetAppRetryRandomBound(app);
        if (appRandomBound != -1) {
            return appRandomBound;
        }
        return PropertySupplier.getValue(brokerContext.getPropertySupplier(), ConsumeConfigKey.RETRY_RANDOM_BOUND);
    }

    protected int doGetTopicRetryRandomBound(String topic) {
        return PropertySupplier.getValue(brokerContext.getPropertySupplier(),
                ConsumeConfigKey.RETRY_RANDOM_BOUND_TOPIC_PREFIX.getName() + topic,
                ConsumeConfigKey.RETRY_RANDOM_BOUND_TOPIC_PREFIX.getType(),
                ConsumeConfigKey.RETRY_RANDOM_BOUND_TOPIC_PREFIX.getValue());
    }

    protected int doGetAppRetryRandomBound(String app) {
        return PropertySupplier.getValue(brokerContext.getPropertySupplier(),
                ConsumeConfigKey.RETRY_RANDOM_BOUND_APP_PREFIX.getName() + app,
                ConsumeConfigKey.RETRY_RANDOM_BOUND_APP_PREFIX.getType(),
                ConsumeConfigKey.RETRY_RANDOM_BOUND_APP_PREFIX.getValue());
    }

    /**
     * 获取消息信息
     *
     * @param topic
     * @param app
     * @return
     */
    public Consumer getConsumer(TopicName topic, String app) throws JoyQueueException {
        Consumer consumer = tryGetConsumer(topic, app);
        if (null == consumer) {
            throw new JoyQueueException(JoyQueueCode.FW_CONSUMER_NOT_EXISTS);
        }
        return consumer;
    }

    public Consumer tryGetConsumer(TopicName topic, String app) {
        return localCache.getConsumerByTopicAndApp(topic, app);
    }

    /**
     * 获取消费策略
     *
     * @param topic
     * @param app
     * @return
     */
    public Consumer.ConsumerPolicy tryGetConsumerPolicy(TopicName topic, String app) {
        Consumer consumer = localCache.getConsumerByTopicAndApp(topic, app);
        if (consumer == null) {
            if (StringUtils.equals(brokerConfig.getAdminUser(), app)) {
                return brokerContext.getConsumerPolicy();
            }
            return null;
        }
        return getConsumerPolicyOrDefault(consumer);
    }

    /**
     * 获取生产策略
     *
     * @param topic
     * @param app
     * @return
     */
    public Producer.ProducerPolicy tryGetProducerPolicy(TopicName topic, String app) {
        Producer producer = localCache.getProducerByTopicAndApp(topic, app);
        if (producer == null) {
            if (StringUtils.equals(brokerConfig.getAdminUser(), app)) {
                return brokerContext.getProducerPolicy();
            }
            return null;
        }
        return getProducerPolicyOrDefault(producer);
    }


    /**
     * 是否需要长轮询
     *
     * @param topic 主题
     * @return 是否长轮询
     */
    public boolean isNeedLongPull(String topic) {
        if (topic == null) {
            return false;
        }

        // 顺序消息不支持长轮询
        TopicConfig config = localCache.getTopicConfigCache().get(topic);
        return !(config != null && config.checkSequential());
    }

    public Producer getProducer(TopicName topic, String app) throws JoyQueueException {
        Producer producer = tryGetProducer(topic, app);
        if (null == producer) {
            throw new JoyQueueException(JoyQueueCode.FW_PRODUCER_NOT_EXISTS);
        }
        return producer;
    }

    public Producer tryGetProducer(TopicName topic, String app) {
        return localCache.getProducerByTopicAndApp(topic, app);
    }

    /**
     * 获取生产策略
     *
     * @param topic
     * @param app
     * @return
     */
    public Producer.ProducerPolicy getProducerPolicy(TopicName topic, String app) throws JoyQueueException {
        Producer producer = localCache.getProducerByTopicAndApp(topic, app);
        if (null == producer) {
            if (StringUtils.equals(brokerConfig.getAdminUser(), app)) {
                return brokerContext.getProducerPolicy();
            }
            throw new JoyQueueException(JoyQueueCode.FW_PRODUCER_NOT_EXISTS);
        }
        return getProducerPolicyOrDefault(producer);
    }

    private Producer.ProducerPolicy getProducerPolicyOrDefault(Producer producer) {
        Producer.ProducerPolicy producerPolicy = producer.getProducerPolicy();
        if (producerPolicy == null) {
            return brokerContext.getProducerPolicy();
        }
        return producer.getProducerPolicy();
    }

    private Consumer.ConsumerPolicy getConsumerPolicyOrDefault(Consumer consumer) {
        Consumer.ConsumerPolicy consumerPolicy = consumer.getConsumerPolicy();
        if (consumerPolicy == null) {
            return brokerContext.getConsumerPolicy();
        }
        return consumer.getConsumerPolicy();
    }

    public boolean isLeader(String topic, short partition) {
        return isLeader(TopicName.parse(topic), partition);
    }

    public boolean isLeader(String topic, int partitionGroup) {
        return isLeader(TopicName.parse(topic), partitionGroup);
    }


    public boolean isLeader(TopicName topic, int partitionGroupId) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            return false;
        }
        PartitionGroup partitionGroup = topicConfig.fetchPartitionGroupByGroup(partitionGroupId);
        return (partitionGroup != null && isLeader(partitionGroup));
    }

    public boolean isLeader(TopicName topic, short partition) {
        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            return false;
        }
        PartitionGroup partitionGroup = topicConfig.fetchPartitionGroupByPartition(partition);
        return (partitionGroup != null && isLeader(partitionGroup));
    }

    public boolean isLeader(PartitionGroup partitionGroup) {
        if (!partitionGroup.getReplicas().contains(getBrokerId())) {
            return false;
        }
        if (!config.getTopicLocalElectionEnable() || partitionGroup.getElectType().equals(PartitionGroup.ElectType.fix)) {
            return getBrokerId().equals(partitionGroup.getLeader());
        }
        ClusterNode clusterNode = clusterNameService.getTopicGroupNode(partitionGroup.getTopic(), partitionGroup.getGroup());
        if (clusterNode == null) {
            return false;
        }
        return (clusterNode.getLeader() == getBrokerId());
    }

    /**
     * 检查是否能生产数据
     * <br>
     * <ui>是否有PartitionGroup是master</ui>
     * <ui>有主题</ui>
     * <ui>有应用</ui>
     * <ui>是否在黑名单</ui>
     *
     * @param topic 主题
     * @param app   应用
     * @return
     */
    public BooleanResponse checkWritable(TopicName topic, String app, String address) {
        // 检查broker写权限
        BooleanResponse brokerWritable = checkBrokerWritable();
        if (!brokerWritable.isSuccess()) {
            // 当前broker没有写权限
            return brokerWritable;
        }

        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            // 没有主题配置
            logger.error("topic[{}] app[{}] cant't be write on broker [{}],has no topicConfig", topic, app, broker.getId() + "[" + broker.getIp() + ":" + broker.getPort() + "]");
            return BooleanResponse.failed(JoyQueueCode.FW_TOPIC_NOT_EXIST);
        }
        Producer.ProducerPolicy producerPolicy = null;
        try {
            producerPolicy = getProducerPolicy(topic, app);
        } catch (JoyQueueException e) {
            logger.error("topic[{}],app[{}],error[{}]", topic, app, e.getMessage());
            return BooleanResponse.failed(JoyQueueCode.valueOf(e.getCode()));
        }
        Set<String> blackList = producerPolicy != null ? producerPolicy.getBlackList() : null;
        if (blackList != null) {
            if (blackList.stream().anyMatch(ip -> ip.trim().equals(address))) {
                // 是否在生产黑名单内
                logger.error("topic[{}] app[{}] cant't be write on broker [] in blacklist", topic, app, broker.getId() + "[" + broker.getIp() + ":" + broker.getPort() + "]");
                return BooleanResponse.failed(JoyQueueCode.FW_PUT_MESSAGE_TOPIC_NOT_WRITE);
            }
        }
        Collection<PartitionGroup> partitionGroups = topicConfig.fetchTopicPartitionGroupsByBrokerId(broker.getId());
        if (partitionGroups.stream().noneMatch(partitionGroup -> isLeader(partitionGroup))) {
            logger.error("topic[{}] cant't be write on broker [] ", topic, app, broker.getId() + "[" + broker.getIp() + ":" + broker.getPort() + "]");
            return BooleanResponse.failed(JoyQueueCode.FW_PRODUCE_MESSAGE_BROKER_NOT_LEADER);
        }
        return BooleanResponse.success();
    }

    /**
     * 检查是否能生产数据
     * <br>
     * <ui>是否有PartitionGroup是master</ui>
     * <ui>有主题</ui>
     * <ui>有应用</ui>
     * <ui>是否在黑名单</ui>
     *
     * @param topic 主题
     * @param app   应用
     * @return
     * @Param address ip
     * @Param partition
     */
    public BooleanResponse checkWritable(TopicName topic, String app, String address, short partition) {
        BooleanResponse response = checkWritable(topic, app, address);
        if (!response.isSuccess()) {
            return response;
        }
        TopicConfig topicConfig = getTopicConfig(topic);
        PartitionGroup group = topicConfig.fetchPartitionGroupByPartition(partition);
        if (group == null || !isLeader(group)) {
            logger.error("topic[{}],app[{}],partition[{}],error[{}]", topic, app,partition, JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_BROKER_NOT_LEADER.getMessage());
            return BooleanResponse.failed(JoyQueueCode.FW_PRODUCE_MESSAGE_BROKER_NOT_LEADER);
        }
        return BooleanResponse.success();
    }

    /**
     * 检查是否能消费数据
     * <br>
     * <ui>是否有PartitionGroup是master</ui>
     * <ui>有主题</ui>
     * <ui>有应用</ui>
     * <ui>是否停止消费</ui>
     * <ui>是否在黑名单</ui>
     *
     * @param topic 主题
     * @param app   应用
     * @return 是否可读
     */
    public BooleanResponse checkReadable(TopicName topic, String app, String address) {
        // 检查broker读权限
        BooleanResponse brokerReadable = checkBrokerReadable();
        if (!brokerReadable.isSuccess()) {
            // 没有读权限
            return brokerReadable;
        }

        TopicConfig topicConfig = getTopicConfig(topic);
        if (topicConfig == null) {
            // 没有主题配置
            return BooleanResponse.failed(JoyQueueCode.FW_TOPIC_NOT_EXIST);
        }
        Consumer.ConsumerPolicy consumerPolicy = null;
        try {
            consumerPolicy = getConsumerPolicy(topic, app);
        } catch (JoyQueueException e) {
            logger.error("topic[{}],app[{}],error[{}]", topic, app, e.getMessage());
            return BooleanResponse.failed(JoyQueueCode.valueOf(e.getCode()));
        }
        Boolean paused = consumerPolicy.getPaused();
        if (paused) {
            // 暂停消费
            logger.info("topic is paused, topic: {}, app: {}", topic, app);
            return BooleanResponse.failed(JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_PAUSED);
        }
        Set<String> blackList = consumerPolicy.getBlackList();
        if (blackList != null) {
            // 是否在消费黑名单内
            if (blackList.stream().anyMatch(ip -> ip.trim().equals(address))) {
                logger.info("app client ip not readable, topic: {}, app: {}, ip: {}", topic, app, address);
                return BooleanResponse.failed(JoyQueueCode.FW_GET_MESSAGE_APP_CLIENT_IP_NOT_READ);
            }
        }
        Collection<PartitionGroup> partitionGroups = topicConfig.fetchTopicPartitionGroupsByBrokerId(broker.getId());
        // 当前主题在该broker上有角色是master的分区组
        if (partitionGroups.stream().noneMatch(partitionGroup -> isLeader(partitionGroup))) {
            logger.error("topic[{}],app[{}],error[{}]", topic, app, JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_BROKER_NOT_LEADER.getMessage());
            return BooleanResponse.failed(JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_BROKER_NOT_LEADER);
        }
        return BooleanResponse.success();
    }

    /**
     * 检查是否能消费数据
     * <br>
     * <ui>是否有PartitionGroup是master</ui>
     * <ui>有主题</ui>
     * <ui>有应用</ui>
     * <ui>是否停止消费</ui>
     * <ui>是否在黑名单</ui>
     *
     * @param topic 主题
     * @param app   应用
     * @return 是否可读
     */
    public BooleanResponse checkReadable(TopicName topic, String app, String address, short partition) {
        BooleanResponse response = checkReadable(topic, app, address);
        if (!response.isSuccess()) {
            return response;
        }
        TopicConfig topicConfig = getTopicConfig(topic);
        PartitionGroup group = topicConfig.fetchPartitionGroupByPartition(partition);
        if (group == null || !isLeader(group)) {
            logger.error("topic[{}],app[{}],partition[{}],error[{}]", topic, app,partition, JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_BROKER_NOT_LEADER.getMessage());
            return BooleanResponse.failed(JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_BROKER_NOT_LEADER);
        }
        return BooleanResponse.success();
    }

    /**
     * 检查broker级别是否读
     *
     * @return broker是否可读
     */
    public BooleanResponse checkBrokerReadable(){
        Broker broker = getBroker();
        if (Broker.PermissionEnum.FULL != broker.getPermission() && Broker.PermissionEnum.READ != broker.getPermission()) {
            logger.error("No read permission broker:[{}]", broker);

            return BooleanResponse.failed(JoyQueueCode.FW_BROKER_NOT_READABLE);
        }
        return BooleanResponse.success();
    }

    /**
     * 检查broker级别是否写
     *
     * @return broker是否可写
     */
    public BooleanResponse checkBrokerWritable(){
        Broker broker = getBroker();
        if (Broker.PermissionEnum.FULL != broker.getPermission() && Broker.PermissionEnum.WRITE != broker.getPermission()) {
            logger.error("No write permission broker info is [{}]", broker);

            return BooleanResponse.failed(JoyQueueCode.FW_BROKER_NOT_WRITABLE);
        }
        return BooleanResponse.success();
    }

    /**
     * 根据主题获取应用列表
     *
     * @param topic 主题
     * @return
     */
    public List<String> getAppByTopic(TopicName topic) {
        List<Consumer> consumerByTopic = nameService.getConsumerByTopic(topic);
        if (CollectionUtils.isEmpty(consumerByTopic)) {
            return new ArrayList<>(0);
        }
        List<String> appList = new ArrayList<>(consumerByTopic.size());
        consumerByTopic.stream().forEach(consume -> appList.add(consume.getApp()));

        return appList;
    }

    /**
     * 获取订阅了当前broker上topic的app
     *
     * @param topic
     * @return
     */
    public List<String> getLocalSubscribeAppByTopic(TopicName topic) {
        List<String> result = Collections.emptyList();

        if (topic != null) {
            Map<String, MetaDataLocalCache.CacheConsumer> consumers = localCache.getTopicConsumers(topic);
            if (consumers != null) {
                result = new ArrayList<>(consumers.keySet());
            }
        }

        return result;
    }

    public List<Producer> getLocalProducersByTopic(TopicName topic) {
        return Lists.newArrayList(nameService.getProducerByTopic(topic));
    }

    public List<Consumer> getLocalConsumersByTopic(TopicName topic) {
        return Lists.newArrayList(nameService.getConsumerByTopic(topic));
    }

    public AppToken getAppToken(String app, String token) {
        return nameService.getAppToken(app, token);
    }

    public boolean checkArchiveable(TopicName topicName) {
        Map<String, MetaDataLocalCache.CacheProducer> producers = localCache.getTopicProducers(topicName);
        if (null != producers && producers.size() > 0) {
            for (Iterator<MetaDataLocalCache.CacheProducer> it = producers.values().iterator(); it.hasNext(); ) {
                if (getProducerPolicyOrDefault(it.next().getProducer()).getArchive()) {
                    return true;
                }
            }
        }
        Map<String, MetaDataLocalCache.CacheConsumer> consumers = localCache.getTopicConsumers(topicName);
        if (null != consumers && consumers.size() > 0) {
            for (Iterator<MetaDataLocalCache.CacheConsumer> it = consumers.values().iterator(); it.hasNext(); ) {
                if (getConsumerPolicyOrDefault(it.next().getConsumer()).getArchive()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取可以直连的重试broker
     *
     * @return
     */
    public List<Broker> getLocalRetryBroker() {
        List<Broker> brokers = nameService.getAllBrokers();
        List<Broker> localRetryBrokers = Lists.newLinkedList();
        if (brokers != null) {
            for (Broker broker : brokers) {
                if (!Broker.DEFAULT_RETRY_TYPE.equals(broker.getRetryType())) {
                    localRetryBrokers.add(broker);
                }
            }
        }
        return localRetryBrokers;
    }


    /**
     * 上报选举结果
     *
     * @param topic
     * @param partitionGroup
     * @param leaderBrokerId
     * @param isrId
     * @param termId
     */
    public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, Integer termId) {
        nameService.leaderReport(topic, partitionGroup, leaderBrokerId, isrId, termId);
    }

    public boolean hasSubscribe(String subscribeApp, Subscription.Type subscribe) {
        return nameService.hasSubscribe(subscribeApp, subscribe);
    }

    private void writeBroker(Integer brokerId) throws Exception {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(brokerIdFile))) {
            bufferedWriter.write(brokerId.toString());
        }
    }

    private Integer readBroker() throws Exception {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(brokerIdFile))) {
            String brokerId = bufferedReader.readLine();
            if (null != brokerId && !"".equals(brokerId.trim())) {
                return Integer.valueOf(brokerId);
            }
            return null;
        }
    }

    public boolean doAuthorization(String app, String token) {
        Date now = Calendar.getInstance().getTime();
        AppToken appToken = nameService.getAppToken(app, token);
        return null != appToken && appToken.getEffectiveTime().before(now) && appToken.getExpirationTime().after(now);
    }

    public TopicConfig rebuildTopicConfigCache(TopicName topicName) {
        return localCache.buildTopicConfigCache(topicName);
    }

    public NameService getNameService() {
        return nameService;
    }

    /**
     * 元数据本地缓存
     * 1.本broker上所有的topic配置
     * 2.broker.id
     * <p>
     * Created by chengzhiliang on 2018/9/4.
     */
    private class MetaDataLocalCache implements LifeCycle {
        private AtomicBoolean start = new AtomicBoolean(false);
        private final NameService nameService;
        private ScheduledExecutorService timerUpdateAllExecutor;
        private long cacheTime = 60 * 1000;

        MetaDataLocalCache(NameService nameService) {
            this.nameService = nameService;
            nameService.addListener(new MetaDataListener());
        }

        // K=主题，V=主题配置; 缓存当前broker上的所有主题配置信息
        private ConcurrentMap<String, TopicConfig> topicConfigCache = new ConcurrentHashMap<>();

        // K=主题+应用, V=消费者; 缓存当前broker上接入者->消费者
        private ConcurrentMap<String, ConcurrentHashMap<String, CacheConsumer>> consumerCache = new ConcurrentHashMap<>();
        // K=主题+应用, V=生产者; 缓存当前broker上接入者->生产者
        private ConcurrentMap<String, ConcurrentHashMap<String, CacheProducer>> producerCache = new ConcurrentHashMap<>();

        private Cache<TopicConfig, List<Short>> partitionCache =
                CacheBuilder.newBuilder()
                        .expireAfterWrite(1000 * 5, TimeUnit.MILLISECONDS)
                        .build();

        /**
         * 刷新缓存
         */
        protected void initCache() {
            buildTopicConfigCaches();
        }

        /**
         * 构建主题对于主题配置的K-V结构
         */
        protected void buildTopicConfigCaches() {
            Map<TopicName, TopicConfig> topicConfigByBroker = nameService.getTopicConfigByBroker(brokerConfig.getBrokerId());
            if (null != topicConfigByBroker) {
                for (Map.Entry<TopicName, TopicConfig> entry : topicConfigByBroker.entrySet()) {
                    logger.info("build topic config, topic: {}", entry.getKey());
                    buildTopicConfigCache(entry.getValue());
                }
            }
        }

        protected TopicConfig getTopicConfig(TopicName topic) {
            TopicConfig topicConfig = topicConfigCache.get(topic.getFullName());
            if (null != topicConfig) {
                return topicConfig;
            }
            return buildTopicConfigCache(topic);
        }

        /**
         * 根据主题更新主题配置
         *
         * @param topic 主题
         */
        protected TopicConfig buildTopicConfigCache(TopicName topic) {
            TopicConfig topicConfig = nameService.getTopicConfig(topic);
            if (null != topicConfig) {
                return buildTopicConfigCache(topicConfig);
            }
            return null;
        }
        private TopicConfig buildTopicConfigCache(TopicConfig topicConfig) {
            logger.info("build topic cache, topic: {}", topicConfig.getName());
            TopicName topic = topicConfig.getName();
            topicConfigCache.put(topic.getFullName(), topicConfig);
            if(!consumerCache.containsKey(topic.getFullName())){
                consumerCache.put(topic.getFullName(),new ConcurrentHashMap<>());
            }
            if(!producerCache.containsKey(topic.getFullName())){
                producerCache.put(topic.getFullName(),new ConcurrentHashMap<>());
            }
            return topicConfig;
        }

        /**
         * 按接入对象更新消费策略
         *
         * @param topic 主题
         * @param app   应用
         */

        protected Consumer buildConsumeCache(TopicName topic, String app) {
            logger.info("build consumer cache, topic: {}, app: {}", topic, app);
            Consumer consumerByTopic = nameService.getConsumerByTopicAndApp(topic, app);
            if (null != consumerByTopic) {
                consumerCache.get(topic.getFullName()).put(app, new CacheConsumer(consumerByTopic, SystemClock.now()));
            }
            return consumerByTopic;
        }

        protected Consumer buildConsumeCache(Consumer consumer) {
            logger.info("build consumer cache, topic: {}, app: {}", consumer.getTopic(), consumer.getApp());
            ConcurrentHashMap<String, CacheConsumer> consumers = consumerCache.get(consumer.getTopic().getFullName());
            if (consumers == null) {
                consumers = new ConcurrentHashMap<>();
                consumerCache.putIfAbsent(consumer.getTopic().getFullName(), consumers);
                consumers = consumerCache.get(consumer.getTopic().getFullName());
            }
            consumers.put(consumer.getApp(), new CacheConsumer(consumer, SystemClock.now()));
            return consumer;
        }

        protected Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
            if (!consumerCache.containsKey(topic.getFullName())) {
                logger.warn("topic {} does not exist on this broker", topic.getFullName());
                return null;
            }
            CacheConsumer consumer = consumerCache.get(topic.getFullName()).get(app);
            if (null == consumer) {
                if (topicConfigCache.containsKey(topic.getFullName())) {
                    return buildConsumeCache(topic, app);
                }
            }
            return consumer.getConsumer();
        }

        protected Producer getProducerByTopicAndApp(TopicName topic, String app) {
            if (!producerCache.containsKey(topic.getFullName())) {
                logger.warn("topic {} does not exist on this broker", topic.getFullName());
                return null;
            }
            CacheProducer producer = producerCache.get(topic.getFullName()).get(app);
            if (null == producer) {
                if (topicConfigCache.containsKey(topic.getFullName())) {
                    return buildProduceCache(topic, app);
                }
            }
            return producer.getProducer();
        }

        /**
         * 按接入对象更新生产策略
         *
         * @param topic 主题
         * @param app   应用
         */
        protected Producer buildProduceCache(TopicName topic, String app) {
            logger.info("build producer cache, topic: {}, app: {}", topic, app);
            Producer producerByTopic = nameService.getProducerByTopicAndApp(topic, app);
            if (null != producerByTopic) {
                producerCache.get(topic.getFullName()).put(app, new CacheProducer(producerByTopic));
            }
            return producerByTopic;
        }

        protected Producer buildProduceCache(Producer producer) {
            logger.info("build producer cache, topic: {}, app: {}", producer.getTopic(), producer.getApp());
            ConcurrentHashMap<String, CacheProducer> producers = producerCache.get(producer.getTopic().getFullName());
            if (producers == null) {
                producers = new ConcurrentHashMap<>();
                producerCache.putIfAbsent(producer.getTopic().getFullName(), producers);
                producers = producerCache.get(producer.getTopic().getFullName());
            }
            producers.put(producer.getApp(), new CacheProducer(producer));
            return producer;
        }


        public ConcurrentMap<String, TopicConfig> getTopicConfigCache() {
            return topicConfigCache;
        }

        public Map<String, CacheConsumer> getTopicConsumers(TopicName topic) {
            return consumerCache.get(topic.getFullName());
        }

        public Map<String, CacheProducer> getTopicProducers(TopicName topic) {
            return producerCache.get(topic.getFullName());
        }

        public Cache<TopicConfig, List<Short>> getPartitionCache() {
            return partitionCache;
        }

        @Deprecated
        private void clearConsumerCache(TopicName topicName) {
            Map<String, CacheConsumer> cacheConsuemerMap = consumerCache.get(topicName.getFullName());
            cacheConsuemerMap.values().forEach(cacheConsuemer -> {
                if (SystemClock.now() > cacheConsuemer.getExpireTime()) {
                    cacheConsuemerMap.remove(cacheConsuemer.getConsumer().getApp());
                }
            });
        }
        /**
         * 更新消费者配置信息
         * </br>
         * 用本地缓存去查询远程nameserver，如果查到覆盖本地，查不到删除本地
         * @param topicName
         */
        private void updateConsumerCache(TopicName topicName) {
            Map<String, CacheConsumer> cacheConsumerMapOld = consumerCache.get(topicName.getFullName());
            Iterator<Map.Entry<String, CacheConsumer>> iterator = cacheConsumerMapOld.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<String, CacheConsumer> next = iterator.next();
                String app = next.getKey();
                Consumer consumerByTopicAndApp = nameService.getConsumerByTopicAndApp(topicName, app);
                if (null != consumerByTopicAndApp) {
                    cacheConsumerMapOld.put(app, new CacheConsumer(consumerByTopicAndApp));
                } else {
                    iterator.remove();
                }
            }
        }

        /**
         * 补偿生产
         * @param topicName
         */
        private void compensateAddProduceEvent(TopicName topicName) {
            List<Producer> producerByTopic = nameService.getProducerByTopic(topicName);
            ConcurrentHashMap<String, CacheProducer> localProducerCache = producerCache.get(topicName.getFullName());
            producerByTopic.stream().forEach(producer -> {
                if (producer != null) {
                    String app = producer.getApp();
                    if (!localProducerCache.contains(app)) {
                        // 如果本地缓存不包含这个订阅关系，则补偿一个订阅关系事件
                        ProducerEvent producerEvent = new ProducerEvent();
                        producerEvent.setEventType(EventType.ADD_PRODUCER);
                        producerEvent.setApp(app);
                        producerEvent.setTopic(topicName);

                        // 添加订阅事件
                        eventBus.add(producerEvent);
                    }

                    // 添加到本地缓存
                    localProducerCache.put(app, new CacheProducer(producer));
                }
            });
        }

        /**
         * 补偿订阅事件
         * </br>
         * 远程拉取主题的所有订阅者和本地缓存对比，如果本地缓存没有，则补偿一个订阅事件
         * @param topicName
         */
        private void compensateAddConsumeEvent(TopicName topicName) {
            List<Consumer> consumerByTopic = nameService.getConsumerByTopic(topicName);
            ConcurrentHashMap<String, CacheConsumer> localConsumeCache = consumerCache.get(topicName.getFullName());
            consumerByTopic.stream().forEach(consumer -> {
                if (consumer != null) {
                    String app = consumer.getApp();
                    if (!localConsumeCache.contains(app)) {
                        // 如果本地缓存不包含这个订阅关系，则补偿一个订阅关系事件
                        ConsumerEvent consumerEvent = new ConsumerEvent();
                        consumerEvent.setEventType(EventType.ADD_CONSUMER);
                        consumerEvent.setApp(app);
                        consumerEvent.setTopic(topicName);

                        // 添加订阅事件
                        eventBus.add(consumerEvent);
                    }

                    // 添加到本地缓存
                    localConsumeCache.put(app, new CacheConsumer(consumer));
                }
            });
        }

        @Deprecated
        private void clearProducerCache(TopicName topicName) {
            Map<String, CacheProducer> cacheProducerMap = producerCache.get(topicName.getFullName());
            cacheProducerMap.values().forEach(cacheProducer -> {
                if (SystemClock.now() > cacheProducer.getExpireTime()) {
                    cacheProducerMap.remove(cacheProducer.getProducer().getApp());
                }
            });
        }

        /**
         * 更新发送者配置信息
         * </br>
         * 用本地缓存去查询远程nameserver，如果查到覆盖本地，查不到删除本地
         * @param topicName
         */
        private void updateProducerCache(TopicName topicName) {
            Map<String, CacheProducer> cacheProducerMapOld = producerCache.get(topicName.getFullName());

            Iterator<Map.Entry<String, CacheProducer>> iterator = cacheProducerMapOld.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<String, CacheProducer> next = iterator.next();
                String app = next.getKey();
                Producer producerByTopic = nameService.getProducerByTopicAndApp(topicName, app);
                if (null != producerByTopic) {
                    cacheProducerMapOld.put(app, new CacheProducer(producerByTopic));
                } else {
                    iterator.remove();
                }
            }
        }

        @Override
        public void start() throws Exception {
            timerUpdateAllExecutor = Executors.newSingleThreadScheduledExecutor();
            start.compareAndSet(false, true);
        }

        @Override
        public void stop() {
            timerUpdateAllExecutor.shutdown();
            start.compareAndSet(true, false);
        }

        @Override
        public boolean isStarted() {
            return start.get();
        }

        protected void publishEvent(MetaEvent event) {
            eventBus.inform(event);
        }

        /**
         * 主题信息更新事件处理
         */
        class MetaDataListener implements EventListener<NameServerEvent> {

            @Override
            public void onEvent(NameServerEvent event) {
                if (logger.isDebugEnabled()) {
                    logger.debug("onEvent, event: {}", JSON.toJSONString(event));
                }

                switch (event.getEventType()) {
                    case ADD_TOPIC: {
                        AddTopicEvent addTopicEvent = (AddTopicEvent) event.getMetaEvent();
                        TopicConfig topicConfig = TopicConfig.toTopicConfig(addTopicEvent.getTopic(), addTopicEvent.getPartitionGroups());
                        buildTopicConfigCache(topicConfig);
                        break;
                    }
                    case UPDATE_TOPIC: {
                        UpdateTopicEvent updateTopicEvent = (UpdateTopicEvent) event.getMetaEvent();
                        TopicConfig oldTopicConfig = topicConfigCache.get(updateTopicEvent.getOldTopic().getName().getFullName());
                        if (oldTopicConfig == null) {
                            break;
                        }
                        TopicConfig newTopicConfig = TopicConfig.toTopicConfig(updateTopicEvent.getNewTopic());
                        newTopicConfig.setPartitionGroups(oldTopicConfig.getPartitionGroups());
                        buildTopicConfigCache(newTopicConfig);
                        break;
                    }
                    case REMOVE_TOPIC: {
                        RemoveTopicEvent removeTopicEvent = (RemoveTopicEvent) event.getMetaEvent();
                        TopicConfig topicConfig = topicConfigCache.remove(removeTopicEvent.getTopic().getName().getFullName());
                        if (topicConfig == null) {
                            break;
                        }
                        consumerCache.remove(topicConfig.getName().getFullName());
                        producerCache.remove(topicConfig.getName().getFullName());
                        for (PartitionGroup partitionGroup : topicConfig.fetchTopicPartitionGroupsByBrokerId(brokerConfig.getBrokerId())) {
                            publishEvent(new RemovePartitionGroupEvent(topicConfig.getName(), partitionGroup));
                        }
                        break;
                    }
                    case ADD_PARTITION_GROUP: {
                        AddPartitionGroupEvent addPartitionGroupEvent = (AddPartitionGroupEvent) event.getMetaEvent();
                        PartitionGroup partitionGroup = addPartitionGroupEvent.getPartitionGroup();
                        TopicConfig oldTopicConfig = topicConfigCache.get(addPartitionGroupEvent.getTopic().getFullName());
                        if (oldTopicConfig == null) {
                            break;
                        }

                        Map<Integer, PartitionGroup> topicPartitionGroups = Maps.newHashMap(oldTopicConfig.getPartitionGroups());
                        topicPartitionGroups.put(addPartitionGroupEvent.getPartitionGroup().getGroup(), partitionGroup);
                        oldTopicConfig.setPartitionGroups(topicPartitionGroups);
                        buildTopicConfigCache(oldTopicConfig);
                        break;
                    }
                    case UPDATE_PARTITION_GROUP: {
                        UpdatePartitionGroupEvent updatePartitionGroupEvent = (UpdatePartitionGroupEvent) event.getMetaEvent();
                        PartitionGroup oldPartitionGroup = updatePartitionGroupEvent.getOldPartitionGroup();
                        PartitionGroup newPartitionGroup = updatePartitionGroupEvent.getNewPartitionGroup();
                        TopicConfig oldTopicConfig = topicConfigCache.get(updatePartitionGroupEvent.getTopic().getFullName());
                        if (oldTopicConfig == null) {
                            break;
                        }

                        Map<Integer, PartitionGroup> topicPartitionGroups = Maps.newHashMap(oldTopicConfig.getPartitionGroups());
                        topicPartitionGroups.put(newPartitionGroup.getGroup(), newPartitionGroup);
                        oldTopicConfig.setPartitionGroups(topicPartitionGroups);
                        buildTopicConfigCache(oldTopicConfig);
                        break;
                    }
                    case REMOVE_PARTITION_GROUP: {
                        RemovePartitionGroupEvent removePartitionGroupEvent = (RemovePartitionGroupEvent) event.getMetaEvent();
                        PartitionGroup partitionGroup = removePartitionGroupEvent.getPartitionGroup();
                        TopicConfig oldTopicConfig = topicConfigCache.get(removePartitionGroupEvent.getTopic().getFullName());
                        if (oldTopicConfig == null) {
                            break;
                        }

                        Map<Integer, PartitionGroup> topicPartitionGroups = Maps.newHashMap(oldTopicConfig.getPartitionGroups());
                        topicPartitionGroups.remove(partitionGroup.getGroup());
                        oldTopicConfig.setPartitionGroups(topicPartitionGroups);
                        TopicConfig newTopiConfig =  buildTopicConfigCache(oldTopicConfig);

                        if (CollectionUtils.isEmpty(newTopiConfig.fetchTopicPartitionGroupsByBrokerId(getBrokerId()))) {
                            consumerCache.remove(newTopiConfig.getName().getFullName());
                            producerCache.remove(newTopiConfig.getName().getFullName());
                        }

                        break;
                    }
                    case ADD_CONSUMER: {
                        AddConsumerEvent addConsumerEvent = (AddConsumerEvent) event.getMetaEvent();
                        buildConsumeCache(addConsumerEvent.getConsumer());
                        break;
                    }
                    case UPDATE_CONSUMER: {
                        UpdateConsumerEvent updateConsumerEvent = (UpdateConsumerEvent) event.getMetaEvent();
                        buildConsumeCache(updateConsumerEvent.getNewConsumer());
                        break;
                    }
                    case REMOVE_CONSUMER: {
                        RemoveConsumerEvent removeConsumerEvent = (RemoveConsumerEvent) event.getMetaEvent();
                        ConcurrentHashMap<String, CacheConsumer> topicConsumerCache = consumerCache.get(removeConsumerEvent.getTopic().getFullName());
                        if (topicConsumerCache != null) {
                            topicConsumerCache.remove(removeConsumerEvent.getConsumer().getApp());
                        }
                        break;
                    }
                    case ADD_PRODUCER: {
                        AddProducerEvent addProducerEvent = (AddProducerEvent) event.getMetaEvent();
                        buildProduceCache(addProducerEvent.getProducer());
                        break;
                    }
                    case UPDATE_PRODUCER: {
                        UpdateProducerEvent updateProducerEvent = (UpdateProducerEvent) event.getMetaEvent();
                        buildProduceCache(updateProducerEvent.getNewProducer());
                        break;
                    }
                    case REMOVE_PRODUCER: {
                        RemoveProducerEvent removeProducerEvent = (RemoveProducerEvent) event.getMetaEvent();
                        ConcurrentHashMap<String, CacheProducer> topicProducerCache = producerCache.get(removeProducerEvent.getTopic().getFullName());
                        if (topicProducerCache != null) {
                            topicProducerCache.remove(removeProducerEvent.getProducer().getApp());
                        }
                        break;
                    }
                    case UPDATE_BROKER: {
                        UpdateBrokerEvent updateBrokerEvent = (UpdateBrokerEvent) event.getMetaEvent();
                        if (broker != null) {
                            broker.setPermission(updateBrokerEvent.getNewBroker().getPermission());
                            broker.setRetryType(updateBrokerEvent.getNewBroker().getRetryType());
                        }
                        break;
                    }
                    case COMPENSATE: {
                        org.joyqueue.nsr.event.CompensateEvent compensateEvent = (org.joyqueue.nsr.event.CompensateEvent) event.getMetaEvent();
                        publishEvent(new CompensateEvent(compensateEvent.getNewCache().getTopicConfigBrokerMap().get(getBrokerId())));
                        break;
                    }
                }

                if (!event.getEventType().equals(EventType.COMPENSATE)) {
                    publishEvent(event.getMetaEvent());
                }
            }
        }

        private class CacheConsumer {
            private Consumer consumer;
            private long expireTime;


            CacheConsumer(Consumer consumer) {
                this.consumer = consumer;
                this.expireTime = SystemClock.now() + cacheTime;
            }

            CacheConsumer(Consumer consumer, long expireTime) {
                this.consumer = consumer;
                this.expireTime = expireTime;
            }

            public Consumer getConsumer() {
                return consumer;
            }

            public long getExpireTime() {
                return expireTime;
            }
        }

        private class CacheProducer {
            private Producer producer;
            private long expireTime;

            CacheProducer(Producer producer) {
                this.producer = producer;
                this.expireTime = SystemClock.now() + cacheTime;
            }

            CacheProducer(Producer producer, long expireTime) {
                this.producer = producer;
                this.expireTime = expireTime;
            }

            public Producer getProducer() {
                return producer;
            }

            public long getExpireTime() {
                return expireTime;
            }
        }
    }

    @Override
    protected void doStop() {
        super.doStop();
        if (eventBus.isStarted()){
            eventBus.stop();
        }
        if (localCache.isStarted()) {
            localCache.stop();
        }
        logger.info("clusterManager is stopped");
    }
}
