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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.jd.laf.extension.Type;
import org.apache.commons.collections.CollectionUtils;
import org.joyqueue.config.BrokerConfigKey;
import org.joyqueue.domain.AllMetadata;
import org.joyqueue.domain.AppToken;
import org.joyqueue.domain.Broker;
import org.joyqueue.domain.ClientType;
import org.joyqueue.domain.Config;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Replica;
import org.joyqueue.domain.Subscription;
import org.joyqueue.domain.Topic;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.event.NameServerEvent;
import org.joyqueue.monitor.PointTracer;
import org.joyqueue.monitor.TraceStat;
import org.joyqueue.network.command.GetTopics;
import org.joyqueue.network.command.GetTopicsAck;
import org.joyqueue.network.command.Subscribe;
import org.joyqueue.network.command.SubscribeAck;
import org.joyqueue.network.command.UnSubscribe;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.TransportClient;
import org.joyqueue.network.transport.codec.JoyQueueHeader;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.CommandCallback;
import org.joyqueue.network.transport.command.Direction;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.NsrPlugins;
import org.joyqueue.nsr.config.NameServiceConfig;
import org.joyqueue.nsr.exception.NsrException;
import org.joyqueue.nsr.network.NsrTransportClientFactory;
import org.joyqueue.nsr.network.command.AddTopic;
import org.joyqueue.nsr.network.command.GetAllBrokers;
import org.joyqueue.nsr.network.command.GetAllBrokersAck;
import org.joyqueue.nsr.network.command.GetAllConfigs;
import org.joyqueue.nsr.network.command.GetAllConfigsAck;
import org.joyqueue.nsr.network.command.GetAllMetadataRequest;
import org.joyqueue.nsr.network.command.GetAllMetadataResponse;
import org.joyqueue.nsr.network.command.GetAllTopics;
import org.joyqueue.nsr.network.command.GetAllTopicsAck;
import org.joyqueue.nsr.network.command.GetAppToken;
import org.joyqueue.nsr.network.command.GetAppTokenAck;
import org.joyqueue.nsr.network.command.GetBroker;
import org.joyqueue.nsr.network.command.GetBrokerAck;
import org.joyqueue.nsr.network.command.GetBrokerByRetryType;
import org.joyqueue.nsr.network.command.GetBrokerByRetryTypeAck;
import org.joyqueue.nsr.network.command.GetConfig;
import org.joyqueue.nsr.network.command.GetConfigAck;
import org.joyqueue.nsr.network.command.GetConsumerByTopic;
import org.joyqueue.nsr.network.command.GetConsumerByTopicAck;
import org.joyqueue.nsr.network.command.GetConsumerByTopicAndApp;
import org.joyqueue.nsr.network.command.GetConsumerByTopicAndAppAck;
import org.joyqueue.nsr.network.command.GetDataCenter;
import org.joyqueue.nsr.network.command.GetDataCenterAck;
import org.joyqueue.nsr.network.command.GetProducerByTopic;
import org.joyqueue.nsr.network.command.GetProducerByTopicAck;
import org.joyqueue.nsr.network.command.GetProducerByTopicAndApp;
import org.joyqueue.nsr.network.command.GetProducerByTopicAndAppAck;
import org.joyqueue.nsr.network.command.GetReplicaByBroker;
import org.joyqueue.nsr.network.command.GetReplicaByBrokerAck;
import org.joyqueue.nsr.network.command.GetTopicConfig;
import org.joyqueue.nsr.network.command.GetTopicConfigAck;
import org.joyqueue.nsr.network.command.GetTopicConfigByApp;
import org.joyqueue.nsr.network.command.GetTopicConfigByAppAck;
import org.joyqueue.nsr.network.command.GetTopicConfigByBroker;
import org.joyqueue.nsr.network.command.GetTopicConfigByBrokerAck;
import org.joyqueue.nsr.network.command.HasSubscribe;
import org.joyqueue.nsr.network.command.HasSubscribeAck;
import org.joyqueue.nsr.network.command.LeaderReport;
import org.joyqueue.nsr.network.command.NsrCommandType;
import org.joyqueue.nsr.network.command.NsrConnection;
import org.joyqueue.nsr.network.command.Register;
import org.joyqueue.nsr.network.command.RegisterAck;
import org.joyqueue.toolkit.concurrent.EventBus;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.config.PropertySupplierAware;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.lang.LifeCycle;
import org.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wylixiaobin
 * Date: 2019/1/27
 */
