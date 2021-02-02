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
package org.joyqueue.nsr.nameservice;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jd.laf.extension.ExtensionPoint;
import com.jd.laf.extension.ExtensionPointLazy;
import org.apache.commons.lang3.RandomUtils;
import org.joyqueue.domain.*;
import org.joyqueue.event.NameServerEvent;
import org.joyqueue.nsr.ManageServer;
import org.joyqueue.nsr.MetaManager;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.ServiceProvider;
import org.joyqueue.nsr.config.NameServerConfig;
import org.joyqueue.nsr.exception.NsrException;
import org.joyqueue.nsr.service.*;
import org.joyqueue.nsr.util.DCWrapper;
import org.joyqueue.toolkit.concurrent.EventBus;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.config.PropertySupplierAware;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.lang.LifeCycle;
import org.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 1.启动nameServer
 * 2.器动管理端的http接口
 *
 * @author wylixiaobin
 * @date 2018/9/4
 */
public class NameServerInternal extends Service implements NameService, PropertySupplierAware {
    /**
     * name server config
     */
    private NameServerConfig nameServerConfig;
    /**
     * manage server
     */
    private ManageServer manageServer;
    /**
     * 元数据管理器
     */
    private MetaManager metaManager;
    /**
     * properties
     */
    private PropertySupplier propertySupplier;
    /**
     * service provider
     */
    private ServiceProvider serviceProvider;

    /**
     * 事件管理器
     */
    protected EventBus<NameServerEvent> eventManager = new EventBus<>("joyqueue-nameserver-eventBus");
    /**
     * service provider
     */
    public static ExtensionPoint<ServiceProvider, String> serviceProviderPoint = new ExtensionPointLazy<>(ServiceProvider.class);
    private static final Logger logger = LoggerFactory.getLogger(NameServerInternal.class);

    public NameServerInternal() {

    }

    @Override
    public void setSupplier(PropertySupplier supplier) {
        try {
            propertySupplier = supplier;
            if (nameServerConfig == null) {
                nameServerConfig = new NameServerConfig(propertySupplier);
            }
            if (serviceProvider == null){
                serviceProvider = loadServiceProvider(propertySupplier);
            }
            if (metaManager == null) {
                metaManager = buildMetaManager();
            }
            if (manageServer == null) {
                this.manageServer = buildManageServer();
            }
            this.manageServer.setManagerPort(nameServerConfig.getManagerPort());
            this.metaManager.start();
            this.eventManager.start();
            this.manageServer.start();
            logger.info("nameServer is started");
        } catch (Exception e) {
            throw new NsrException(e);
        }
    }

    @Override
    public void doStop() {
        super.doStop();
        try {
            Close.close(manageServer);
            Close.close(metaManager);
            Close.close(eventManager);
            if(serviceProvider instanceof LifeCycle)
            Close.close((LifeCycle )serviceProvider);
        } finally {
            logger.info("nameServer is stopped");
        }
    }

