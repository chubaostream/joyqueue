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
package io.chubao.joyqueue.broker.monitor;

import com.google.common.collect.Lists;
import io.chubao.joyqueue.broker.cluster.ClusterManager;
import io.chubao.joyqueue.broker.monitor.config.BrokerMonitorConfig;
import io.chubao.joyqueue.broker.monitor.stat.AppStat;
import io.chubao.joyqueue.broker.monitor.stat.BrokerStat;
import io.chubao.joyqueue.broker.monitor.stat.ConsumerStat;
import io.chubao.joyqueue.broker.monitor.stat.PartitionGroupStat;
import io.chubao.joyqueue.broker.monitor.stat.ProducerStat;
import io.chubao.joyqueue.broker.monitor.stat.ReplicationStat;
import io.chubao.joyqueue.broker.monitor.stat.TopicStat;
import io.chubao.joyqueue.domain.PartitionGroup;
import io.chubao.joyqueue.domain.TopicName;
import io.chubao.joyqueue.event.MetaEvent;
import io.chubao.joyqueue.monitor.Client;
import io.chubao.joyqueue.network.session.Connection;
import io.chubao.joyqueue.network.session.Consumer;
import io.chubao.joyqueue.network.session.Producer;
import io.chubao.joyqueue.nsr.event.RemoveConsumerEvent;
import io.chubao.joyqueue.nsr.event.RemovePartitionGroupEvent;
import io.chubao.joyqueue.nsr.event.RemoveProducerEvent;
import io.chubao.joyqueue.nsr.event.RemoveTopicEvent;
import io.chubao.joyqueue.nsr.event.UpdatePartitionGroupEvent;
import io.chubao.joyqueue.toolkit.concurrent.EventListener;
import io.chubao.joyqueue.toolkit.network.IpUtil;
import io.chubao.joyqueue.toolkit.service.Service;
import io.chubao.joyqueue.toolkit.time.SystemClock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * BrokerMonitor
 *
 * author: gaohaoxiang
 * date: 2018/11/16
 */
public class BrokerMonitor extends Service implements ConsumerMonitor, ProducerMonitor, ReplicationMonitor, SessionMonitor, EventListener<SessionManager.SessionEvent> {

    private static final Logger logger = LoggerFactory.getLogger(BrokerMonitor.class);

    private BrokerMonitorConfig config;
    private SessionManager sessionManager;
    private BrokerStatManager brokerStatManager;
    // 集群管理
    private ClusterManager clusterManager;

    // 统计基础汇总信息
    private BrokerStat brokerStat;

    public BrokerMonitor() {

    }

    public BrokerMonitor(BrokerMonitorConfig config, SessionManager sessionManager, BrokerStatManager brokerStatManager, ClusterManager clusterManager) {
        this.config = config;
        this.sessionManager = sessionManager;
        this.brokerStatManager = brokerStatManager;
        this.clusterManager = clusterManager;
    }

    @Override
    protected void doStart() throws Exception {
        brokerStat = brokerStatManager.getBrokerStat();
        clearInvalidStat(brokerStat);
        sessionManager.addListener(this);
        clusterManager.addListener(new MonitorMateDataListener());
    }

