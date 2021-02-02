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
package org.joyqueue.nsr;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.joyqueue.nsr.config.NameServiceConfigKey;
import org.joyqueue.nsr.exception.NsrException;
import org.joyqueue.nsr.nameservice.CompensatedNameService;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.network.IpUtil;
import org.junit.Assert;
import org.junit.Before;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CompensatedNameServiceTest
 * author: gaohaoxiang
 * date: 2019/12/9
 */
public class CompensatedNameServiceTest {

    private NameServiceStub nameServiceStub;
    private CompensatedNameService compensatedNameService;
    private PropertySupplier propertySupplier;

    @Before
    public void before() throws Exception {
        Map<String, Object> propertySupplierMap = new HashMap<>();
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_MESSENGER_TYPE.getName(), "test");
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_COMPENSATION_INTERVAL.getName(), 1000 * 60 * 1);
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_RETRY_INTERVAL.getName(), 1000 * 10);
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_THRESHOLD.getName(), 3);
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_COMPENSATION_CACHE_ENABLE.getName(), false);
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_CACHE_ENABLE.getName(), true);
        propertySupplier = new PropertySupplier.MapSupplier(propertySupplierMap);

        nameServiceStub = new NameServiceStub();
        compensatedNameService = new CompensatedNameService(nameServiceStub);
        nameServiceStub.subscribe(new Subscription(TopicName.parse("test_topic"), "test_app", Subscription.Type.PRODUCTION), ClientType.JOYQUEUE);
        nameServiceStub.subscribe(new Subscription(TopicName.parse("test_topic"), "test_app", Subscription.Type.CONSUMPTION), ClientType.JOYQUEUE);
        compensatedNameService.setSupplier(propertySupplier);
        compensatedNameService.register(1, "127.0.0.1",  "127.0.0.1",50088, IpUtil.getNode());
        compensatedNameService.start();
    }

