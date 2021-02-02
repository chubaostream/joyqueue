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

import com.google.common.collect.Lists;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.Joyqueue0Consts;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.GetMessage;
import org.joyqueue.broker.joyqueue0.command.GetMessageAck;
import org.joyqueue.broker.joyqueue0.config.Joyqueue0Config;
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
import org.joyqueue.broker.network.traffic.Traffic;
import org.joyqueue.broker.polling.LongPolling;
import org.joyqueue.broker.polling.LongPollingCallback;
import org.joyqueue.broker.polling.LongPollingManager;
import org.joyqueue.broker.producer.ProduceConfig;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.TopicName;
import org.joyqueue.domain.TopicType;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.SourceType;
import org.joyqueue.network.protocol.annotation.FetchHandler;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.Consumer;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.response.BooleanResponse;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * 拉取消息处理
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/28
 */
@FetchHandler
public class GetMessageHandler implements Joyqueue0CommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(GetMessageHandler.class);

    private ClusterManager clusterManager;
    private SessionManager sessionManager;
    private Consume consume;
    private LongPollingManager longPollingManager;
    private PropertySupplier propertySupplier;
    private MessageConvertSupport messageConvertSupport;
    private BrokerContext brokerContext;
    private Joyqueue0Config config;
    private ProduceConfig produceConfig;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.clusterManager = brokerContext.getClusterManager();
        this.sessionManager = brokerContext.getSessionManager();
        this.consume = brokerContext.getConsume();
        this.propertySupplier = brokerContext.getPropertySupplier();
        this.messageConvertSupport = brokerContext.getMessageConvertSupport();
        this.brokerContext = brokerContext;
        this.config = new Joyqueue0Config(brokerContext.getPropertySupplier());
        this.produceConfig = new ProduceConfig(brokerContext.getPropertySupplier());
        initLongPollingManager(sessionManager, clusterManager, consume);
    }

    private void initLongPollingManager(SessionManager sessionManager, ClusterManager clusterManager, Consume consume) {
        this.longPollingManager = new LongPollingManager(sessionManager, clusterManager, consume, propertySupplier);
        try {
            longPollingManager.start();
        } catch (Exception e) {
            logger.error("Init LongPollingManager error, {}", e);
            throw new RuntimeException("Init LongPollingManager Exception");
        }
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        GetMessage getMessage = (GetMessage) command.getPayload();
        Connection connection = SessionHelper.getConnection(transport);
        Consumer consumer = sessionManager.getConsumerById(getMessage.getConsumerId().getConsumerId());

        if (connection == null || consumer == null) {
            logger.warn("consumer session is not exist, transport: {}, topic: {}", transport, getMessage.getTopic());
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }

        Traffic traffic = new Traffic(consumer.getApp());

        // 是否可读
        BooleanResponse checkResult = clusterManager.checkReadable(TopicName.parse(consumer.getTopic()), consumer.getApp(), connection.getHost());
        if (!checkResult.isSuccess()) {
            logger.warn("checkReadable failed, transport: {}, topic: {}, app: {}, code: {}", transport, consumer.getTopic(), consumer.getApp(), checkResult.getJoyQueueCode());
            if (checkResult.getJoyQueueCode().equals(JoyQueueCode.FW_FETCH_TOPIC_MESSAGE_PAUSED) || checkResult.getJoyQueueCode().equals(JoyQueueCode.FW_GET_MESSAGE_APP_CLIENT_IP_NOT_READ)) {
                return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
            }
            return BooleanAck.build(JoyQueueCode.CN_NO_PERMISSION);
        }

        try {
            PullResult pullResult = getMessage(transport, command, consumer, traffic, getMessage);
            if (pullResult == null) {
                return null;
            }
            if (!pullResult.getCode().equals(JoyQueueCode.SUCCESS)) {
                logger.error("getMessage error, code: {}, transport: {}, consumer: {}", pullResult.getCode(), transport, consumer);
            }
            return buildGetMessageAck(transport, traffic, consumer, getMessage, pullResult);
        } catch (JoyQueueException e) {
            logger.error("getMessage exception, transport: {}, consumer: {}", transport, consumer, e);
            return GetMessageAck.build(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("getMessage exception, transport: {}, consumer: {}", transport, consumer, e);
            return GetMessageAck.build(JoyQueueCode.CN_UNKNOWN_ERROR.getCode(), JoyQueueCode.CN_UNKNOWN_ERROR.getMessage());
        }
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_MESSAGE.getCode();
    }

    protected PullResult getMessage(Transport transport, Command command, Consumer consumer, Traffic traffic, GetMessage getMessage) throws Exception {
        org.joyqueue.domain.Consumer topicConsumer = clusterManager.getConsumer(getMessage.getTopic(), consumer.getApp());
        if (topicConsumer.getTopicType().equals(TopicType.TOPIC)) {
            return doGetMessage(transport, command, consumer, traffic, getMessage);
        } else if (topicConsumer.getTopicType().equals(TopicType.BROADCAST)) {
            return doGetBroadcastMessage(transport, command, consumer, traffic, getMessage, topicConsumer);
        } else {
            logger.warn("unsupported topic type, topic: {}, app: {}, type: {}", getMessage.getTopic(), consumer.getApp(), topicConsumer.getTopicType());
            return null;
        }
    }

    protected PullResult doGetMessage(Transport transport, Command command, Consumer consumer, Traffic traffic, GetMessage getMessage) throws Exception {
        PullResult pullResult = consume.getMessage(consumer, (int) getMessage.getCount(), getMessage.getAckTimeout());
        // 判断是否需要长轮询（1.没有拉到消息；2.长轮询时间大于0；3.配置支持长轮询；4.加入长轮询队列成功）
        if (pullResult.count() <= 0 && getMessage.getLongPull() > 0 && clusterManager.isNeedLongPull(consumer.getTopic())
                && longPollingManager.suspend(new LongPolling(consumer, getMessage.getCount(), getMessage.getAckTimeout(), getMessage.getLongPull(), new LongPollingCallback() {

            @Override
            public void onSuccess(Consumer consumer, PullResult pullResult) throws TransportException {
                logger.debug("getMessage longPolling success, consumer: {}, messageSize: {}", consumer, pullResult.count());
                Command getMessageAck = buildGetMessageAck(transport, traffic, consumer, getMessage, pullResult);
                transport.acknowledge(command, getMessageAck);
            }

            @Override
            public void onExpire(Consumer consumer) throws TransportException {
                logger.debug("getMessage longPolling expire, consumer: {}", consumer);
                transport.acknowledge(command, GetMessageAck.build());
            }

            @Override
            public void onException(Consumer consumer, Throwable throwable) throws TransportException {
                logger.error("getMessage longPolling exception, transport: {}, consumer: {}", transport, consumer, throwable);
                if (throwable instanceof JoyQueueException) {
                    JoyQueueException e = (JoyQueueException) throwable;
                    transport.acknowledge(command, GetMessageAck.build(e.getCode(), e.getMessage()));
                } else {
                    transport.acknowledge(command, GetMessageAck.build(JoyQueueCode.CN_UNKNOWN_ERROR.getCode(), JoyQueueCode.CN_UNKNOWN_ERROR.getMessage()));
                }
            }
        }))) {
            return null;
        } else {
            return pullResult;
        }
    }

    protected PullResult doGetBroadcastMessage(Transport transport, Command command, Consumer consumer, Traffic traffic,
                                               GetMessage getMessage, org.joyqueue.domain.Consumer topicConsumer) throws Exception {
        List<PartitionGroup> partitionGroups = clusterManager.getTopicConfig(getMessage.getTopic()).fetchTopicPartitionGroupsByBrokerId(clusterManager.getBrokerId());
        if (partitionGroups.size() > 1) {
            logger.warn("broadcast consumer error, partitionGroup is {}, topic: {}, app: {}", partitionGroups.size(), getMessage.getTopic(), consumer.getApp());
            PullResult pullResult = new PullResult(consumer, (short) -1, Collections.emptyList());
            pullResult.setCode(JoyQueueCode.FW_GET_MESSAGE_ERROR);
            return pullResult;
        }

        long index = getMessage.getOffset();
        short partition = (short) CollectionUtils.get(partitionGroups.get(0).getPartitions(), getMessage.getQueueId() - 1);
        long minIndex = consume.getMinIndex(consumer, partition);
        long maxIndex = consume.getMaxIndex(consumer, partition);
        int count = (getMessage.getCount() != 0 ? getMessage.getCount() :
                (topicConsumer.getConsumerPolicy() != null ? topicConsumer.getConsumerPolicy().getBatchSize() : brokerContext.getConsumerPolicy().getBatchSize()));

        if (index == -1) {
            index = minIndex;
        } else {
            index /= Joyqueue0Consts.INDEX_LENGTH;
            if (index < minIndex || index > maxIndex) {
                logger.warn("get message exception, index out of range, transport: {}, consumer: {}, partition: {}, offset: {}, minOffset: {}, maxOffset: {}",
                        transport, consumer, partition, index, minIndex, maxIndex);
                index = maxIndex;
            }
        }

        // 广播消息特殊处理，partition + 1，index * 22;
        PullResult pullResult = consume.getMessage(consumer, partition, index, count);
        if (CollectionUtils.isNotEmpty(pullResult.getBuffers())) {
            List<BrokerMessage> brokerMessages = Lists.newLinkedList();
            List<ByteBuffer> buffers = Lists.newLinkedList();

            for (ByteBuffer buffer : pullResult.getBuffers()) {
                try {
                    BrokerMessage brokerMessage = Serializer.readBrokerMessage(buffer);
                    brokerMessage.setTopic(pullResult.getTopic());
                    brokerMessage.setApp(pullResult.getApp());
                    brokerMessages.add(brokerMessage);
                } catch (Exception e) {
                    logger.error("read broadcast message exception, transport: {}, getMessage: {}, consumer: {}", transport, getMessage, consumer, e);
                }
            }

            brokerMessages = messageConvertSupport.convert(brokerMessages, SourceType.JOYQUEUE0.getValue());

            for (BrokerMessage brokerMessage : brokerMessages) {
                brokerMessage.setPartition((short) (brokerMessage.getPartition() + 1));
                brokerMessage.setMsgIndexNo(brokerMessage.getMsgIndexNo() * Joyqueue0Consts.INDEX_LENGTH);
                brokerMessage.setSource(SourceType.JOYQUEUE0.getValue());

                int size = Serializer.sizeOf(brokerMessage);
                ByteBuffer allocate = ByteBuffer.allocate(size);
                Serializer.write(brokerMessage, allocate, size);
                buffers.add(allocate);
            }
            pullResult.setBuffers(buffers);
        }

        return pullResult;
    }

    protected Command buildGetMessageAck(Transport transport, Traffic traffic, Consumer consumer, GetMessage getMessage, PullResult pullResult) {
        List<ByteBuffer> buffers = pullResult.getBuffers();
        List<BrokerMessage> brokerMessages = Lists.newArrayListWithCapacity(buffers.size());

        for (ByteBuffer buffer : buffers) {
            try {
                BrokerMessage brokerMessage = Serializer.readBrokerMessage(buffer);
                brokerMessage.setTopic(pullResult.getTopic());
                brokerMessage.setApp(pullResult.getApp());
                traffic.record(consumer.getTopic(), brokerMessage.getByteBody().length, 1);
                brokerMessages.add(brokerMessage);
            } catch (Exception e) {
                logger.error("read broker message exception, transport: {}, getMessage: {}, consumer: {}", transport, getMessage, consumer, e);
                return BooleanAck.build(JoyQueueCode.FW_GET_MESSAGE_ERROR);
            }
        }

        brokerMessages = messageConvertSupport.convert(brokerMessages, SourceType.JOYQUEUE0.getValue());

        if (CollectionUtils.isNotEmpty(brokerMessages) && config.getMessageBusinessIdRewrite(consumer.getTopic())) {
            for (BrokerMessage brokerMessage : brokerMessages) {
                if (StringUtils.isBlank(brokerMessage.getBusinessId())) {
                    continue;
                }
                int businessIdLength = brokerMessage.getBusinessId().getBytes().length;
                if (businessIdLength > produceConfig.getBusinessIdLength()) {
                    brokerMessage.setBusinessId(brokerMessage.getBusinessId().substring(0, produceConfig.getBusinessIdLength()));
                }
            }
        }

        GetMessageAck getMessageAck = new GetMessageAck();
        getMessageAck.setTraffic(traffic);
        getMessageAck.setMessages(brokerMessages);
        return new Command(getMessageAck);
    }
}