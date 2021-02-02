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
package org.joyqueue.broker.joyqueue0.handler;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.Joyqueue0Consts;
import org.joyqueue.broker.joyqueue0.command.AddProducer;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.GetProducerHealth;
import org.joyqueue.broker.joyqueue0.command.RemoveProducer;
import org.joyqueue.broker.joyqueue0.helper.VersionHelper;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.cluster.ClusterManager;
import org.joyqueue.broker.helper.SessionHelper;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.ConnectionId;
import org.joyqueue.network.session.Producer;
import org.joyqueue.network.session.ProducerId;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Types;
import org.joyqueue.network.transport.exception.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 生产者处理器
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/28
 */
public class ProducerHandler implements Joyqueue0CommandHandler, Types, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(ProducerHandler.class);

    private ClusterManager clusterManager;
    private SessionManager sessionManager;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.clusterManager = brokerContext.getClusterManager();
        this.sessionManager = brokerContext.getSessionManager();
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        Object payload = command.getPayload();
        if (payload instanceof AddProducer) {
            return addProducer(transport, command, (AddProducer) payload);
        } else if (payload instanceof RemoveProducer) {
            return removeProducer(transport, command, (RemoveProducer) payload);
        } else if (payload instanceof GetProducerHealth) {
            return getProducerHealth(transport, command, (GetProducerHealth) payload);
        } else {
            throw new TransportException.RequestErrorException(JoyQueueCode.CN_COMMAND_UNSUPPORTED.getMessage(payload.getClass()));
        }
    }

    protected Command addProducer(Transport transport, Command command, AddProducer addProducer) {
        TopicName topic = addProducer.getTopic();
        ProducerId producerId = addProducer.getProducerId();
        ConnectionId connectionId = producerId.getConnectionId();
        Connection connection = sessionManager.getConnectionById(connectionId.getConnectionId());
        // 连接不存在
        if (connection == null) {
            logger.warn("connection {} is not exists, topic: {}", connectionId.getConnectionId(), topic);
            return BooleanAck.build(JoyQueueCode.FW_CONNECTION_NOT_EXISTS.getCode(),
                    String.format("connection %s is not exists. topic:%s", connectionId.getConnectionId(), topic));
        }

        // Broker被禁用了或不能发送消息
        if (clusterManager.tryGetProducer(topic, connection.getApp()) == null) {
            logger.warn("addProducer failed, transport: {}, topic: {}, app: {}", transport, topic, connection.getApp());
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }

        TopicConfig topicConfig = clusterManager.getTopicConfig(topic);
        if (topicConfig == null) {
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }

        // 确认版本号
        if (!VersionHelper.checkVersion(topicConfig, connection.getApp(), connection.getVersion())) {
            return BooleanAck.build(JoyQueueCode.CT_LOW_VERSION.getCode(),
                    String.format("current client version %s less than minVersion %s for using broadcast or sequential!", connection.getVersion(), Joyqueue0Consts.MIN_SUPPORTED_VERSION_STR));
        }

        Producer producer = new Producer(producerId.getProducerId(), connectionId.getConnectionId(), topic.getFullName());
        producer.setType(Producer.ProducerType.JOYQUEUE0);
        producer.setApp(connection.getApp());
        sessionManager.addProducer(producer);

        return BooleanAck.build();
    }

    protected Command removeProducer(Transport transport, Command command, RemoveProducer removeProducer) {
        ProducerId producerId = removeProducer.getProducerId();
        Producer producer = sessionManager.getProducerById(producerId.getProducerId());
        if (producer != null) {
            sessionManager.removeProducer(producer.getId());
        }
        return BooleanAck.build();
    }

    protected Command getProducerHealth(Transport transport, Command command, GetProducerHealth getProducerHealth) {
        Connection connection = SessionHelper.getConnection(transport);
        if (connection == null) {
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }
        if (!clusterManager.checkWritable(TopicName.parse(getProducerHealth.getTopic()), getProducerHealth.getApp(), null).isSuccess()) {
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }
        if (sessionManager.getProducerById(getProducerHealth.getProducerId()) == null) {
            return BooleanAck.build(JoyQueueCode.FW_PRODUCER_NOT_EXISTS);
        }
        return BooleanAck.build();
    }

    @Override
    public int[] types() {
        return new int[]{Joyqueue0CommandType.ADD_PRODUCER.getCode(), Joyqueue0CommandType.REMOVE_PRODUCER.getCode(), Joyqueue0CommandType.GET_PRODUCER_HEALTH.getCode()};
    }
}