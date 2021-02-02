package org.joyqueue.nsr.nameservice;

import com.jd.laf.extension.Type;
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
import org.joyqueue.network.transport.TransportServer;
import org.joyqueue.network.transport.config.ServerConfig;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.config.NameServerConfig;
import org.joyqueue.nsr.network.NsrTransportServerFactory;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.config.PropertySupplierAware;
import org.joyqueue.toolkit.service.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ThinNameServer
 * author: gaohaoxiang
 * date: 2020/4/20
 */
public class ThinNameServer extends Service implements NameService, PropertySupplierAware, Type {

    private ThinNameService delegate;
    private NameServerConfig nameServerConfig;
    private NsrTransportServerFactory transportServerFactory;
    private TransportServer transportServer;

    public ThinNameServer() {
        delegate = new ThinNameService();
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
        return delegate.getBroker(brokerId);
    }

    @Override
    public List<Broker> getAllBrokers() {
        return delegate.getAllBrokers();
    }

    @Override
    public void addTopic(Topic topic, List<PartitionGroup> partitionGroups) {
        delegate.addTopic(topic, partitionGroups);
    }

    @Override
    public TopicConfig getTopicConfig(TopicName topic) {
        return delegate.getTopicConfig(topic);
    }

    @Override
    public Set<String> getAllTopicCodes() {
        return delegate.getAllTopicCodes();
    }

    @Override
    public Set<String> getTopics(String app, Subscription.Type subscription) {
        return delegate.getTopics(app, subscription);
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId) {
        return delegate.getTopicConfigByBroker(brokerId);
    }

    @Override
    public Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node) {
        return delegate.register(brokerId, brokerHost, brokerIp, port, node);
    }

    @Override
    public Producer getProducerByTopicAndApp(TopicName topic, String app) {
        return delegate.getProducerByTopicAndApp(topic, app);
    }

    @Override
    public Consumer getConsumerByTopicAndApp(TopicName topic, String app) {
        return delegate.getConsumerByTopicAndApp(topic, app);
    }

    @Override
    public Map<TopicName, TopicConfig> getTopicConfigByApp(String app, Subscription.Type subscribe) {
        return delegate.getTopicConfigByApp(app, subscribe);
    }

    @Override
    public DataCenter getDataCenter(String ip) {
        return delegate.getDataCenter(ip);
    }

    @Override
    public String getConfig(String group, String key) {
        return delegate.getConfig(group, key);
    }

    @Override
    public List<Config> getAllConfigs() {
        return delegate.getAllConfigs();
    }

    @Override
    public List<Broker> getBrokerByRetryType(String retryType) {
        return delegate.getBrokerByRetryType(retryType);
    }

    @Override
    public List<Consumer> getConsumerByTopic(TopicName topic) {
        return delegate.getConsumerByTopic(topic);
    }

    @Override
    public List<Producer> getProducerByTopic(TopicName topic) {
        return delegate.getProducerByTopic(topic);
    }

    @Override
    public List<Replica> getReplicaByBroker(Integer brokerId) {
        return delegate.getReplicaByBroker(brokerId);
    }

    @Override
    public AppToken getAppToken(String app, String token) {
        return delegate.getAppToken(app, token);
    }

    @Override
    public AllMetadata getAllMetadata() {
        return delegate.getAllMetadata();
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
    public void setSupplier(PropertySupplier supplier) {
        delegate.setSupplier(supplier);
        nameServerConfig = new NameServerConfig(supplier);
        transportServerFactory = new NsrTransportServerFactory(this, supplier);
        transportServer = buildTransportServer();
    }

    @Override
    public void start() throws Exception {
        delegate.start();
        transportServer.start();
    }

    @Override
    public void stop() {
        transportServer.stop();
        delegate.stop();
    }

    protected TransportServer buildTransportServer() {
        ServerConfig serverConfig = nameServerConfig.getServerConfig();
        serverConfig.setPort(nameServerConfig.getServicePort());
        serverConfig.setAcceptThreadName("joyqueue-nameserver-accept-eventLoop");
        serverConfig.setIoThreadName("joyqueue-nameserver-io-eventLoop");
        return transportServerFactory.bind(serverConfig, serverConfig.getHost(), serverConfig.getPort());
    }

    @Override
    public Object type() {
        return "thin-server";
    }
}