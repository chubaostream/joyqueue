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
package io.chubao.joyqueue.nsr.nameservice;

import com.jd.laf.extension.ExtensionPoint;
import com.jd.laf.extension.ExtensionPointLazy;
import io.chubao.joyqueue.domain.AllMetadata;
import io.chubao.joyqueue.domain.AppToken;
import io.chubao.joyqueue.domain.Broker;
import io.chubao.joyqueue.domain.ClientType;
import io.chubao.joyqueue.domain.Config;
import io.chubao.joyqueue.domain.Consumer;
import io.chubao.joyqueue.domain.DataCenter;
import io.chubao.joyqueue.domain.PartitionGroup;
import io.chubao.joyqueue.domain.Producer;
import io.chubao.joyqueue.domain.Replica;
import io.chubao.joyqueue.domain.Subscription;
import io.chubao.joyqueue.domain.Topic;
import io.chubao.joyqueue.domain.TopicConfig;
import io.chubao.joyqueue.domain.TopicName;
import io.chubao.joyqueue.event.NameServerEvent;
import io.chubao.joyqueue.nsr.NameService;
import io.chubao.joyqueue.nsr.config.NameServiceConfig;
import io.chubao.joyqueue.nsr.exception.NsrException;
import io.chubao.joyqueue.nsr.message.Messenger;
import io.chubao.joyqueue.toolkit.concurrent.EventBus;
import io.chubao.joyqueue.toolkit.concurrent.EventListener;
import io.chubao.joyqueue.toolkit.config.PropertySupplier;
import io.chubao.joyqueue.toolkit.config.PropertySupplierAware;
import io.chubao.joyqueue.toolkit.lang.LifeCycle;
import io.chubao.joyqueue.toolkit.service.Service;
import io.chubao.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CompensatedNameService
 * author: gaohaoxiang
 * date: 2019/8/28
 */
public class CompensatedNameService extends Service implements NameService, PropertySupplierAware {

    protected static final Logger logger = LoggerFactory.getLogger(CompensatedNameService.class);

    private final EventBus<NameServerEvent> eventBus = new EventBus("joyqueue-compensated-nameservice-eventBus");
    private final ExtensionPoint<Messenger, String> serviceProviderPoint = new ExtensionPointLazy<>(Messenger.class);

    private NameServiceConfig config;
    private NameService delegate;

    private PropertySupplier supplier;
    private Messenger messenger;
    private NameServiceCacheManager nameServiceCacheManager;
    private NameServiceCompensator nameServiceCompensator;
    private NameServiceCompensateThread nameServiceCompensateThread;
    private int brokerId;
    private volatile long nameserverLastAvailableTime = 0;

    public CompensatedNameService(NameService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setSupplier(PropertySupplier supplier) {
        this.supplier = supplier;
        this.config = new NameServiceConfig(supplier);
        this.messenger = serviceProviderPoint.get(config.getMessengerType());
        this.nameServiceCacheManager = new NameServiceCacheManager(config);
        this.nameServiceCompensator = new NameServiceCompensator(config, eventBus);
        this.nameServiceCompensateThread = new NameServiceCompensateThread(config, delegate, nameServiceCacheManager, nameServiceCompensator);

        try {
            enrichIfNecessary(messenger);
            delegate.start();
            eventBus.start();

            nameServiceCacheManager.start();
            nameServiceCompensator.start();
            nameServiceCompensateThread.doCompensate();
        } catch (Exception e) {
            throw new NsrException(e);
        }
    }

    @Override
    protected void doStart() throws Exception {
        try {
            nameServiceCompensateThread.start();
        } catch (Exception e) {
            throw new NsrException(e);
        }
        messenger.addListener(new NameServiceCacheEventListener(config, nameServiceCacheManager));
        messenger.addListener(new NameServiceEventListenerAdapter(eventBus));
    }

    @Override
    protected void doStop() {
        nameServiceCompensateThread.stop();
        nameServiceCompensator.stop();
        nameServiceCacheManager.stop();
        delegate.stop();
        messenger.stop();
    }

    protected  <T> T enrichIfNecessary(T obj) throws Exception {
        if (obj instanceof LifeCycle) {
            if (((LifeCycle) obj).isStarted()) {
                return obj;
            }
        }
        if (obj instanceof PropertySupplierAware) {
            ((PropertySupplierAware) obj).setSupplier(supplier);
        }
        if (obj instanceof LifeCycle) {
            ((LifeCycle) obj).start();
        }
        return obj;
    }

    @Override
    public TopicConfig subscribe(Subscription subscription, ClientType clientType) {
        return delegate.subscribe(subscription, clientType);
    }

    @Override
    public List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType) {
        return delegate.subscribe(subscriptions, clientType);
    }

