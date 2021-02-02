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
package org.joyqueue.broker.monitor;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.broker.cluster.ClusterManager;
import org.joyqueue.broker.election.ElectionEvent;
import org.joyqueue.broker.election.ElectionNode;
import org.joyqueue.broker.election.TopicPartitionGroup;
import org.joyqueue.broker.monitor.config.BrokerMonitorConfig;
import org.joyqueue.broker.monitor.stat.AppStat;
import org.joyqueue.broker.monitor.stat.BrokerStat;
import org.joyqueue.broker.monitor.stat.ConsumerStat;
import org.joyqueue.broker.monitor.stat.ElectionEventStat;
import org.joyqueue.broker.monitor.stat.PartitionGroupStat;
import org.joyqueue.broker.monitor.stat.ProducerStat;
import org.joyqueue.broker.monitor.stat.ReplicationStat;
import org.joyqueue.broker.monitor.stat.TopicStat;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.event.MetaEvent;
import org.joyqueue.monitor.Client;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.Consumer;
import org.joyqueue.network.session.Producer;
import org.joyqueue.nsr.event.RemoveConsumerEvent;
import org.joyqueue.nsr.event.RemovePartitionGroupEvent;
import org.joyqueue.nsr.event.RemoveProducerEvent;
import org.joyqueue.nsr.event.RemoveTopicEvent;
import org.joyqueue.nsr.event.UpdatePartitionGroupEvent;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.network.IpUtil;
import org.joyqueue.toolkit.service.Service;
import org.joyqueue.toolkit.time.SystemClock;
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
            TopicConfig topicConfig = clusterManager.getTopicConfig(topic);
            if (topicConfig == null || !topicConfig.isReplica(clusterManager.getBrokerId())) {
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
        consumerPartitionGroupStat.getOrCreatePartitionStat(partition).lastPullTime(SystemClock.now());

//            partitionGroupStat.getDeQueueStat().mark(time, size, count);
//            partitionStat.getDeQueueStat().mark(time, size, count);
        brokerStat.getDeQueueStat().mark(time, size, count);
    }

    @Override
    public void onAckMessage(String topic, String app, int partitionGroup, short partition) {
        if (!config.isEnable()) {
            return;
        }
        brokerStat.getOrCreateTopicStat(topic)
                .getOrCreateAppStat(app)
                .getConsumerStat()
                .getOrCreatePartitionGroupStat(partitionGroup)
                .getOrCreatePartitionStat(partition)
                .lastAckTime(SystemClock.now());
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
    public void onReplicaStateChange(String topic, int partitionGroup, ElectionNode.State newState) {
        if (!config.isEnable()) {
            return;
        }
        ReplicationStat replicationStat = brokerStat.getOrCreateTopicStat(topic).getOrCreatePartitionGroupStat(partitionGroup).getReplicationStat();
        replicationStat.getStat().setState(newState);
        replicationStat.getStat().setTimestamp(SystemClock.now());
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
        TopicStat topicStat = brokerStat.getOrCreateTopicStat(topic);
        AppStat appStat = topicStat.getOrCreateAppStat(app);
        appStat.getConsumerStat().getRetryStat().getTotal().mark(time, count);
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
    public void onOffsetReset(String topic, String app, long count) {
        if (!config.isEnable()) {
            return;
        }
        getConsumerStat(topic, app).getOffsetResetStat().getCount().mark(count);
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
        client.setAuth(connection.isAuth());

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



    public class ElectionListener implements EventListener<ElectionEvent>{

        @Override
        public void onEvent(ElectionEvent event) {
            // event.
            TopicPartitionGroup partitionGroup= event.getTopicPartitionGroup();
            if(partitionGroup!=null) {
                PartitionGroupStat partitionGroupStat= brokerStat.getOrCreateTopicStat(partitionGroup.getTopic()).getOrCreatePartitionGroupStat(partitionGroup.getPartitionGroupId());
                ElectionEventStat electionStat= partitionGroupStat.getElectionEventStat();
                electionStat.setState(event.getEventType());
                electionStat.setTerm(event.getTerm());
                electionStat.setTimestamp(SystemClock.now());
            }
        }
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
                org.joyqueue.domain.Consumer consumer = removeConsumerEvent.getConsumer();
                TopicStat topicStat = brokerStat.getTopicStats().get(consumer.getTopic().getFullName());
                if (topicStat == null) {
                    return;
                }
                AppStat appStat = topicStat.getAppStats().get(consumer.getApp());
                if (appStat != null) {
                    appStat.getConsumerStat().clear();
                    if (clusterManager.tryGetProducer(consumer.getTopic(), consumer.getApp()) == null) {
                        topicStat.getAppStats().remove(consumer.getApp());
                    }
                }
            } catch (Throwable th) {
                logger.error("listen remove consumer event exception, topic: {}, app: {}",
                        removeConsumerEvent.getConsumer().getTopic(), removeConsumerEvent.getConsumer().getApp(), th);
            }
        }

        private void removeProducer(RemoveProducerEvent removeProducerEvent) {
            try {
                org.joyqueue.domain.Producer producer = removeProducerEvent.getProducer();
                TopicStat topicStat = brokerStat.getTopicStats().get(producer.getTopic().getFullName());
                if (topicStat == null) {
                    return;
                }
                AppStat appStat = topicStat.getAppStats().get(producer.getApp());
                if (appStat != null) {
                    appStat.getProducerStat().clear();
                    if (clusterManager.tryGetConsumer(producer.getTopic(), producer.getApp()) == null) {
                        topicStat.getAppStats().remove(producer.getApp());
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
