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
import org.joyqueue.network.transport.command.JoyQueueCommand;
import org.joyqueue.nsr.config.NameServiceConfigKey;
import org.joyqueue.nsr.exception.NsrException;
import org.joyqueue.nsr.network.codec.GetAllMetadataResponseCodec;
import org.joyqueue.nsr.network.command.GetAllMetadataRequest;
import org.joyqueue.nsr.network.command.GetAllMetadataResponse;
import org.joyqueue.nsr.network.handler.GetAllMetadataRequestHandler;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GetAllMetadataRequestHandlerTest
 * author: gaohaoxiang
 * date: 2020/3/30
 */
public class GetAllMetadataRequestHandlerTest {

    private Map<String, Object> propertySupplierMap;
    private NameServiceStub nameServiceStub;
    private GetAllMetadataRequestHandler getAllMetadataRequestHandler;

    @Before
    public void before() {
        propertySupplierMap = new HashMap<>();
        propertySupplierMap.put(NameServiceConfigKey.NAMESERVER_ALL_METADATA_CACHE_ENABLE.getName(), false);
        nameServiceStub = new NameServiceStub();
        getAllMetadataRequestHandler = new GetAllMetadataRequestHandler();
        getAllMetadataRequestHandler.setSupplier(new PropertySupplier.MapSupplier(propertySupplierMap));
        getAllMetadataRequestHandler.setNameService(nameServiceStub);
    }

    @Test
    public void mergeMemoryConfigTest() {
        nameServiceStub.getAllConfigs().add(new Config("all", "key1", "value1"));
        GetAllMetadataResponse response = (GetAllMetadataResponse) getAllMetadataRequestHandler.handle(null, new JoyQueueCommand(new GetAllMetadataRequest())).getPayload();
        response.setMetadata((AllMetadata) GetAllMetadataResponseCodec.parseJson(response.getResponse(), AllMetadata.class));

        Assert.assertEquals(1, response.getMetadata().getConfigs().size());
        Assert.assertEquals("key1", response.getMetadata().getConfigs().get(0).getKey());
        Assert.assertEquals("value1", response.getMetadata().getConfigs().get(0).getValue());
        Assert.assertEquals("all", response.getMetadata().getConfigs().get(0).getGroup());

        nameServiceStub.getAllMetadata().getConfigs().add(new Config("all", "key1", "value1"));
        NameServiceStub.THROW_EXCEPTION = true;
        response = (GetAllMetadataResponse) getAllMetadataRequestHandler.handle(null, new JoyQueueCommand(new GetAllMetadataRequest())).getPayload();
        response.setMetadata((AllMetadata) GetAllMetadataResponseCodec.parseJson(response.getResponse(), AllMetadata.class));

        Assert.assertEquals(1, response.getMetadata().getConfigs().size());
        for (Config config : response.getMetadata().getConfigs()) {
            if (config.getKey().equals("nameservice.allmetadata.cache.enable")) {
                Assert.assertEquals("false", config.getValue());
                Assert.assertEquals(null, config.getGroup());
            } else if (config.getKey().equals("key1")) {
                Assert.assertEquals("value1", config.getValue());
                Assert.assertEquals("all", config.getGroup());
            } else {
                Assert.fail();
            }
        }
    }

    public static class NameServiceStub implements NameService {

        public static boolean THROW_EXCEPTION = false;

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
                throw new NsrException();
            }
        }
    }
}