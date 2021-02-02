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
import org.joyqueue.broker.joyqueue0.command.AddConsumer;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.GetConsumerHealth;
import org.joyqueue.broker.joyqueue0.command.RemoveConsumer;
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
import org.joyqueue.network.session.Consumer;
import org.joyqueue.network.session.ConsumerId;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Types;
import org.joyqueue.network.transport.exception.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消费者处理器
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/28
 */
public class ConsumerHandler implements Joyqueue0CommandHandler, Types, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(ConsumerHandler.class);

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
        if (payload instanceof AddConsumer) {
            return addConsumer(transport, command, (AddConsumer) payload);
        } else if (payload instanceof RemoveConsumer) {
            return removeConsumer(transport, command, (RemoveConsumer) payload);
        } else if (payload instanceof GetConsumerHealth) {
            return getConsumerHealth(transport, command, (GetConsumerHealth) payload);
        } else {
            throw new TransportException.RequestErrorException(JoyQueueCode.CN_COMMAND_UNSUPPORTED.getMessage(payload.getClass()));
        }
    }

    protected Command addConsumer(Transport transport, Command command, AddConsumer addConsumer) {
        TopicName topic = addConsumer.getTopic();
        ConsumerId consumerId = addConsumer.getConsumerId();
        ConnectionId connectionId = consumerId.getConnectionId();
        Connection connection = sessionManager.getConnectionById(connectionId.getConnectionId());

        // 连接不存在
        if (connection == null) {
            logger.warn("connection {} is not exists. topic: {}", connectionId.getConnectionId(), topic);
            return BooleanAck.build(JoyQueueCode.FW_CONNECTION_NOT_EXISTS.getCode(),
                    String.format("connection %d is not exists. topic:%s", connectionId.getConnectionId(), topic));
        }

        // 验证权限
        if (clusterManager.tryGetConsumer(topic, connection.getApp()) == null) {
            logger.warn("addConsumer failed, transport: {}, topic: {}, app: {}", transport, topic, connection.getApp());
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

        Consumer consumer = new Consumer(consumerId.getConsumerId(), connectionId.getConnectionId(), topic.getFullName(), addConsumer.getSelector());
        consumer.setApp(connection.getApp());
        consumer.setType(Consumer.ConsumeType.JOYQUEUE0);
        sessionManager.addConsumer(consumer);
        return BooleanAck.build();
    }

    protected Command removeConsumer(Transport transport, Command command, RemoveConsumer removeConsumer) {
        ConsumerId consumerId = removeConsumer.getConsumerId();
        Consumer consumer = sessionManager.getConsumerById(consumerId.getConsumerId());
        if (consumer != null) {
            sessionManager.removeConsumer(consumer.getId());
        }
        return BooleanAck.build();
    }

    protected Command getConsumerHealth(Transport transport, Command command, GetConsumerHealth getConsumerHealth) {
        Connection connection = SessionHelper.getConnection(transport);
        if (connection == null) {
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }
        if (!clusterManager.checkReadable(TopicName.parse(getConsumerHealth.getTopic()), getConsumerHealth.getApp(), null).isSuccess()) {
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }
        if (sessionManager.getConsumerById(getConsumerHealth.getConsumerId()) == null) {
            return BooleanAck.build(JoyQueueCode.FW_CONSUMER_NOT_EXISTS);
        }
        return BooleanAck.build();
    }

    @Override
    public int[] types() {
        return new int[]{Joyqueue0CommandType.ADD_CONSUMER.getCode(), Joyqueue0CommandType.REMOVE_CONSUMER.getCode(), Joyqueue0CommandType.GET_CONSUMER_HEALTH.getCode()};
    }
}