    protected void clearInvalidStat(BrokerStat brokerStat) {
        Iterator<Map.Entry<String, TopicStat>> topicIterator = brokerStat.getTopicStats().entrySet().iterator();
        while (topicIterator.hasNext()) {
            TopicStat topicStat = topicIterator.next().getValue();
            TopicName topic = TopicName.parse(topicStat.getTopic());
            if (clusterManager.getTopicConfig(topic) == null) {
                topicIterator.remove();
                continue;
            }

            Iterator<Map.Entry<String, AppStat>> appIterator = topicStat.getAppStats().entrySet().iterator();
            while (appIterator.hasNext()) {
                Map.Entry<String, AppStat> appStatEntry = appIterator.next();
                AppStat appStat = appStatEntry.getValue();
                if (StringUtils.isBlank(appStat.getApp())) {
                    appIterator.remove();
                } else {
                    boolean isExistConsumer = clusterManager.tryGetConsumer(topic, appStat.getApp()) != null;
                    boolean isExistProducer = clusterManager.tryGetProducer(topic, appStat.getApp()) != null;

                    if (!isExistConsumer && !isExistProducer) {
                        appIterator.remove();
                    } else {
                        if (!isExistConsumer) {
                            appStat.getConsumerStat().clear();
                        }
                        if (!isExistProducer) {
                            appStat.getProducerStat().clear();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doStop() {
        sessionManager.removeListener(this);
    }

    @Override
    public void onPutMessage(String topic, String app, int partitionGroup, short partition, long count, long size, double time) {
        if (!config.isEnable()) {
            return;
        }

        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        topicStat.getEnQueueStat().mark(time, size, count);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
//            PartitionGroupStat partitionGroupStat = appStat.getOrCreatePartitionGroupStat(partitionGroup);
//            PartitionStat partitionStat = partitionGroupStat.getOrCreatePartitionStat(partition);
        ProducerStat producerStat = appStat.getProducerStat();
        PartitionGroupStat producerPartitionGroupStat = producerStat.getOrCreatePartitionGroupStat(partitionGroup);

//            topicStat.getEnQueueStat().mark(time, size, count);
//            topicStat.getOrCreatePartitionGroupStat(partitionGroup).getEnQueueStat().mark(time, size, count);
//            topicStat.getOrCreatePartitionGroupStat(partitionGroup).getOrCreatePartitionStat(partition).getEnQueueStat().mark(time, size, count);

        producerStat.getEnQueueStat().mark(time, size, count);
        producerPartitionGroupStat.getEnQueueStat().mark(time, size, count);
        producerPartitionGroupStat.getOrCreatePartitionStat(partition).getEnQueueStat().mark(time, size, count);

//            partitionGroupStat.getEnQueueStat().mark(time, size, count);
//            partitionStat.getEnQueueStat().mark(time, size, count);
        brokerStat.getEnQueueStat().mark(time, size, count);
    }

    @Override
    public ConsumerStat getConsumerStat(String topic, String app) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        return appStat.getConsumerStat();
    }

    @Override
    public void onGetMessage(String topic, String app, int partitionGroup, short partition, long count, long size, double time) {
        if (!config.isEnable()) {
            return;
        }
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        topicStat.getDeQueueStat().mark(time, size, count);

        AppStat appStat = topicStat.getOrCreateAppStat(app);
//            PartitionGroupStat partitionGroupStat = appStat.getOrCreatePartitionGroupStat(partitionGroup);
//            PartitionStat partitionStat = partitionGroupStat.getOrCreatePartitionStat(partition);
        ConsumerStat consumerStat = appStat.getConsumerStat();
        PartitionGroupStat consumerPartitionGroupStat = consumerStat.getOrCreatePartitionGroupStat(partitionGroup);

//            topicStat.getDeQueueStat().mark(time, size, count);
//            topicStat.getOrCreatePartitionGroupStat(partitionGroup).getDeQueueStat().mark(time, size, count);
//            topicStat.getOrCreatePartitionGroupStat(partitionGroup).getOrCreatePartitionStat(partition).getDeQueueStat().mark(time, size, count);

        consumerStat.getDeQueueStat().mark(time, size, count);
        consumerPartitionGroupStat.getDeQueueStat().mark(time, size, count);
        consumerPartitionGroupStat.getOrCreatePartitionStat(partition).getDeQueueStat().mark(time, size, count);

//            partitionGroupStat.getDeQueueStat().mark(time, size, count);
//            partitionStat.getDeQueueStat().mark(time, size, count);
        brokerStat.getDeQueueStat().mark(time, size, count);
    }

    @Override
    public void onAckMessage(String topic, String app, int partitionGroup, short partition) {
        if (!config.isEnable()) {
            return;
        }
        brokerStat.getOrCreateTopicStat(topic).
                getOrCreateAppStat(app)
                .getConsumerStat()
                .getOrCreatePartitionGroupStat(partitionGroup).
                getOrCreatePartitionStat(partition).
                lastAckTime(SystemClock.now());
    }

    @Override
    public void onReplicateMessage(String topic, int partitionGroup, long count, long size, double time) {
        if (!config.isEnable()) {
            return;
        }
        ReplicationStat replicationStat = brokerStat.getOrCreateTopicStat(topic).getOrCreatePartitionGroupStat(partitionGroup).getReplicationStat();
        replicationStat.getReplicaStat().mark(time, size, count);
        brokerStat.getReplicationStat().getReplicaStat().mark(time, size, count);
    }

    @Override
    public void onAppendReplicateMessage(String topic, int partitionGroup, long count, long size, double time) {
        if (!config.isEnable()) {
            return;
        }
        ReplicationStat replicationStat = brokerStat.getOrCreateTopicStat(topic).getOrCreatePartitionGroupStat(partitionGroup).getReplicationStat();
        replicationStat.getAppendStat().mark(time, size, count);
        brokerStat.getReplicationStat().getAppendStat().mark(time, size, count);
    }

    @Override
    public void onGetRetry(String topic, String app, long count, double time) {
        if (!config.isEnable()) {
            return;
        }
    }

    @Override
    public void onAddRetry(String topic, String app, long count, double time) {
        if (!config.isEnable()) {
            return;
        }
    }

    @Override
    public void onRetrySuccess(String topic, String app, long count) {
        if (!config.isEnable()) {
            return;
        }
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        appStat.getConsumerStat().getRetryStat().getSuccess().mark(count);
    }

    @Override
    public void onRetryFailure(String topic, String app, long count) {
        if (!config.isEnable()) {
            return;
        }
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        appStat.getConsumerStat().getRetryStat().getFailure().mark(count);
    }

    @Override
    public void addProducer(Producer producer) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(producer.getTopic());
        AppStat appStat = topicStat.getOrCreateAppStat(producer.getApp());
        Client client = brokerStat.getConnectionStat().getConnection(producer.getConnectionId());

        if (client == null) {
            return;
        }
        client.setProducerRole(true);
        appStat.getConnectionStat().addConnection(client);
        appStat.getConnectionStat().incrProducer();
        appStat.getProducerStat().getConnectionStat().addConnection(client);
        appStat.getProducerStat().getConnectionStat().incrProducer();

        topicStat.getConnectionStat().addConnection(client);
        topicStat.getConnectionStat().incrProducer();

        brokerStat.getConnectionStat().incrProducer();
    }

    @Override
    public void addConsumer(Consumer consumer) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(consumer.getTopic());
        AppStat appStat = topicStat.getOrCreateAppStat(consumer.getApp());
        Client client = brokerStat.getConnectionStat().getConnection(consumer.getConnectionId());

        if (client == null) {
            return;
        }
        client.setConsumerRole(true);
        appStat.getConnectionStat().addConnection(client);
        appStat.getConnectionStat().incrConsumer();
        appStat.getConsumerStat().getConnectionStat().addConnection(client);
        appStat.getConsumerStat().getConnectionStat().incrConsumer();

        topicStat.getConnectionStat().addConnection(client);
        topicStat.getConnectionStat().incrConsumer();

        brokerStat.getConnectionStat().incrConsumer();
    }

    @Override
    public void removeProducer(Producer producer) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(producer.getTopic());
        AppStat appStat = topicStat.getOrCreateAppStat(producer.getApp());

        if (!appStat.getConnectionStat().removeConnection(producer.getConnectionId())) {
            return;
        }

        appStat.getConnectionStat().removeConnection(producer.getConnectionId());
        appStat.getConnectionStat().decrProducer();
        appStat.getProducerStat().getConnectionStat().removeConnection(producer.getConnectionId());
        appStat.getProducerStat().getConnectionStat().decrProducer();

        topicStat.getConnectionStat().removeConnection(producer.getConnectionId());
        topicStat.getConnectionStat().decrProducer();

        brokerStat.getConnectionStat().decrProducer();
    }

    @Override
    public void removeConsumer(Consumer consumer) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(consumer.getTopic());
        AppStat appStat = topicStat.getOrCreateAppStat(consumer.getApp());

        if (!appStat.getConnectionStat().removeConnection(consumer.getConnectionId())) {
            return;
        }

        appStat.getConnectionStat().removeConnection(consumer.getConnectionId());
        appStat.getConnectionStat().decrConsumer();
        appStat.getConsumerStat().getConnectionStat().removeConnection(consumer.getConnectionId());
        appStat.getConsumerStat().getConnectionStat().decrConsumer();

        topicStat.getConnectionStat().removeConnection(consumer.getConnectionId());
        topicStat.getConnectionStat().decrConsumer();

        brokerStat.getConnectionStat().decrConsumer();
    }

    @Override
    public int getProducer(String topic, String app) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        return appStat.getProducerStat().getConnectionStat().getProducer();
    }