//    @Test
    public void testErrorCache() throws Exception {
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
        }
        Assert.assertEquals(0, NameServiceStub.EXCEPTION_COUNTER);

        NameServiceStub.THROW_EXCEPTION = true;
        NameServiceStub.EXCEPTION_SLEEP_TIME = 1000 * 1;
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
        }
        Assert.assertEquals(propertySupplier.getProperty(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_THRESHOLD.getName()).getInteger().intValue(), NameServiceStub.EXCEPTION_COUNTER);

        Thread.currentThread().sleep(1000 * 10);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
        }
        Assert.assertEquals(propertySupplier.getProperty(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_THRESHOLD.getName()).getInteger().intValue() * 2, NameServiceStub.EXCEPTION_COUNTER);

        NameServiceStub.THROW_EXCEPTION = false;
        Thread.currentThread().sleep(propertySupplier.getProperty(NameServiceConfigKey.NAMESERVER_COMPENSATION_ERROR_RETRY_INTERVAL.getName()).getInteger());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getProducerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getTopic().getFullName(), "test_topic");
            Assert.assertEquals(compensatedNameService.getConsumerByTopicAndApp(TopicName.parse("test_topic"), "test_app").getApp(), "test_app");
        }
        Assert.assertEquals(0, NameServiceStub.EXCEPTION_COUNTER);
    }

    public static class NameServiceStub implements NameService {

        public static int EXCEPTION_SLEEP_TIME = 1000 * 1;
        public static boolean THROW_EXCEPTION = false;
        public static int EXCEPTION_COUNTER = 0;
        public static int INVOKE_COUNTER = 0;

        private AllMetadata allMetadata;

        public NameServiceStub() {
            allMetadata = new AllMetadata();
            allMetadata.setTopics(Maps.newHashMap());
            allMetadata.setBrokers(Maps.newHashMap());
            allMetadata.setProducers(Lists.newArrayList());
            allMetadata.setConsumers(Lists.newArrayList());
            allMetadata.setDataCenters(Lists.newArrayList());
            allMetadata.setConfigs(Lists.newArrayList());
            allMetadata.setAppTokens(Lists.newArrayList());

        }

        @Override
        public TopicConfig subscribe(Subscription subscription, ClientType clientType) {
            if (subscription.getType().equals(Subscription.Type.CONSUMPTION)) {
                Consumer consumer = new Consumer();
                consumer.setTopic(subscription.getTopic());
                consumer.setApp(subscription.getApp());
                consumer.setClientType(clientType);
                allMetadata.getConsumers().add(consumer);
            } else if (subscription.getType().equals(Subscription.Type.PRODUCTION)) {
                Producer producer = new Producer();
                producer.setTopic(subscription.getTopic());
                producer.setApp(subscription.getApp());
                producer.setClientType(clientType);
                allMetadata.getProducers().add(producer);
            }
            return null;
        }

        @Override
        public List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType) {
            return null;
        }

        @Override
        public void unSubscribe(Subscription subscription) {

        }

        @Override
        public void unSubscribe(List<Subscription> subscriptions) {

        }

        @Override
        public boolean hasSubscribe(String app, Subscription.Type subscribe) {
            return false;
        }

        @Override
        public void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId) {

        }

        @Override
        public Broker getBroker(int brokerId) {
            check();
            return allMetadata.getBrokers().get(brokerId);
        }

        @Override
        public List<Broker> getAllBrokers() {
            check();
            return Lists.newArrayList(allMetadata.getBrokers().values());
        }

        @Override
        public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
            allMetadata.getTopics().put(topic.getName(), TopicConfig.toTopicConfig(topic, partitionGroups));
        }

        @Override
        public TopicConfig getTopicConfig(TopicName topic) {
            check();
            return allMetadata.getTopics().get(topic);
        }

        @Override
        public Set<String> getAllTopicCodes() {
            check();
            Set<String> result = Sets.newHashSet();
            for (Map.Entry<TopicName, TopicConfig> entry : allMetadata.getTopics().entrySet()) {
                result.add(entry.getKey().getFullName());
            }
            return result;
        }

        @Override
        public Set<String> getTopics(String app, Subscription.Type subscription) {
            check();
            Set<String> result = Sets.newHashSet();
            if (subscription.equals(Subscription.Type.CONSUMPTION)) {
                for (Consumer consumer : allMetadata.getConsumers()) {
                    if (consumer.getApp().equals(app)) {
                        result.add(consumer.getTopic().getFullName());
                    }
                }
            } else if (subscription.equals(Subscription.Type.PRODUCTION)) {
                for (Producer producer : allMetadata.getProducers()) {
                    if (producer.getApp().equals(app)) {
                        result.add(producer.getTopic().getFullName());
                    }
                }
            }
            return result;
        }

        @Override
        public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
            check();
            Map<TopicName, TopicConfig> result = Maps.newHashMap();
            for (Map.Entry<TopicName, TopicConfig> entry : allMetadata.getTopics().entrySet()) {
                if (entry.getValue().isReplica(brokerId)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
            return result;
        }

        @Override
        public Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
            check();
            Broker broker = new Broker();
            broker.setId(brokerId);
            broker.setIp(brokerHost);
            broker.setExternalIp(brokerIp);
            broker.setPort(port);
            broker.setExternalPort(port);
            broker.setNode(node);
            allMetadata.getBrokers().put(brokerId, broker);
            return broker;
        }

        @Override
        public Producer getProducerByTopicAndApp(TopicName topic, String app) {
            check();
            for (Producer producer : allMetadata.getProducers()) {
                if (producer.getTopic().equals(topic) && producer.getApp().equals(app)) {
                    return producer;
                }
            }
            return null;
        }

        @Override
        public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
            check();
            for (Consumer consumer : allMetadata.getConsumers()) {
                if (consumer.getTopic().equals(topic) && consumer.getApp().equals(app)) {
                    return consumer;
                }
            }
            return null;
        }

        @Override
        public Map<TopicName, TopicConfig> getTopicConfigByApp(String subscribeApp, Subscription.Type subscribe) {
            check();
            Map<TopicName, TopicConfig> result = Maps.newHashMap();
            if (subscribe.equals(Subscription.Type.CONSUMPTION)) {
                for (Consumer consumer : allMetadata.getConsumers()) {
                    if (consumer.getApp().equals(subscribeApp)) {
                        result.put(consumer.getTopic(), allMetadata.getTopics().get(consumer.getTopic()));
                    }
                }
            } else if (subscribe.equals(Subscription.Type.PRODUCTION)) {
                for (Producer producer : allMetadata.getProducers()) {
                    if (producer.getApp().equals(subscribeApp)) {
                        result.put(producer.getTopic(), allMetadata.getTopics().get(producer.getTopic()));
                    }
                }
            }
            return result;
        }

        @Override
        public DataCenter getDataCenter(String ip) {
            check();
            for (DataCenter dataCenter : allMetadata.getDataCenters()) {
                if (dataCenter.getUrl().equals(ip)) {
                    return dataCenter;
                }
            }
            return null;
        }

        @Override
        public String getConfig(String group, String key) {
            for (Config config : allMetadata.getConfigs()) {
                if (config.getGroup().equals(group) && config.getKey().equals(key)) {
                    return config.getValue();
                }
            }
            return null;
        }

        @Override
        public List<Config> getAllConfigs() {
            check();
            return allMetadata.getConfigs();
        }

        @Override
        public List<Broker> getBrokerByRetryType(String retryType) {
            check();
            return Lists.newArrayList(allMetadata.getBrokers().values());
        }

        @Override
        public List<Consumer> getConsumerByTopic(TopicName topic) {
            check();
            List<Consumer> result = Lists.newArrayList();
            for (Consumer consumer : allMetadata.getConsumers()) {
                if (consumer.getTopic().equals(topic)) {
                    result.add(consumer);
                }
            }
            return result;
        }

        @Override
        public List<Producer> getProducerByTopic(TopicName topic) {
            check();
            List<Producer> result = Lists.newArrayList();
            for (Producer producer : allMetadata.getProducers()) {
                if (producer.getTopic().equals(topic)) {
                    result.add(producer);
                }
            }
            return result;
        }

        @Override
        public List<Replica> getReplicaByBroker(Integer brokerId) {
            check();
            List<Replica> result = Lists.newArrayList();
            return result;
        }

        @Override
        public AppToken getAppToken(String app, String token) {
            check();
            return null;
        }

        @Override
        public AllMetadata getAllMetadata() {
            check();
            return allMetadata;
        }

        @Override
        public void addListener(EventListener<NameServerEvent> listener) {

        }

        @Override
        public void removeListener(EventListener<NameServerEvent> listener) {

        }

        @Override
        public void addEvent(NameServerEvent event) {

        }

        @Override
        public void start() throws Exception {

        }

        @Override
        public void stop() {

        }

        @Override
        public boolean isStarted() {
            return true;
        }

        protected void check() {
            if (THROW_EXCEPTION) {
                EXCEPTION_COUNTER++;
                try {
                    Thread.currentThread().sleep(EXCEPTION_SLEEP_TIME);
                } catch (InterruptedException e) {
                }
                throw new NsrException();
            } else {
                EXCEPTION_COUNTER = 0;
            }
            INVOKE_COUNTER++;
        }
    }
}