/**
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

import com.google.common.collect.Lists;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.RetryMessage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.buffer.Serializer;
import org.joyqueue.broker.cluster.ClusterManager;
import org.joyqueue.broker.consumer.Consume;
import org.joyqueue.broker.consumer.MessageConvertSupport;
import org.joyqueue.broker.consumer.model.PullResult;
import org.joyqueue.broker.helper.SessionHelper;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.domain.Partition;
import org.joyqueue.domain.TopicName;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.MessageLocation;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.Consumer;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.server.retry.api.MessageRetry;
import org.joyqueue.server.retry.model.RetryMessageModel;
import org.joyqueue.toolkit.lang.Pair;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 重试处理
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/9/10
 */
public class RetryMessageHandler implements Joyqueue0CommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(RetryMessageHandler.class);

    private SessionManager sessionManager;
    private Consume consume;
    private MessageRetry<Long> retryManager;
    private ClusterManager clusterManager;
    private MessageConvertSupport messageConvertSupport;
    private BrokerContext brokerContext;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.sessionManager = brokerContext.getSessionManager();
        this.consume = brokerContext.getConsume();
        this.retryManager = brokerContext.getRetryManager();
        this.clusterManager = brokerContext.getClusterManager();
        this.messageConvertSupport = brokerContext.getMessageConvertSupport();
        this.brokerContext = brokerContext;
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        long startTime = SystemClock.now();

        RetryMessage retryMessage = (RetryMessage) command.getPayload();
        List<MessageLocation> messageLocations = retryMessage.getLocations();
        if (CollectionUtils.isEmpty(messageLocations)) {
            return BooleanAck.build();
        }

        Connection connection = SessionHelper.getConnection(transport);
        if (connection == null) {
            logger.warn("connection is not exists. transport: {}", transport);
            return BooleanAck.build(JoyQueueCode.FW_CONNECTION_NOT_EXISTS.getCode(), "connection is not exists");
        }

        Consumer consumer = sessionManager.getConsumerById(retryMessage.getConsumerId().getConsumerId());
        if (consumer == null) {
            logger.warn("consumer session is not exist, transport: {}, consumerId: {}", transport, retryMessage.getConsumerId().getConsumerId());
            return BooleanAck.build(JoyQueueCode.FW_CONSUMER_NOT_EXISTS.getCode(), "consumer is not exists");
        }

        try {
            org.joyqueue.domain.Consumer consumerConfig = clusterManager.getConsumer(TopicName.parse(consumer.getTopic()), consumer.getApp());
            Boolean retry = (consumerConfig.getConsumerPolicy() == null ? brokerContext.getConsumerPolicy() : consumerConfig.getConsumerPolicy()).getRetry();
            boolean isRetry = retry == null ? false : retry.booleanValue();

            /**
             * 没有开启重试服务，直接返回，不进重试库，不写ACK
             */
            if (!isRetry) {
                /**
                 * 释放分区占用
                 */
                consume.releasePartition(consumer.getTopic(), consumer.getApp(), messageLocations.get(0).getPartition());
                return BooleanAck.build(JoyQueueCode.SUCCESS);
            }
        } catch (JoyQueueException e) {
            logger.error("get consumer from clusterManager error.", e);
        }

        try {
            List<MessageLocation> retryLocations = Lists.newLinkedList();

            for (MessageLocation messageLocation : messageLocations) {
                if (messageLocation.getPartition() == RetryMessage.RETRY_PARTITION_ID) {
                    retryLocations.add(messageLocation);
                }
            }

            if (CollectionUtils.isNotEmpty(retryLocations)) {
                if (StringUtils.isNotBlank(retryMessage.getException())) {
                    retryManager.retryError(consumer.getTopic(), consumer.getApp(), buildMessageIds(messageLocations));
                } else {
                    retryManager.retrySuccess(consumer.getTopic(), consumer.getApp(), buildMessageIds(messageLocations));
                }
            } else {
                try {
                    // 获取异常消息
                    List<Pair<BrokerMessage, byte[]>> brokerMessages = readBrokerMessage(consumer, messageLocations);
                    // 转换成RetryMessageModel
                    List<RetryMessageModel> retryMessageModels = generateRetryMessage(consumer, brokerMessages, retryMessage);
                    // 添加重试消息
                    retryManager.addRetry(retryMessageModels);
                } catch (JoyQueueException e) {
                    if (e.getCode() == JoyQueueCode.RETRY_TOKEN_LIMIT.getCode()) {
                        logger.warn("add retry limited, transport: {}, topic: {}, app: {}", connection.getTransport(), consumer.getTopic(), consumer.getApp());
                    } else {
                        logger.error("add retry exception, transport: {}, topic: {}, app: {}", connection.getTransport(), consumer.getTopic(), consumer.getApp(), e);
                    }
                    consume.releasePartition(consumer.getTopic(), consumer.getApp(), messageLocations.get(0).getPartition());
                    return BooleanAck.build(JoyQueueCode.FW_CONSUMER_ACK_FAIL);
                } catch (Exception e) {
                    logger.error("add retry exception, transport: {}, topic: {}, app: {}", connection.getTransport(), consumer.getTopic(), consumer.getApp(), e);
                    consume.releasePartition(consumer.getTopic(), consumer.getApp(), messageLocations.get(0).getPartition());
                    return BooleanAck.build(JoyQueueCode.FW_CONSUMER_ACK_FAIL);
                }
                // 应答
                consume.acknowledge(messageLocations.toArray(new MessageLocation[0]), consumer, connection, false);
            }

            return BooleanAck.build();
        } catch (JoyQueueException e) {
            logger.error("putRetry exception, transport: {}, messageLocations: {}, consumer: {}", transport, messageLocations, consumer, e);
            return BooleanAck.build(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("putRetry exception, transport: {}, messageLocations: {}, consumer: {}", transport, messageLocations, consumer, e);
            return BooleanAck.build(JoyQueueCode.CN_UNKNOWN_ERROR.getCode(), JoyQueueCode.CN_UNKNOWN_ERROR.getMessage());
        }
    }

    protected Long[] buildMessageIds(List<MessageLocation> messageLocations) {
        Long[] result = new Long[messageLocations.size()];
        for (int i = 0; i < messageLocations.size(); i++) {
            result[i] = messageLocations.get(i).getIndex();
        }
        return result;
    }

    protected List<RetryMessageModel> generateRetryMessage(Consumer consumer, List<Pair<BrokerMessage, byte[]/* BrokerMessage 序列化后的字节数组 */>> brokerMessages, RetryMessage retryMessage) {
        List<RetryMessageModel> result = new LinkedList<>();
        for (Pair<BrokerMessage, byte[]> pair : brokerMessages) {
            RetryMessageModel model = new RetryMessageModel();
            BrokerMessage brokerMessage = pair.getKey();
            model.setBusinessId(brokerMessage.getBusinessId());
            model.setTopic(consumer.getTopic());
            model.setApp(consumer.getApp());
            model.setPartition(Partition.RETRY_PARTITION_ID);
            model.setIndex(brokerMessage.getMsgIndexNo());
            model.setBrokerMessage(pair.getValue());

            String exception = retryMessage.getException();
            if (StringUtils.isNotEmpty(exception)) {
                byte[] exceptionBytes =  exception.getBytes(Charset.forName("UTF-8"));
                model.setException(exceptionBytes);
            }
            model.setSendTime(brokerMessage.getStartTime());

            result.add(model);
        }

        return result;
    }

    private List<Pair<BrokerMessage, byte[]>> readBrokerMessage(Consumer consumer, List<MessageLocation> messageLocations) throws JoyQueueException {
        List<Pair<BrokerMessage, byte[]>> brokerMessages = new ArrayList<>(messageLocations.size());
        for (MessageLocation messageLocation : messageLocations) {
            short partition = messageLocation.getPartition();
            long index = messageLocation.getIndex();
            PullResult pullResult = consume.getMessage(consumer, partition, index, 1);
            List<ByteBuffer> buffers = pullResult.getBuffers();
            if (buffers.size() == 1) {
                BrokerMessage brokerMessage;
                ByteBuffer buffer = buffers.get(0);
                try {
                    brokerMessage = Serializer.readBrokerMessage(buffer);
                } catch (Exception e) {
                    logger.error("readBrokerMessage exception, consumer: {}", consumer, e);
                    throw new JoyQueueException(JoyQueueCode.SE_IO_ERROR, e);
                }
                brokerMessages.add(new Pair<>(brokerMessage, buffer.array()));
            }
        }
        return brokerMessages;
    }

    /**
     * 将BrokerMessage转换成RByteBuffer
     *
     * @param brokerMessage
     * @return
     * @throws JoyQueueException
     */
    private ByteBuffer convertBrokerMessage2RByteBuffer(BrokerMessage brokerMessage) throws JoyQueueException {
        int msgSize = Serializer.sizeOf(brokerMessage);
        ByteBuffer allocate = ByteBuffer.allocate(msgSize);
        try {
            Serializer.write(brokerMessage, allocate, msgSize);
        } catch (Exception e) {
            logger.error("Serialize message error! topic:{},app:{}", brokerMessage.getTopic(), brokerMessage.getApp(), e);
            throw new JoyQueueException(JoyQueueCode.SE_SERIALIZER_ERROR);
        }
        return allocate;
    }


    @Override
    public int type() {
        return Joyqueue0CommandType.RETRY_MESSAGE.getCode();
    }
}