public class ThinNameService extends Service implements NameService, PropertySupplierAware, Type {
    private static final Logger logger = LoggerFactory.getLogger(ThinNameService.class);

    private NameServiceConfig nameServiceConfig;
    private PointTracer tracer;

    private ClientTransport clientTransport;
    private PropertySupplier propertySupplier;
    private Broker broker;
    /**
     * 事件管理器
     */
    protected EventBus<NameServerEvent> eventBus = new EventBus<>("joyqueue-thin-nameservice-eventBus");

    private Cache<TopicName, Optional<TopicConfig>> topicCache;

    public ThinNameService() {
        //do nothing
    }

    public ThinNameService(NameServiceConfig nameServiceConfig) {
        this.nameServiceConfig = nameServiceConfig;
    }

    @Override
    public void setSupplier(PropertySupplier supplier) {
        this.propertySupplier = supplier;
        if (nameServiceConfig == null) {
            nameServiceConfig = new NameServiceConfig(propertySupplier);
        }
        if (topicCache == null) {
            topicCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(nameServiceConfig.getThinCacheExpireTime(), TimeUnit.MILLISECONDS)
                    .build();
        }
        tracer = NsrPlugins.TRACERERVICE.get(PropertySupplier.getValue(propertySupplier, BrokerConfigKey.TRACER_TYPE));
        clientTransport = new ClientTransport(nameServiceConfig,this);
        try {
            eventBus.start();
            clientTransport.start();
        } catch (Exception e) {
            throw new NsrException(e);
        }
    }

    @Override
    public TopicConfig subscribe(Subscription subscription, ClientType clientType) {
        List<TopicConfig> topicConfigs = subscribe(Lists.newArrayList(subscription), clientType);
        if (CollectionUtils.isEmpty(topicConfigs)) {
            return null;
        }
        return topicConfigs.get(0);
    }