    @Override
    public void unSubscribe(Subscription subscription) {
        delegate.unSubscribe(subscription);
    }

    @Override
    public void unSubscribe(List<Subscription> subscriptions) {
        delegate.unSubscribe(subscriptions);
    }

    @Override
    public boolean hasSubscribe(String app, Subscription.Type subscribe) {
        return delegate.hasSubscribe(app, subscribe);
    }

    @Override
    public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId) {
        delegate.leaderReport(topic, partitionGroup, leaderBrokerId, isrId, termId);
    }

    @Override
    public Broker getBroker(int brokerId) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getBroker(brokerId);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getBroker(brokerId);
        }
        try {
            return delegate.getBroker(brokerId);
        } catch (Exception e) {
            logger.error("gerBroker exception, brokerId: {}", brokerId, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getBroker(brokerId);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Broker> getAllBrokers() {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getAllBrokers();
        }
        try {
            return delegate.getAllBrokers();
        } catch (Exception e) {
            logger.error("getAllBrokers exception", e);
            if (config.getCompensationErrorCacheEnable()) {
                return nameServiceCacheManager.getAllBrokers();
            }
            throw new NsrException(e);
        }
    }

    @Override
    public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
        delegate.addTopic(topic, partitionGroups);
    }

    @Override
    public TopicConfig getTopicConfig(TopicName topic) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getTopicConfig(topic);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getTopicConfig(topic);
        }
        try {
            return delegate.getTopicConfig(topic);
        } catch (Exception e) {
            logger.error("getTopicConfig exception, topic: {]", topic, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getTopicConfig(topic);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Set<String> getAllTopicCodes() {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getAllTopicCodes();
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getAllTopicCodes();
        }
        try {
            return delegate.getAllTopicCodes();
        } catch (Exception e) {
            logger.error("getAllTopicCodes exception", e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getAllTopicCodes();
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Set<String> getTopics(String app, Subscription.Type subscription) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getTopics(app, subscription);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getTopics(app, subscription);
        }
        try {
            return delegate.getTopics(app, subscription);
        } catch (Exception e) {
            logger.error("getTopics exception, app: {}, subscription: {}", app, subscription, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getTopics(app, subscription);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getTopicConfigByBroker(brokerId);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getTopicConfigByBroker(brokerId);
        }
        try {
            return delegate.getTopicConfigByBroker(brokerId);
        } catch (Exception e) {
            logger.error("getTopicConfigByBroker exception, brokerId: {}", brokerId, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getTopicConfigByBroker(brokerId);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Broker register(Integer brokerId, String brokerIp, Integer port) {
        Broker broker = delegate.register(brokerId, brokerIp, port);
        if (broker != null) {
            this.brokerId = broker.getId();
            this.nameServiceCompensator.setBrokerId(this.brokerId);
        }
        return broker;
    }

    @Override
    public Producer getProducerByTopicAndApp(TopicName topic, String app) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getProducerByTopicAndApp(topic, app);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getProducerByTopicAndApp(topic, app);
        }
        try {
            return delegate.getProducerByTopicAndApp(topic, app);
        } catch (Exception e) {
            logger.error("getProducerByTopicAndApp exception, topic: {}, app: {}", topic, app, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getProducerByTopicAndApp(topic, app);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getConsumerByTopicAndApp(topic, app);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getConsumerByTopicAndApp(topic, app);
        }
        try {
            return delegate.getConsumerByTopicAndApp(topic, app);
        } catch (Exception e) {
            logger.error("getConsumerByTopicAndApp exception, topic: {}, app: {}", topic, app, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getConsumerByTopicAndApp(topic, app);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByApp(String subscribeApp, Subscription.Type subscribe) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getTopicConfigByApp(subscribeApp, subscribe);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getTopicConfigByApp(subscribeApp, subscribe);
        }
        try {
            return delegate.getTopicConfigByApp(subscribeApp, subscribe);
        } catch (Exception e) {
            logger.error("getTopicConfigByApp exception, subscribeApp: {}, subscribe: {}", subscribeApp, subscribe, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getTopicConfigByApp(subscribeApp, subscribe);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public DataCenter getDataCenter(String ip) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getDataCenter(ip);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getDataCenter(ip);
        }
        try {
            return delegate.getDataCenter(ip);
        } catch (Exception e) {
            logger.error("getDataCenter exception, ip: {}", ip, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getDataCenter(ip);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public String getConfig(String group, String key) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getConfig(group, key);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getConfig(group, key);
        }
        try {
            return delegate.getConfig(group, key);
        } catch (Exception e) {
            logger.error("getConfig exception, group: {}, key: {}", group, key, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getConfig(group, key);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Config> getAllConfigs() {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getAllConfigs();
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getAllConfigs();
        }
        try {
            return delegate.getAllConfigs();
        } catch (Exception e) {
            logger.error("getAllConfigs exception", e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getAllConfigs();
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Broker> getBrokerByRetryType(String retryType) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getBrokerByRetryType(retryType);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getBrokerByRetryType(retryType);
        }
        try {
            return delegate.getBrokerByRetryType(retryType);
        } catch (Exception e) {
            logger.error("getBrokerByRetryType exception, retryType: {}", retryType, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getBrokerByRetryType(retryType);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Consumer> getConsumerByTopic(TopicName topic) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getConsumerByTopic(topic);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getConsumerByTopic(topic);
        }
        try {
            return delegate.getConsumerByTopic(topic);
        } catch (Exception e) {
            logger.error("getConsumerByTopic exception, topic: {}", topic, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getConsumerByTopic(topic);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Producer> getProducerByTopic(TopicName topic) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getProducerByTopic(topic);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getProducerByTopic(topic);
        }
        try {
            return delegate.getProducerByTopic(topic);
        } catch (Exception e) {
            logger.error("getProducerByTopic exception, topic: {}", topic, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getProducerByTopic(topic);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public List<Replica> getReplicaByBroker(Integer brokerId) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getReplicaByBroker(brokerId);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getReplicaByBroker(brokerId);
        }
        try {
            return delegate.getReplicaByBroker(brokerId);
        } catch (Exception e) {
            logger.error("getReplicaByBroker exception, brokerId: {}", brokerId, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getReplicaByBroker(brokerId);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public AppToken getAppToken(String app, String token) {
        if (config.getCompensationCacheEnable()) {
            return nameServiceCacheManager.getAppToken(app, token);
        }
        if (config.getCompensationErrorCacheEnable() && !nameserverIsAvailable()) {
            return nameServiceCacheManager.getAppToken(app, token);
        }
        try {
            return delegate.getAppToken(app, token);
        } catch (Exception e) {
            logger.error("getAppToken exception, app: {}, token: {}", app, token, e);
            if (config.getCompensationErrorCacheEnable()) {
                setNameserverNotAvailable();
                return nameServiceCacheManager.getAppToken(app, token);
            }
            throw new NsrException(e);
        }
    }

    @Override
    public AllMetadata getAllMetadata() {
        return delegate.getAllMetadata();
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

    protected boolean nameserverIsAvailable() {
        return nameserverLastAvailableTime == 0 ||
                SystemClock.now() - nameserverLastAvailableTime < config.getCompensationErrorRetryInterval();
    }

    protected void setNameserverNotAvailable() {
        nameserverLastAvailableTime = SystemClock.now();
    }

    public NameService getDelegate() {
        return delegate;
    }
}