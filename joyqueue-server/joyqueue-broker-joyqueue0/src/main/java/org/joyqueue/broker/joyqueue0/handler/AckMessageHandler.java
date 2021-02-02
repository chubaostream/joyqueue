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

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.AckMessage;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.apache.commons.lang3.ArrayUtils;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.consumer.Consume;
import org.joyqueue.broker.helper.SessionHelper;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.message.MessageLocation;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.session.Consumer;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ack
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/29
 */
public class AckMessageHandler implements Joyqueue0CommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(AckMessageHandler.class);

    private Consume consume;
    private SessionManager sessionManager;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.consume = brokerContext.getConsume();
        this.sessionManager = brokerContext.getSessionManager();
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        long startTime = SystemClock.now();

        AckMessage ackMessage = (AckMessage) command.getPayload();
        MessageLocation[] locations = ackMessage.getLocations();
        if (ArrayUtils.isEmpty(locations)) {
            return BooleanAck.build(JoyQueueCode.FW_CONSUMER_ACK_FAIL);
        }

        Consumer consumer = sessionManager.getConsumerById(ackMessage.getConsumerId().getConsumerId());
        if (consumer == null) {
            logger.warn("consumer session is not exist, transport: {}, locations: {}", transport, locations);
            return BooleanAck.build(JoyQueueCode.FW_CONSUMER_NOT_EXISTS);
        }
        Connection connection = SessionHelper.getConnection(transport);
       /* for (MessageLocation location : locations) {
            if (location.getPartition() == RetryMessage.RETRY_PARTITION_ID) {
                location.setPartition(Partition.RETRY_PARTITION_ID);
            }
        }*/

        try {
            consume.acknowledge(locations, consumer, connection, true);
        } catch (JoyQueueException e) {
            logger.error("ack exception, transport: {}, consumer: {}, locations: {}", transport, consumer, locations, e);
            return BooleanAck.build(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("ack exception, transport: {}, consumer: {}, locations: {}", transport, consumer, locations, e);
            return BooleanAck.build(JoyQueueCode.CN_UNKNOWN_ERROR);
        } finally {
            long endTime = SystemClock.now();

            if (endTime - startTime > 10) {
                logger.debug("ack elapsed time more than 10ms, command:[{}]", command);
            }
        }

        return BooleanAck.build();
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.ACK_MESSAGE.getCode();
    }
}