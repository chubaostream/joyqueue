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
package io.chubao.joyqueue.broker.protocol.handler;

import com.google.common.collect.Lists;
import io.chubao.joyqueue.broker.BrokerContext;
import io.chubao.joyqueue.broker.BrokerContextAware;
import io.chubao.joyqueue.broker.protocol.JoyQueueCommandHandler;
import io.chubao.joyqueue.broker.cluster.ClusterManager;
import io.chubao.joyqueue.broker.protocol.converter.CheckResultConverter;
import io.chubao.joyqueue.broker.helper.SessionHelper;
import io.chubao.joyqueue.broker.producer.Produce;
import io.chubao.joyqueue.domain.TopicName;
import io.chubao.joyqueue.exception.JoyQueueCode;
import io.chubao.joyqueue.exception.JoyQueueException;
import io.chubao.joyqueue.network.command.BooleanAck;
import io.chubao.joyqueue.network.command.FetchProduceFeedbackAckData;
import io.chubao.joyqueue.network.command.FetchProduceFeedbackRequest;
import io.chubao.joyqueue.network.command.FetchProduceFeedbackResponse;
import io.chubao.joyqueue.network.command.JoyQueueCommandType;
import io.chubao.joyqueue.network.session.Connection;
import io.chubao.joyqueue.network.session.Producer;
import io.chubao.joyqueue.network.session.TransactionId;
import io.chubao.joyqueue.network.transport.Transport;
import io.chubao.joyqueue.network.transport.command.Command;
import io.chubao.joyqueue.network.transport.command.Type;
import io.chubao.joyqueue.response.BooleanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * FetchProduceFeedbackRequestHandler
 *
 * author: gaohaoxiang
 * date: 2018/12/19
 */
public class FetchProduceFeedbackRequestHandler implements JoyQueueCommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(FetchProduceFeedbackRequestHandler.class);

    private Produce produce;
    private ClusterManager clusterManager;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.produce = brokerContext.getProduce();
        this.clusterManager = brokerContext.getClusterManager();
    }

    @Override
    public Command handle(Transport transport, Command command) {
        FetchProduceFeedbackRequest fetchProduceFeedbackRequest = (FetchProduceFeedbackRequest) command.getPayload();
        Connection connection = SessionHelper.getConnection(transport);

        if (connection == null || !connection.isAuthorized(fetchProduceFeedbackRequest.getApp())) {
            logger.warn("connection is not exists, transport: {}, app: {}", transport, fetchProduceFeedbackRequest.getApp());
            return BooleanAck.build(JoyQueueCode.FW_CONNECTION_NOT_EXISTS.getCode());
        }

        BooleanResponse checkResult = clusterManager.checkWritable(TopicName.parse(fetchProduceFeedbackRequest.getTopic()),
                fetchProduceFeedbackRequest.getApp(), connection.getHost());
        if (!checkResult.isSuccess()) {
            logger.warn("checkWritable failed, transport: {}, topic: {}, app: {}, code: {}", transport,
                    fetchProduceFeedbackRequest.getTopic(), fetchProduceFeedbackRequest.getApp(), checkResult.getJoyQueueCode());
            return new Command(new FetchProduceFeedbackResponse(CheckResultConverter.convertCommonCode(command.getHeader().getVersion(), checkResult.getJoyQueueCode())));
        }

        FetchProduceFeedbackResponse fetchProduceFeedbackResponse = fetchProduceFeedback(connection, fetchProduceFeedbackRequest);
        return new Command(fetchProduceFeedbackResponse);
    }

    protected FetchProduceFeedbackResponse fetchProduceFeedback(Connection connection, FetchProduceFeedbackRequest fetchProduceFeedbackRequest) {
        Producer producer = new Producer(connection.getId(), fetchProduceFeedbackRequest.getTopic(), fetchProduceFeedbackRequest.getApp(), Producer.ProducerType.JOYQUEUE);
        try {
            FetchProduceFeedbackResponse fetchProduceFeedbackResponse = new FetchProduceFeedbackResponse();
            List<TransactionId> transactionIdList = produce.getFeedback(producer, fetchProduceFeedbackRequest.getCount());
            fetchProduceFeedbackResponse.setData(buildFeedbackAckData(transactionIdList));
            fetchProduceFeedbackResponse.setCode(JoyQueueCode.SUCCESS);
            return fetchProduceFeedbackResponse;
        } catch (JoyQueueException e) {
            logger.error("fetch feedback exception, transport: {}, topic: {}, app: {}", connection.getTransport().remoteAddress(),
                    fetchProduceFeedbackRequest.getTopic(), fetchProduceFeedbackRequest.getApp(), e);
            return new FetchProduceFeedbackResponse(JoyQueueCode.valueOf(e.getCode()));
        } catch (Exception e) {
            logger.error("fetch feedback exception, transport: {}, topic: {}, app: {}", connection.getTransport().remoteAddress(),
                    fetchProduceFeedbackRequest.getTopic(), fetchProduceFeedbackRequest.getApp(), e);
            return new FetchProduceFeedbackResponse(JoyQueueCode.CN_UNKNOWN_ERROR);
        }
    }

    protected List<FetchProduceFeedbackAckData> buildFeedbackAckData(List<TransactionId> transactionIdList) {
        List<FetchProduceFeedbackAckData> result = Lists.newArrayListWithCapacity(transactionIdList.size());
        for (TransactionId transactionId : transactionIdList) {
            result.add(new FetchProduceFeedbackAckData(transactionId.getTopic(), transactionId.getTxId(), transactionId.getQueryId()));
        }
        return result;
    }

    @Override
    public int type() {
        return JoyQueueCommandType.FETCH_PRODUCE_FEEDBACK_REQUEST.getCode();
    }
}