    @Override
    public int getConsumer(String topic, String app) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        return appStat.getProducerStat().getConnectionStat().getConsumer();
    }

    @Override
    public void addConnection(Connection connection) {
        InetSocketAddress address = IpUtil.toAddress(connection.getAddress());
        Client client = new Client();
        client.setConnectionId(connection.getId());
        client.setApp(connection.getApp());
        client.setLanguage(connection.getLanguage().name());
        client.setVersion(connection.getVersion());
        client.setSource(connection.getSource());
        client.setRegion(connection.getRegion());
        client.setNamespace(connection.getNamespace());
        client.setCreateTime(connection.getCreateTime());

        if (address != null && address.getAddress() != null) {
            client.setIp(address.getAddress().getHostAddress());
            client.setPort(address.getPort());
        }

        if (!brokerStat.getConnectionStat().addConnection(client)) {
            return;
        }
    }

    @Override
    public void removeConnection(Connection connection) {
        if (!brokerStat.getConnectionStat().removeConnection(connection.getId())) {
            return;
        }

        sessionManager.removeProducer(connection);
        sessionManager.removeConsumer(connection);
    }

    @Override
    public List<Client> getConnections(String topic, String app) {
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        return Lists.newArrayList(appStat.getConnectionStat().getConnectionMap().values());
    }

    @Override
    public void onEvent(SessionManager.SessionEvent event) {
        SessionManager.SessionEventType eventType = event.getType();
        switch (eventType) {
            case AddConnection:
                addConnection(event.getConnection());
                break;
            case RemoveConnection:
                removeConnection(event.getConnection());
            case AddProducer:
                addProducer(event.getProducer());
                break;
            case RemoveProducer:
                removeProducer(event.getProducer());
                break;
            case AddConsumer:
                addConsumer(event.getConsumer());
                break;
            case RemoveConsumer:
                removeConsumer(event.getConsumer());
                break;
        }
    }

    public BrokerStat getBrokerStat() {
        return brokerStat;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * 元数据监听
     */
    class MonitorMateDataListener implements EventListener<MetaEvent> {
        @Override
        public void onEvent(MetaEvent event) {
            switch (event.getEventType()) {
                case REMOVE_CONSUMER:
                    removeConsumer((RemoveConsumerEvent) event);
                    break;
                case REMOVE_PRODUCER:
                    removeProducer((RemoveProducerEvent) event);
                    break;
                case REMOVE_TOPIC:
                    removeTopic((RemoveTopicEvent) event);
                    break;
                case REMOVE_PARTITION_GROUP:
                    removePartitionGroup((RemovePartitionGroupEvent) event);
                    break;
                case UPDATE_PARTITION_GROUP:
                    updatePartitionGroup((UpdatePartitionGroupEvent) event);
                    break;
            }
        }

        private void removeConsumer(RemoveConsumerEvent removeConsumerEvent) {
            try {
                io.chubao.joyqueue.domain.Consumer consumer = removeConsumerEvent.getConsumer();
                TopicStat topicStat = brokerStat.getTopicStats().get(consumer.getTopic());
                if (topicStat == null) {
                    return;
                }
                AppStat appStat = topicStat.getAppStats().get(consumer.getApp());
                if (appStat != null) {
                    if (clusterManager.tryGetProducer(consumer.getTopic(), consumer.getApp()) == null) {
                        topicStat.getAppStats().remove(consumer.getApp());
                    } else {
                        appStat.getConsumerStat().clear();
                    }
                }
            } catch (Throwable th) {
                logger.error("listen remove consumer event exception, topic: {}, app: {}",
                        removeConsumerEvent.getConsumer().getTopic(), removeConsumerEvent.getConsumer().getApp(), th);
            }
        }

        private void removeProducer(RemoveProducerEvent removeProducerEvent) {
            try {
                io.chubao.joyqueue.domain.Producer producer = removeProducerEvent.getProducer();
                TopicStat topicStat = brokerStat.getTopicStats().get(producer.getTopic());
                if (topicStat == null) {
                    return;
                }
                AppStat appStat = topicStat.getAppStats().get(producer.getApp());
                if (appStat != null) {
                    if (clusterManager.tryGetConsumer(producer.getTopic(), producer.getApp()) == null) {
                        topicStat.getAppStats().remove(producer.getApp());
                    } else {
                        appStat.getProducerStat().clear();
                    }
                }
            } catch (Throwable th) {
                logger.error("listen remove producer event exception, topic: {}, app: {}",
                        removeProducerEvent.getProducer().getTopic(), removeProducerEvent.getProducer().getApp(), th);
            }
        }

        private void removeTopic(RemoveTopicEvent removeTopicEvent) {
            try {
                brokerStat.getTopicStats().remove(removeTopicEvent.getTopic().getName().getFullName());
            } catch (Throwable th) {
                logger.error("listen remove topic event exception, topic: {}", removeTopicEvent.getTopic(), th);
            }
        }

        private void removePartitionGroup(RemovePartitionGroupEvent removePartitionGroupEvent) {
            try {
                TopicStat topicStat = brokerStat.getTopicStats().get(removePartitionGroupEvent.getPartitionGroup().getTopic().getFullName());
                if (topicStat == null) {
                    return;
                }
                topicStat.removePartitionGroup(removePartitionGroupEvent.getPartitionGroup().getGroup());
            } catch (Throwable th) {
                logger.error("listen remove partition group event exception, topic: {}, partitionGroup: {}",
                        removePartitionGroupEvent.getPartitionGroup().getTopic(), removePartitionGroupEvent.getPartitionGroup().getGroup(), th);
            }
        }

        private void updatePartitionGroup(UpdatePartitionGroupEvent updatePartitionGroupEvent) {
            try {
                TopicStat topicStat = brokerStat.getTopicStats().get(updatePartitionGroupEvent.getTopic().getFullName());
                if (topicStat == null) {
                    return;
                }

                PartitionGroup oldPartitionGroup = updatePartitionGroupEvent.getOldPartitionGroup();
                PartitionGroup newPartitionGroup = updatePartitionGroupEvent.getNewPartitionGroup();

                for (Short partition : oldPartitionGroup.getPartitions()) {
                    if (newPartitionGroup.getPartitions().contains(partition)) {
                        continue;
                    }
                    topicStat.removePartition(partition);
                }
            } catch (Throwable th) {
                logger.error("listen update partition event exception, topic: {}, partitionGroup: {}.",
                        updatePartitionGroupEvent.getTopic(), updatePartitionGroupEvent.getNewPartitionGroup().getGroup(), th);
            }
        }

    }
}