    @Override
    public List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType) {
        List<TopicConfig> configs = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            TopicConfig topicConfig = subscribe(subscription, clientType);
            if (null != topicConfig) {
                configs.add(topicConfig);
            }
        });
        return configs;
    }

    @Override
    public TopicConfig subscribe(Subscription subscription, ClientType clientType) {
        //TODO 考虑下这个
        if (subscription.getType() == Subscription.Type.CONSUMPTION) {
            TopicName topic = subscription.getTopic();
            String app = subscription.getApp();
            TopicConfig topicConfig = doGetTopicConfig(topic);
            if (null == topicConfig){
                return null;
            }
            Consumer consumer = metaManager.getConsumer(topic, app);
            if (null == consumer) {
                consumer = new Consumer();
                consumer.setTopic(topic);
                consumer.setApp(app);

                consumer.setClientType(clientType);
                consumer = metaManager.addConsumer(consumer);
            }
            return topicConfig;
        } else if (subscription.getType() == Subscription.Type.PRODUCTION) {
            TopicName topic = subscription.getTopic();
            String app = subscription.getApp();
            TopicConfig topicConfig = doGetTopicConfig(topic);
            if (null == topicConfig) return null;
            Producer producer = metaManager.getProducer(topic, app);
            if (null == producer) {
                producer = new Producer();
                producer.setTopic(topic);
                producer.setApp(app);

                producer.setClientType(clientType);
                producer = metaManager.addProducer(producer);
            }
            return topicConfig;
        } else {
            throw new IllegalStateException("operation do not supported");
        }
    }

    @Override
    public void unSubscribe(Subscription subscription) {
        if (subscription.getType() == Subscription.Type.CONSUMPTION) {
            TopicConfig topicConfig = doGetTopicConfig(subscription.getTopic());
            if (null == topicConfig){
                return;
            }
            metaManager.removeConsumer(subscription.getTopic(), subscription.getApp());
        } else if (subscription.getType() == Subscription.Type.PRODUCTION) {
            TopicConfig topicConfig = doGetTopicConfig(subscription.getTopic());
            if (null == topicConfig) return;
            metaManager.removeProducer(subscription.getTopic(), subscription.getApp());
        } else {
            throw new IllegalStateException("operation do not supported");
        }

    }

    @Override
    public void unSubscribe(List<Subscription> subscriptions) {
        subscriptions.forEach(subscription -> unSubscribe(subscription));
    }


    @Override
    public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId) {
        logger.info("Leader report, topic is {}, partition group is {}, leader is {}, term is {}",
                topic, partitionGroup, leaderBrokerId, termId);
        TopicConfig topicConfig = doGetTopicConfig(topic);
        if (topicConfig == null) {
            logger.warn("topic not exist, topic: {}, partitionGroup: {}, leaderBrokerId: {}", topic, partitionGroup, leaderBrokerId);
            return;
        }
        PartitionGroup group = null;
        Iterator<PartitionGroup> groupIterator = topicConfig.getPartitionGroups().values().iterator();
        while (groupIterator.hasNext()) {
            PartitionGroup pgroup = groupIterator.next();
            if (pgroup.getGroup() == partitionGroup) {
//                if (pgroup.getTerm() > termId || (pgroup.getTerm() == termId && leaderBrokerId == -1)) {
//                    logger.warn("Leader report for topic {} group {}, term {} less than current term {}, leaderId is {}",
//                            topic, partitionGroup, termId, pgroup.getTerm(), leaderBrokerId);
//                    return;
//                }
                group = pgroup;
                break;
            }
        }
        if (null == group) {
            throw new RuntimeException(String.format("topic[%s] group[%s] does not exist", topic, partitionGroup));
        }
        group.setIsrs(isrId);
        group.setLeader(leaderBrokerId);
        group.setTerm(termId);
        metaManager.leaderReport(group);
    }

    @Override
    public Broker getBroker(int brokerId) {
        return metaManager.getBrokerById(brokerId);
    }

    @Override
    public List<Broker> getAllBrokers() {
        return metaManager.getAllBrokers();
    }

    @Override
    public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
        metaManager.addTopic(topic, partitionGroups);
    }

    @Override
    public TopicConfig getTopicConfig(TopicName topicName) {
        return doGetTopicConfig(topicName);
    }

    protected TopicConfig doGetTopicConfig(TopicName topicName) {
        Topic topic = metaManager.getTopicByName(topicName);
        if (null == topic) {
            return null;
        }
        List<PartitionGroup> partitionGroups = metaManager.getPartitionGroupByTopic(topicName);
        TopicConfig topicConfig = TopicConfig.toTopicConfig(topic);
        topicConfig.setPartitionGroups(partitionGroups.stream().collect(Collectors.toMap(PartitionGroup::getGroup, group -> group)));
        if (null != partitionGroups) {
            partitionGroups.forEach(group -> {
                Map<Integer, Broker> brokerMap = new HashMap<>();
                group.getReplicas().forEach(brokerId -> {
                    if (!brokerMap.containsKey(brokerId)) {
                        Broker broker = getBroker(brokerId);
                        if (broker == null) {
                            throw new NsrException(String.format("broker %s not exist, topic: %s, group: {}", brokerId, topicName, group.getGroup()));
                        }
                        brokerMap.put(brokerId, broker);
                    }
                });
                group.setBrokers(brokerMap);
            });
        }
        return topicConfig;
    }

    @Override
    public Set<String> getAllTopicCodes() {
        List<Topic> allTopics = metaManager.getAllTopics();
        if (null != allTopics) {
            Set<String> topics = new HashSet<>();
            allTopics.forEach(topic -> topics.add(topic.getName().getFullName()));
            return topics;
        } else {
            return Collections.emptySet();
        }
    }

    //TODO Ignite不可用
    @Override
    public Set<String> getTopics(String app, Subscription.Type subscribe) {
        Set<String> topics = new HashSet<>();
        if (null == subscribe) {
            List<Producer> producers = metaManager.getProducer(app);
            if (null != producers){
                producers.forEach(producer -> topics.add(producer.getTopic().getFullName()));
            }
            List<Consumer> consumers = metaManager.getConsumer(app);
            if (null != consumers){
                consumers.forEach(consumer -> topics.add(consumer.getTopic().getFullName()));
            }
        } else {
            switch (subscribe) {
                case PRODUCTION:
                    List<Producer> producers = metaManager.getProducer(app);
                    if (null != producers){
                        producers.forEach(producer -> topics.add(producer.getTopic().getFullName()));
                    }
                    break;
                case CONSUMPTION:
                    List<Consumer> consumers = metaManager.getConsumer(app);
                    if (null != consumers){
                        consumers.forEach(consumer -> topics.add(consumer.getTopic().getFullName()));
                    }
                    break;
            }
        }
        return topics;
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
        Map<TopicName,TopicConfig> map = new HashMap<>();
        List<Replica> replicas = metaManager.getReplicaByBroker(brokerId);
        for(Replica replica:replicas){
            if (map.containsKey(replica.getTopic())){
                continue;
            }
            TopicConfig topicConfig = doGetTopicConfig(replica.getTopic());
            if (topicConfig == null) {
                logger.error("topic not exist, topic: {}, brokerId: {}", replica.getTopic(), brokerId);
                continue;
            }
            map.put(replica.getTopic(), doGetTopicConfig(replica.getTopic()));
        }
        return map;
    }

    @Override
    public List<Replica> getReplicaByBroker(Integer brokerId) {
        return metaManager.getReplicaByBroker(brokerId);
    }

    @Override
    public AppToken getAppToken(String app, String token) {
        return metaManager.findAppToken(app, token);
    }

    @Override
    public Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
        Broker broker;
        if (null == brokerId) {
            broker = metaManager.getBrokerByIpAndPort(brokerHost, port);
            if (null == broker) {
                broker = addNewBroker(generateBrokerId(), brokerHost, brokerIp, port, node);
            } else {
                brokerId = broker.getId();
            }
        }
        //TODO 并未去更新broker的IP
        broker = metaManager.getBrokerById(brokerId);
        if (null != broker) {
            broker.setIp(brokerHost);
            broker.setExternalIp(brokerIp);
            broker.setPort(port);
            broker.setExternalPort(port);
            broker.setDataCenter(getDataCenter(brokerIp).getCode());
            broker.setNode(node);
            metaManager.updateBroker(broker);
        } else {
            broker = addNewBroker(brokerId, brokerHost, brokerIp, port, node);
        }

        return broker;
    }

    protected Broker addNewBroker(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
        Broker broker = new Broker();
        broker.setId(brokerId);
        broker.setIp(brokerHost);
        broker.setExternalIp(brokerIp);
        broker.setPort(port);
        broker.setExternalPort(port);
        broker.setNode(node);

        broker.setDataCenter(getDataCenter(brokerIp).getCode());
        broker.setRetryType(Broker.DEFAULT_RETRY_TYPE);
        broker.setPermission(Broker.PermissionEnum.FULL);
        metaManager.addBroker(broker);
        logger.info("register broker success broker.id {}", brokerId);
        return broker;
    }

    protected int generateBrokerId() {
        int id = 0;
        while (id <= 0) {
            int result = (int) (System.nanoTime() % 1000000000);
            id = result + RandomUtils.nextInt(0, result);
        }
        return id;
    }

    @Override
    public Producer getProducerByTopicAndApp(TopicName topic, String app) {
        return metaManager.getProducer(topic, app);
    }

    @Override
    public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
        return metaManager.getConsumer(topic, app);
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByApp(String subscribeApp, Subscription.Type subscribe) {
        Map<TopicName, TopicConfig> appTopicConfigs = new HashMap<>();

        List<? extends Subscription> subscriptions = null;
        switch (subscribe) {
            case CONSUMPTION:
                subscriptions = metaManager.getConsumer(subscribeApp);
                break;
            case PRODUCTION:
                subscriptions = metaManager.getProducer(subscribeApp);
                break;
        }

        if (null != subscriptions) {
            subscriptions.forEach(p -> {
                TopicConfig topicConfig = doGetTopicConfig(p.getTopic());
                if (null != topicConfig) {
                    appTopicConfigs.put(p.getTopic(), topicConfig);
                }
            });
        }
        return appTopicConfigs;
    }

    // TODO 临时
    private Cache<String, Boolean> hasSubscribeCache = CacheBuilder.newBuilder().expireAfterWrite(1000 * 60, TimeUnit.MILLISECONDS).build();

    @Override
    public boolean hasSubscribe(String subscribeApp, Subscription.Type subscribe) {
        try {
            return hasSubscribeCache.get(subscribeApp + "_" + subscribe, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    //TODO Ignite 不可用
                    switch (subscribe) {
                        case CONSUMPTION:
                            List<Consumer> consumers = metaManager.getConsumer(subscribeApp);
                            return (null != consumers && consumers.size() > 0) ? true : false;
                        case PRODUCTION:
                            List<Producer> producers = metaManager.getProducer(subscribeApp);
                            return (null != producers && producers.size() > 0) ? true : false;
                    }
                    return false;
                }
            });
        } catch (ExecutionException e) {
            logger.error("hasSubscribe, subscribeApp: {}, subscribe: {}", subscribeApp, subscribe, e);
            return true;
        }
    }

    @Override
    public DataCenter getDataCenter(String ip) {
        Collection<DataCenter> dcs = metaManager.getAllDataCenter();
        Optional<DataCenter> optional = dcs.stream().filter(dataCenter -> new DCWrapper(dataCenter).match(ip)).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        }
        return DataCenter.DEFAULT;
    }

    @Override
    public String getConfig(String group, String key) {
        Config config = metaManager.getConfig(group, key);
        return config == null ? null : config.getValue();
    }

    @Override
    public List<Config> getAllConfigs() {
        return metaManager.getAllConfigs();
    }

    @Override
    public List<Broker> getBrokerByRetryType(String retryType) {
        return metaManager.getBrokerByRetryType(retryType);
    }

    @Override
    public List<Consumer> getConsumerByTopic(TopicName topic) {
        return metaManager.getConsumerByTopic(topic);
    }

    @Override
    public List<Producer> getProducerByTopic(TopicName topic) {
        return metaManager.getProducerByTopic(topic);
    }

    @Override
    public AllMetadata getAllMetadata() {
        Map<TopicName, TopicConfig> topicConfigs = getAllTopicConfigs();
        List<Broker> allBrokers = metaManager.getAllBrokers();
        List<Config> allConfigs = metaManager.getAllConfigs();
        List<DataCenter> allDataCenters = Lists.newArrayList(metaManager.getAllDataCenter());
        List<Consumer> allConsumers = metaManager.getAllConsumers();
        List<Producer> allProducers = metaManager.getAllProducers();
        List<AppToken> allAppTokens = metaManager.getAllAppToken();
        Map<Integer, Broker> allBrokerMap = Maps.newHashMap();

        for (Broker broker : allBrokers) {
            allBrokerMap.put(broker.getId(), broker);
        }

        for (Map.Entry<TopicName, TopicConfig> entry : topicConfigs.entrySet()) {
            TopicConfig topicConfig = entry.getValue();
            for (Map.Entry<Integer, PartitionGroup> partitionGroupEntry : topicConfig.getPartitionGroups().entrySet()) {
                PartitionGroup partitionGroup = partitionGroupEntry.getValue();
                Map<Integer, Broker> partitionGroupBrokers = Maps.newHashMap();
                for (Integer replica : partitionGroup.getReplicas()) {
                    Broker broker = allBrokerMap.get(replica);
                    if (broker != null) {
                        partitionGroupBrokers.put(replica, broker);
                    }
                }
            }
        }

        AllMetadata allMetadata = new AllMetadata();
        allMetadata.setTopics(topicConfigs);
        allMetadata.setBrokers(allBrokerMap);
        allMetadata.setProducers(allProducers);
        allMetadata.setConsumers(allConsumers);
        allMetadata.setDataCenters(allDataCenters);
        allMetadata.setConfigs(allConfigs);
        allMetadata.setAppTokens(allAppTokens);
        return allMetadata;
    }

    protected Map<TopicName, TopicConfig> getAllTopicConfigs() {
        List<Topic> topics = metaManager.getAllTopics();
        List<PartitionGroup> partitionGroups = metaManager.getAllPartitionGroups();
        Map<TopicName, Map<Integer, PartitionGroup>> partitionGroupMap = Maps.newHashMap();

        for (PartitionGroup partitionGroup : partitionGroups) {
            Map<Integer, PartitionGroup> topicPartitionGroups = partitionGroupMap.get(partitionGroup.getTopic());
            if (topicPartitionGroups == null) {
                topicPartitionGroups = Maps.newHashMap();
                partitionGroupMap.put(partitionGroup.getTopic(), topicPartitionGroups);
            }
            topicPartitionGroups.put(partitionGroup.getGroup(), partitionGroup);
        }

        Map<TopicName, TopicConfig> result = Maps.newHashMap();
        for (Topic topic : topics) {
            TopicConfig topicConfig = TopicConfig.toTopicConfig(topic);
            Map<Integer, PartitionGroup> topicPartitionGroups = partitionGroupMap.get(topic.getName());
            if (topicPartitionGroups == null) {
                topicPartitionGroups = Maps.newHashMap();
            }

            topicConfig.setPartitionGroups(topicPartitionGroups);
            result.put(topicConfig.getName(), topicConfig);
        }

        return result;
    }

    @Override
    public void addListener(EventListener<NameServerEvent> listener) {
        //TODO 是否需要全量更新一下
        eventManager.addListener(listener);
    }

    @Override
    public void removeListener(EventListener<NameServerEvent> listener) {
        eventManager.removeListener(listener);
    }

    @Override
    public void addEvent(NameServerEvent event) {
        eventManager.add(event);
    }

    private ManageServer buildManageServer() {
        TopicService topicService = serviceProvider.getService(TopicService.class);
        BrokerService brokerService = serviceProvider.getService(BrokerService.class);
        ConsumerService consumerService = serviceProvider.getService(ConsumerService.class);
        ProducerService producerService = serviceProvider.getService(ProducerService.class);
        ConfigService configService = serviceProvider.getService(ConfigService.class);
        AppTokenService appTokenService = serviceProvider.getService(AppTokenService.class);
        DataCenterService dataCenterService = serviceProvider.getService(DataCenterService.class);
        NamespaceService namespaceService = serviceProvider.getService(NamespaceService.class);
        PartitionGroupService partitionGroupService = serviceProvider.getService(PartitionGroupService.class);
        PartitionGroupReplicaService partitionGroupReplicaService = serviceProvider.getService(PartitionGroupReplicaService.class);

        Preconditions.checkArgument(brokerService != null, "broker service can not be null");
        Preconditions.checkArgument(topicService != null, "topic service can not be null");
        Preconditions.checkArgument(consumerService != null, "consumer service can not be null");
        Preconditions.checkArgument(producerService != null, "producer service can not be null");
        Preconditions.checkArgument(appTokenService != null, "appToken service can not be null");
        Preconditions.checkArgument(namespaceService != null, "namespace service can not be null");
        Preconditions.checkArgument(dataCenterService != null, "datacenter service can not be null");
        Preconditions.checkArgument(partitionGroupReplicaService != null, "replica service can not be null");
        Preconditions.checkArgument(partitionGroupService != null, "partitionGroup service can not be null");
        return new ManageServer(topicService, producerService, consumerService,
                brokerService, configService, appTokenService, dataCenterService,
                namespaceService, partitionGroupService, partitionGroupReplicaService);

    }


    private MetaManager buildMetaManager() {
        TopicService topicService = serviceProvider.getService(TopicService.class);
        ConfigService configService = serviceProvider.getService(ConfigService.class);
        BrokerService brokerService = serviceProvider.getService(BrokerService.class);
        ConsumerService consumerService = serviceProvider.getService(ConsumerService.class);
        ProducerService producerService = serviceProvider.getService(ProducerService.class);
        AppTokenService appTokenService = serviceProvider.getService(AppTokenService.class);
        DataCenterService dataCenterService = serviceProvider.getService(DataCenterService.class);
        PartitionGroupService partitionGroupService = serviceProvider.getService(PartitionGroupService.class);
        PartitionGroupReplicaService partitionGroupReplicaService = serviceProvider.getService(PartitionGroupReplicaService.class);

        return new MetaManager(configService, topicService, brokerService,
                consumerService, producerService, partitionGroupService,
                partitionGroupReplicaService, appTokenService, dataCenterService);

    }

    private ServiceProvider loadServiceProvider(PropertySupplier propertySupplier) throws Exception{
        ServiceProvider serviceProvider = serviceProviderPoint.get();
        Preconditions.checkArgument(serviceProvider != null, "service provider can not be null.");
        if (serviceProvider instanceof PropertySupplierAware) {
            ((PropertySupplierAware) serviceProvider).setSupplier(propertySupplier);
        }

        if (serviceProvider instanceof LifeCycle){
            ((LifeCycle) serviceProvider).start();
        }
        return serviceProvider;
    }

    private String createAppTokenCacheKey(String app, String token) {
        return app + "@" + token;
    }

}
