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
import com.google.common.collect.Maps;
import com.jd.laf.extension.Type;
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
import org.joyqueue.network.transport.TransportServer;
import org.joyqueue.network.transport.config.ServerConfig;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.NsrPlugins;
import org.joyqueue.nsr.config.NameServerConfig;
import org.joyqueue.nsr.exception.NsrException;
import org.joyqueue.nsr.network.NsrTransportServerFactory;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.config.PropertySupplierAware;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * NameServerWrapper
 * author: gaohaoxiang
 * date: 2020/2/24
 */
public class NameServer extends Service implements NameService, PropertySupplierAware, Type {

    protected static final Logger logger = LoggerFactory.getLogger(NameServer.class);

    private NameServerInternal delegate;
    private PointTracer tracer;

    private NameServerConfig nameServerConfig;
    private NsrTransportServerFactory transportServerFactory;
    private TransportServer transportServer;

    private Cache<String, Map<TopicName, TopicConfig>> appTopicCache;
    private Cache<String, Optional<TopicConfig>> topicCache;

    public NameServer() {
        this.delegate = new NameServerInternal();
    }

    @Override
    public TopicConfig subscribe(Subscription subscription, ClientType clientType) {
        TraceStat trace = tracer.begin("NameService.subscribe");
        try {
            TopicConfig result = delegate.subscribe(subscription, clientType);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType) {
        TraceStat trace = tracer.begin("NameService.subscribes");
        try {
            List<TopicConfig> result = delegate.subscribe(subscriptions, clientType);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public void unSubscribe(Subscription subscription) {
        TraceStat trace = tracer.begin("NameService.Subscribe");
        try {
            delegate.unSubscribe(subscription);
            tracer.end(trace);
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public void unSubscribe(List<Subscription> subscriptions) {
        TraceStat trace = tracer.begin("NameService.Subscribes");
        try {
            delegate.unSubscribe(subscriptions);
            tracer.end(trace);
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public boolean hasSubscribe(String app, Subscription.Type subscribe) {
        TraceStat trace = tracer.begin("NameService.hasSubscribe");
        try {
            boolean result = delegate.hasSubscribe(app, subscribe);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId) {
        TraceStat trace = tracer.begin("NameService.leaderReport");
        try {
            delegate.leaderReport(topic, partitionGroup, leaderBrokerId, isrId, termId);
            tracer.end(trace);
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Broker getBroker(int brokerId) {
        TraceStat trace = tracer.begin("NameService.getBroker");
        try {
            Broker result = delegate.getBroker(brokerId);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Broker> getAllBrokers() {
        TraceStat trace = tracer.begin("NameService.getAllBrokers");
        try {
            List<Broker> result = delegate.getAllBrokers();
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
        TraceStat trace = tracer.begin("NameService.addTopic");
        try {
            delegate.addTopic(topic, partitionGroups);
            tracer.end(trace);
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public TopicConfig getTopicConfig(TopicName topic) {
        TraceStat trace = tracer.begin("NameService.getTopicConfig");
        try {
            TopicConfig result = doGetTopicConfig(topic);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    protected TopicConfig doGetTopicConfig(TopicName topicName) {
        if (nameServerConfig.getCacheEnable()) {
            try {
                Optional<TopicConfig> result = topicCache.get(topicName.getFullName(), () -> {
                    return Optional.ofNullable(delegate.getTopicConfig(topicName));
                });
                return (result.isPresent() ? result.get() : null);
            } catch (Exception e) {
                logger.error("getTopicConfig exception, topicName: {}", topicName, e);
                throw new NsrException(e);
            }
        } else {
            return delegate.getTopicConfig(topicName);
        }
    }

    @Override
    public Set<String> getAllTopicCodes() {
        TraceStat trace = tracer.begin("NameService.getAllTopicCodes");
        try {
            Set<String> result = delegate.getAllTopicCodes();
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Set<String> getTopics(String app, Subscription.Type subscription) {
        TraceStat trace = tracer.begin("NameService.getTopics");
        try {
            Set<String> result = delegate.getTopics(app, subscription);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
        TraceStat trace = tracer.begin("NameService.getTopicConfigByBroker");
        try {
            Map<TopicName, TopicConfig> result = delegate.getTopicConfigByBroker(brokerId);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
        TraceStat trace = tracer.begin("NameService.register");
        try {
            Broker result = delegate.register(brokerId, brokerHost, brokerIp, port, node);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Producer getProducerByTopicAndApp(TopicName topic, String app) {
        TraceStat trace = tracer.begin("NameService.getProducerByTopicAndApp");
        try {
            Producer result = delegate.getProducerByTopicAndApp(topic, app);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
        TraceStat trace = tracer.begin("NameService.getConsumerByTopicAndApp");
        try {
            Consumer result = delegate.getConsumerByTopicAndApp(topic, app);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByApp(String subscribeApp, Subscription.Type subscribe) {
        TraceStat trace = tracer.begin("NameService.getTopicConfigByApp");
        try {
            Map<TopicName, TopicConfig> result = doGetTopicConfigByApp(subscribeApp, subscribe);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    protected Map<TopicName, TopicConfig> doGetTopicConfigByApp(String subscribeApp, Subscription.Type subscribe) {
        if (nameServerConfig.getCacheEnable()) {
            try {
                return appTopicCache.get(subscribeApp + "_" + String.valueOf(subscribe), new Callable<Map<TopicName, TopicConfig>>() {
                    @Override
                    public Map<TopicName, TopicConfig> call() throws Exception {
                        return delegate.getTopicConfigByApp(subscribeApp, subscribe);
                    }
                });
            } catch (ExecutionException e) {
                logger.error("getTopicConfigByApp exception, subscribeApp: {}, subscribe: {}",
                        subscribeApp, subscribe);
                return Maps.newHashMap();
            }
        } else {
            return delegate.getTopicConfigByApp(subscribeApp, subscribe);
        }
    }

    @Override
    public DataCenter getDataCenter(String ip) {
        TraceStat trace = tracer.begin("NameService.getDataCenter");
        try {
            DataCenter result = delegate.getDataCenter(ip);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public String getConfig(String group, String key) {
        TraceStat trace = tracer.begin("NameService.getConfig");
        try {
            String result = delegate.getConfig(group, key);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Config> getAllConfigs() {
        TraceStat trace = tracer.begin("NameService.getAllConfigs");
        try {
            List<Config> result = delegate.getAllConfigs();
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Broker> getBrokerByRetryType(String retryType) {
        TraceStat trace = tracer.begin("NameService.getBrokerByRetryType");
        try {
            List<Broker> result = delegate.getBrokerByRetryType(retryType);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Consumer> getConsumerByTopic(TopicName topic) {
        TraceStat trace = tracer.begin("NameService.getConsumerByTopic");
        try {
            List<Consumer> result = delegate.getConsumerByTopic(topic);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Producer> getProducerByTopic(TopicName topic) {
        TraceStat trace = tracer.begin("NameService.getProducerByTopic");
        try {
            List<Producer> result = delegate.getProducerByTopic(topic);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public List<Replica> getReplicaByBroker(Integer brokerId) {
        TraceStat trace = tracer.begin("NameService.getReplicaByBroker");
        try {
            List<Replica> result = delegate.getReplicaByBroker(brokerId);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public AppToken getAppToken(String app, String token) {
        TraceStat trace = tracer.begin("NameService.getAppToken");
        try {
            AppToken result = delegate.getAppToken(app, token);
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public AllMetadata getAllMetadata() {
        TraceStat trace = tracer.begin("NameService.getAllMetadata");
        try {
            AllMetadata result = delegate.getAllMetadata();
            tracer.end(trace);
            return result;
        } catch (Exception e) {
            tracer.error(trace);
            throw e;
        }
    }

    @Override
    public void addListener(EventListener<NameServerEvent> listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EventListener<NameServerEvent> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void addEvent(NameServerEvent event) {
        delegate.addEvent(event);
    }

    @Override
    protected void doStop() {
        Close.close(transportServer);
        delegate.doStop();
    }

    @Override
    public void setSupplier(PropertySupplier propertySupplier) {
        delegate.setSupplier(propertySupplier);
        tracer = NsrPlugins.TRACERERVICE.get(PropertySupplier.getValue(propertySupplier, BrokerConfigKey.TRACER_TYPE));
        nameServerConfig = new NameServerConfig(propertySupplier);
        transportServerFactory = new NsrTransportServerFactory(this, propertySupplier);
        transportServer = buildTransportServer();
        topicCache = CacheBuilder.newBuilder()
                .expireAfterWrite(nameServerConfig.getTopicCacheExpireTime(), TimeUnit.MILLISECONDS)
                .build();
        appTopicCache = CacheBuilder.newBuilder()
                .expireAfterWrite(nameServerConfig.getTopicCacheExpireTime(), TimeUnit.MILLISECONDS)
                .build();
        try {
            transportServer.start();
        } catch (Exception e) {
            throw new NsrException(e);
        }
    }

    protected TransportServer buildTransportServer(){
        ServerConfig serverConfig = nameServerConfig.getServerConfig();
        serverConfig.setPort(nameServerConfig.getServicePort());
        serverConfig.setAcceptThreadName("joyqueue-nameserver-accept-eventLoop");
        serverConfig.setIoThreadName("joyqueue-nameserver-io-eventLoop");
        return transportServerFactory.bind(serverConfig, serverConfig.getHost(), serverConfig.getPort());
    }

    @Override
    public Object type() {
        return "server";
    }
}