    @Override
    public List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.SUBSCRIBE),
                new Subscribe().subscriptions(subscriptions).clientType(clientType));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("subscribe error request {},response {}", request, response);
            throw new RuntimeException(String.format("subscribe error request {},response {}", request, response));
        }
        return ((SubscribeAck) response.getPayload()).getTopicConfigs();
    }

    @Override
    public void unSubscribe(Subscription subscription) {
        unSubscribe(Lists.newArrayList(subscription));
    }

    @Override
    public void unSubscribe(List<Subscription> subscriptions) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.UN_SUBSCRIBE), new UnSubscribe().subscriptions(subscriptions));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("unSubscribe error request {},response {}", request, response);
            throw new RuntimeException(String.format("unSubscribe error request {},response {}", request, response));
        }
    }

    @Override
    public boolean hasSubscribe(String app, Subscription.Type subscribe) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.HAS_SUBSCRIBE), new HasSubscribe().app(app).subscribe(subscribe));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("hasSubscribe error request {},response {}", request, response);
            throw new RuntimeException(String.format("hasSubscribe error request {},response {}", request, response));
        }
        return ((HasSubscribeAck) response.getPayload()).isHave();
    }

    @Override
    public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.LEADER_REPORT),
                new LeaderReport().topic(topic).partitionGroup(partitionGroup).leaderBrokerId(leaderBrokerId).isrId(isrId).termId(termId));
        sendAsync(request, new CommandCallback() {
            @Override
            public void onSuccess(Command request, Command response) {
                logger.info("Report leader of topic {} partition group {} to nameserver success",
                        topic, partitionGroup);
            }

            @Override
            public void onException(Command request, Throwable cause) {
                logger.info("Report leader of topic {} partition group {} to nameserver fail",
                        topic, partitionGroup, cause);
            }
        });
    }

    @Override
    public Broker getBroker(int brokerId) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_BROKER), new GetBroker().brokerId(brokerId));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getBroker error request {},response {}", request, response);
            throw new RuntimeException(String.format("getBroker error request {},response {}", request, response));
        }
        return ((GetBrokerAck) response.getPayload()).getBroker();
    }

    @Override
    public List<Broker> getAllBrokers() {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_ALL_BROKERS), new GetAllBrokers());
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getAllBrokers error request {},response {}", request, response);
            throw new RuntimeException(String.format("getAllBrokers error request {},response {}", request, response));
        }
        return ((GetAllBrokersAck) response.getPayload()).getBrokers();
    }

    @Override
    public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.ADD_TOPIC), new AddTopic().topic(topic).partitiionGroups(partitionGroups));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("leaderReport error request {},response {}", request, response);
            throw new RuntimeException(String.format("leaderReport error request {},response {}", request, response));
        }
    }

    @Override
    public TopicConfig getTopicConfig(TopicName topic) {
        if (nameServiceConfig.getThinCacheEnable()) {
            try {
                Optional<TopicConfig> optional = topicCache.get(topic, new Callable<Optional<TopicConfig>>() {
                    @Override
                    public Optional<TopicConfig> call() throws Exception {
                        return Optional.ofNullable(doGetTopicConfig(topic));
                    }
                });
                if (optional.isPresent()) {
                    return optional.get();
                }
                return null;
            } catch (ExecutionException e) {
                logger.error("getTopicConfig exception, topic: {}", topic, e);
                return null;
            }
        } else {
            return doGetTopicConfig(topic);
        }
    }

    protected TopicConfig doGetTopicConfig(TopicName topic) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_TOPICCONFIG), new GetTopicConfig().topic(topic));
        Command response = send(request, nameServiceConfig.getThinTransportTopicTimeout());
        if (!response.isSuccess()) {
            logger.error("getTopicConfig error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopicConfig error request {},response {}", request, response));
        }
        TopicConfig topicConfig = ((GetTopicConfigAck) response.getPayload()).getTopicConfig();
        return topicConfig;
    }

    @Override
    public Set<String> getAllTopicCodes() {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_ALL_TOPICS), new GetAllTopics());
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getAllTopicCodes error request {},response {}", request, response);
            throw new RuntimeException(String.format("getAllTopicCodes error request {},response {}", request, response));
        }
        return ((GetAllTopicsAck) response.getPayload()).getTopicNames();
    }

    @Override
    public Set<String> getTopics(String app, Subscription.Type subscription) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_TOPICS), new GetTopics().app(app).subscribeType(subscription.getValue()));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getTopics error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopics error request {},response {}", request, response));
        }
        return ((GetTopicsAck) response.getPayload()).getTopics();
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_TOPICCONFIGS_BY_BROKER), new GetTopicConfigByBroker().brokerId(brokerId));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getTopicConfigByBroker error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopicConfigByBroker error request {},response {}", request, response));
        }
        return ((GetTopicConfigByBrokerAck) response.getPayload()).getTopicConfigs();
    }

    @Override
    public Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.REGISTER),
                new Register().brokerId(brokerId).brokerIp(brokerIp).brokerHost(brokerHost).port(port).node(node));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("register error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopicConfigByBroker error request {},response {}", request, response));
        }
        broker = ((RegisterAck) response.getPayload()).getBroker();
        return broker;
    }

    @Override
    public Producer getProducerByTopicAndApp(TopicName topic, String app) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_PRODUCER_BY_TOPIC_AND_APP), new GetProducerByTopicAndApp().topic(topic).app(app));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getProducerByTopicAndApp error request {},response {}", request, response);
            throw new RuntimeException(String.format("getProducerByTopicAndApp error request {},response {}", request, response));
        }
        return ((GetProducerByTopicAndAppAck) response.getPayload()).getProducer();
    }

    @Override
    public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_CONSUMER_BY_TOPIC_AND_APP), new GetConsumerByTopicAndApp().topic(topic).app(app));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getConsumerByTopicAndApp error request {},response {}", request, response);
            throw new RuntimeException(String.format("getConsumerByTopicAndApp error request {},response {}", request, response));
        }
        return ((GetConsumerByTopicAndAppAck) response.getPayload()).getConsumer();
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByApp(String app, Subscription.Type subscribe) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_TOPICCONFIGS_BY_APP), new GetTopicConfigByApp().subscribe(subscribe).app(app));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getTopicConfigByApp error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopicConfigByApp error request {},response {}", request, response));
        }
        return ((GetTopicConfigByAppAck) response.getPayload()).getTopicConfigs();
    }

    @Override
    public DataCenter getDataCenter(String ip) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_DATACENTER), new GetDataCenter().ip(ip));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getTopicConfigByApp error request {},response {}", request, response);
            throw new RuntimeException(String.format("getTopicConfigByApp error request {},response {}", request, response));
        }
        return ((GetDataCenterAck) response.getPayload()).getDataCenter();
    }

    @Override
    public String getConfig(String group, String key) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_CONFIG), new GetConfig().group(group).key(key));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getConfig error request {},response {}", request, response);
            throw new RuntimeException(String.format("getConfig error request {},response {}", request, response));
        }
        return ((GetConfigAck) response.getPayload()).getValue();
    }

    @Override
    public List<Config> getAllConfigs() {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_ALL_CONFIG), new GetAllConfigs());
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getAllConfigs error request {},response {}", request, response);
            throw new RuntimeException(String.format("getAllConfigs error request {},response {}", request, response));
        }
        return ((GetAllConfigsAck) response.getPayload()).getConfigs();
    }

    @Override
    public List<Broker> getBrokerByRetryType(String retryType) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_BROKER_BY_RETRYTYPE), new GetBrokerByRetryType().retryType(retryType));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getBrokerByRetryType error request {},response {}", request, response);
            throw new RuntimeException(String.format("getBrokerByRetryType error request {},response {}", request, response));
        }
        return ((GetBrokerByRetryTypeAck) response.getPayload()).getBrokers();
    }

    @Override
    public List<Consumer> getConsumerByTopic(TopicName topic) {
        List<Consumer> consumers = new ArrayList<>();
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_CONSUMER_BY_TOPIC), new GetConsumerByTopic().topic(topic));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getConsumerByTopic error request {},response {}", request, response);
            throw new RuntimeException(String.format("getConsumerByTopic error request {},response {}", request, response));
        }
        List<Consumer> topicConsumes = ((GetConsumerByTopicAck) response.getPayload()).getConsumers();
        if (null != topicConsumes) {
            consumers.addAll(topicConsumes);
        }
        return consumers;
    }

    @Override
    public List<Producer> getProducerByTopic(TopicName topic) {
        List<Producer> producers = new ArrayList<>();
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_PRODUCER_BY_TOPIC), new GetProducerByTopic().topic(topic));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getProducerByTopic error request {},response {}", request, response);
            throw new RuntimeException(String.format("getProducerByTopic error request {},response {}", request, response));
        }
        List<Producer> topicProducers = ((GetProducerByTopicAck) response.getPayload()).getProducers();
        if (null != topicProducers) {
            producers.addAll(topicProducers);
        }
        return producers;
    }

    @Override
    public List<Replica> getReplicaByBroker(Integer brokerId) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_REPLICA_BY_BROKER), new GetReplicaByBroker().brokerId(brokerId));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getReplicaByBroker error request {},response {}", request, response);
            throw new RuntimeException(String.format("getReplicaByBroker error request {},response {}", request, response));
        }
        return ((GetReplicaByBrokerAck) response.getPayload()).getReplicas();
    }

    @Override
    public AppToken getAppToken(String app, String token) {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.GET_APP_TOKEN), new GetAppToken().app(app).token(token));
        Command response = send(request);
        if (!response.isSuccess()) {
            logger.error("getAppToken error request {},response {}", request, response);
            throw new RuntimeException(String.format("getAppToken error request {},response {}", request, response));
        }
        return ((GetAppTokenAck) response.getPayload()).getAppToken();
    }

    @Override
    public AllMetadata getAllMetadata() {
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.NSR_GET_ALL_METADATA_REQUEST), new GetAllMetadataRequest());
        Command response = send(request, nameServiceConfig.getAllMetadataTransportTimeout());
        if (!response.isSuccess()) {
            logger.error("getAllMetadata error request {},response {}", request, response);
            throw new RuntimeException(String.format("getAppToken error request {},response {}", request, response));
        }
        return ((GetAllMetadataResponse) response.getPayload()).getMetadata();
    }

    @Override
    public void addListener(EventListener<NameServerEvent> listener) {
        eventBus.addListener(listener);
    }

    @Override
    public void removeListener(EventListener<NameServerEvent> listener) {
        eventBus.removeListener(listener);
    }

    @Override
    public void addEvent(NameServerEvent event) {
        eventBus.add(event);
    }

    @Override
    protected void doStop() {
        super.doStop();
        Close.close(clientTransport);
        Close.close(eventBus);
        logger.info("name service stopped.");
    }

    protected Command send(Command request) throws TransportException {
        return send(request, nameServiceConfig.getThinTransportTimeout());
    }

    protected Command send(Command request, int timeout) throws TransportException {
        if (tracer == null) {
            return doSend(request, timeout);
        }
        TraceStat traceBegin = tracer.begin("ThinNameService.send." + request.getPayload().getClass().getSimpleName());
        try {
            Command result = doSend(request, timeout);
            tracer.end(traceBegin);
            return result;
        } catch (Exception e) {
            tracer.error(traceBegin);
            throw e;
        }
    }

    protected Command doSend(Command request, int timeout) throws TransportException {
        try {
            return clientTransport.getOrCreateTransport().sync(request, timeout);
        } catch (TransportException exception) {
            logger.error("send command to nameServer error request {}", request);
            throw exception;
        }
    }

    private void sendAsync(Command request, CommandCallback callback) throws TransportException {
        sendAsync(request, nameServiceConfig.getThinTransportTimeout(), callback);
    }

    private void sendAsync(Command request, int timeout, CommandCallback callback) throws TransportException {
        TraceStat traceBegin = tracer.begin("ThinNameService.asyncSend." + request.getPayload().getClass().getSimpleName());
        try {
            clientTransport.getOrCreateTransport().async(request, timeout, callback);
            tracer.end(traceBegin);
        } catch (TransportException exception) {
            logger.error("send command to nameServer error request {}", request);
            tracer.error(traceBegin);
            throw exception;
        }
    }

    private Command registerToNsr() throws TransportException {
        if (null == broker) {
            return null;
        }
        Command request = new Command(new JoyQueueHeader(Direction.REQUEST, NsrCommandType.CONNECT), new NsrConnection().brokerId(broker.getId()));
        return send(request);
    }

    @Override
    public Object type() {
        return "thin";
    }

    private class ClientTransport implements LifeCycle {
        private AtomicBoolean started = new AtomicBoolean(false);
        private TransportClient transportClient;
        protected final AtomicReference<Transport> transports = new AtomicReference<>();

        ClientTransport(NameServiceConfig config, NameService nameService) {
            this.transportClient = new NsrTransportClientFactory(nameService, propertySupplier).create(config.getClientConfig());
        }

        protected Transport getOrCreateTransport() throws TransportException {
            Transport transport = transports.get();
            if (transport == null) {
                synchronized (this) {
                    transport = transports.get();
                    if (transport == null) {
                        transport = transportClient.createTransport(nameServiceConfig.getNameserverAddress());
                        transports.set(transport);
                        registerToNsr();
                    }
                    logger.info("create transport connect to nameServer [{}]", nameServiceConfig.getNameserverAddress());
                }
            }
            return transport;
        }


        @Override
        public void start() throws Exception {
            if (started.compareAndSet(false, true)) {
                this.transportClient.start();
            }
        }

        @Override
        public void stop() {
            started.set(false);
            this.transportClient.stop();
        }

        @Override
        public boolean isStarted() {
            return started.get();
        }
    }


}
