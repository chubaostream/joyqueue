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
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.PutMessage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.cluster.ClusterManager;
import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.broker.helper.SessionHelper;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.broker.network.traffic.Traffic;
import org.joyqueue.broker.producer.Produce;
import org.joyqueue.broker.producer.ProduceConfig;
import org.joyqueue.domain.Topic;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.SourceType;
import org.joyqueue.network.protocol.annotation.ProduceHandler;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.Producer;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.response.BooleanResponse;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 发送消息处理
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/28
 */
@ProduceHandler
public class PutMessageHandler implements Joyqueue0CommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(PutMessageHandler.class);

    private BrokerConfig brokerConfig;
    private ProduceConfig produceConfig;
    private ClusterManager clusterManager;
    private SessionManager sessionManager;
    private Produce produce;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerConfig = brokerContext.getBrokerConfig();
        this.produceConfig = new ProduceConfig(brokerContext.getPropertySupplier());
        this.clusterManager = brokerContext.getClusterManager();
        this.sessionManager = brokerContext.getSessionManager();
        this.produce = brokerContext.getProduce();
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.PUT_MESSAGE.getCode();
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        PutMessage putMessage = (PutMessage) command.getPayload();
        List<BrokerMessage> messages = putMessage.getMessages();
        if (CollectionUtils.isEmpty(messages)) {
            return BooleanAck.build(JoyQueueCode.CN_PARAM_ERROR);
        }

        Connection connection = SessionHelper.getConnection(transport);
        Producer producer = sessionManager.getProducerById(putMessage.getProducerId().getProducerId());
        if (connection == null || producer == null) {
            logger.warn("producer session is not exist, transport: {}, topic: {}", transport, putMessage.getMessages().get(0).getTopic());
            return BooleanAck.build(JoyQueueCode.FW_PRODUCER_NOT_EXISTS);
        }

        Traffic traffic = new Traffic(producer.getApp());
        putMessage.setTraffic(traffic);

        BooleanResponse checkResult = clusterManager.checkWritable(TopicName.parse(producer.getTopic()), producer.getApp(), connection.getHost());
        if (!checkResult.isSuccess()) {
            logger.warn("checkWritable failed, transport: {}, topic: {}, app: {}, code: {}", transport, producer.getTopic(), producer.getApp(), checkResult.getJoyQueueCode());
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }

        try {
            // 校验和填充消息
            checkAndFillMessages(connection, producer, transport, traffic, putMessage, messages);
        } catch (JoyQueueException e) {
            logger.warn("checkMessage error, transport: {}, producer: {}, topic: {}, app: {}", transport, producer, producer.getTopic(), producer.getApp(), e);
            return BooleanAck.build(e.getCode(), e.getMessage());
        }

        try {
            putMessage.setMessages(null); // help gc
            produce.putMessageAsync(producer, messages, command.getHeader().getQosLevel(), (result) -> {
                if (result.getCode().equals(JoyQueueCode.SUCCESS)) {
                    transport.acknowledge(command, BooleanAck.build());
                } else {
                    logger.error("write message error, producer: {}, transport: {}, code: {}", producer, transport, result.getCode());
                    transport.acknowledge(command, BooleanAck.build(JoyQueueCode.FW_PUT_MESSAGE_ERROR));
                }
            });

            return null;
        } catch (Exception e) {
            logger.error("putMessage exception, transport: {}, producer: {}", transport, producer, e);
            return BooleanAck.build(JoyQueueCode.CN_UNKNOWN_ERROR);
        }
    }

    private void checkAndFillMessages(Connection connection, Producer producer, Transport transport, Traffic traffic, PutMessage putMessage, List<BrokerMessage> messages) throws JoyQueueException {
        String topic = producer.getTopic();
        String app = producer.getApp();
        byte[] clientAddress = connection.getAddress();
        long now = SystemClock.getInstance().now();
        TopicConfig topicConfig = clusterManager.getTopicConfig(TopicName.parse(producer.getTopic()));

        for (BrokerMessage message : messages) {
            // 确保主题一致
            if (!message.getTopic().equals(topic)) {
                throw new JoyQueueException(JoyQueueCode.FW_PUT_MESSAGE_ERROR, "the put message command has multi-topic");
            }
            // 校验消息体
            if (message.getByteBody() == null) {
                throw new JoyQueueException(JoyQueueCode.FW_PUT_MESSAGE_ERROR, "body is required, it must not be null");
            }
            if (StringUtils.length(message.getBusinessId()) > produceConfig.getBusinessIdLength()) {
                throw new JoyQueueException(JoyQueueCode.FW_PUT_MESSAGE_ERROR, "message businessId out of rage");
            }
            if (ArrayUtils.getLength(message.getByteBody()) > produceConfig.getBodyLength()) {
                throw new JoyQueueException(JoyQueueCode.CN_PARAM_ERROR, "message body out of rage");
            }
            if (putMessage.getQueueId() > 0 && topicConfig.getType().equals(Topic.Type.SEQUENTIAL)) {
                message.setPartition((short) 0);
//                message.setPartition(putMessage.getQueueId());
            }
            message.setApp(app);
            message.setClientIp(clientAddress);
            message.setStartTime(now);
            message.setSource(SourceType.JOYQUEUE0.getValue());
            traffic.record(topic, message.getByteBody().length, 1);
        }
    }
}