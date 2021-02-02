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
package org.joyqueue.service.impl;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openmessaging.KeyValue;
import io.openmessaging.MessagingAccessPoint;
import io.openmessaging.OMS;
import io.openmessaging.OMSBuiltinKeys;
import io.openmessaging.joyqueue.JoyQueueBuiltinKeys;
import io.openmessaging.joyqueue.producer.ExtensionProducer;
import io.openmessaging.message.Message;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.async.BrokerClusterQuery;
import org.joyqueue.async.BrokerMonitorClusterQuery;
import org.joyqueue.async.RetrieveProvider;
import org.joyqueue.convert.CodeConverter;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.TopicName;
import org.joyqueue.exception.ServiceException;
import org.joyqueue.model.domain.Application;
import org.joyqueue.model.domain.ApplicationToken;
import org.joyqueue.model.domain.Broker;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.domain.PartitionGroupReplica;
import org.joyqueue.model.domain.ProducerSendMessage;
import org.joyqueue.model.domain.SimplifiedBrokeMessage;
import org.joyqueue.model.domain.Subscribe;
import org.joyqueue.model.domain.SubscribeType;
import org.joyqueue.monitor.BrokerMessageInfo;
import org.joyqueue.monitor.RestResponse;
import org.joyqueue.monitor.RestResponseCode;
import org.joyqueue.nsr.AppTokenNameServerService;
import org.joyqueue.nsr.BrokerNameServerService;
import org.joyqueue.nsr.ReplicaServerService;
import org.joyqueue.other.HttpRestService;
import org.joyqueue.service.ApplicationService;
import org.joyqueue.service.ApplicationTokenService;
import org.joyqueue.service.BrokerMessageService;
import org.joyqueue.service.LeaderService;
import org.joyqueue.service.MessagePreviewService;
import org.joyqueue.util.NullUtil;
import org.joyqueue.util.UrlEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.joyqueue.exception.ServiceException.INTERNAL_SERVER_ERROR;


@Service("brokerMessageService")
public class BrokerMessageServiceImpl implements BrokerMessageService {

    private static final Logger logger= LoggerFactory.getLogger(BrokerMessageServiceImpl.class);
    private static final long TIMEOUT=10000;
    public static final ObjectMapper mapper = new ObjectMapper();
    @Resource(type = BrokerMonitorClusterQuery.class)
    private BrokerClusterQuery<Subscribe> brokerClusterQuery;
    @Autowired
    private LeaderService leaderService;
    @Autowired(required = false)
    private HttpRestService httpRestService;
    @Autowired
    private MessagePreviewService messagePreviewService;
    @Autowired
    private ReplicaServerService replicaServerService;
    @Autowired
    private BrokerNameServerService brokerNameServerService;
    @Autowired
    private AppTokenNameServerService appTokenNameServerService;
    @Autowired
    private ApplicationTokenService applicationTokenService;
    @Autowired
    private ApplicationService applicationService;

    @Override
    public List<SimplifiedBrokeMessage> previewMessage(Subscribe subscribe,String messageDecodeType ,int count) {
        List<SimplifiedBrokeMessage> simplifiedBrokeMessages=new ArrayList<>();
        List<Broker> brokers=new ArrayList<>();
        Future<Map<String,String >> resultFuture= brokerClusterQuery.asyncQueryOnBroker(subscribe, new RetrieveProvider<Subscribe>() {
            @Override
            public String getKey(Broker broker, PartitionGroup partitionGroup,short partition ,Subscribe condition) {
                brokers.add(broker);
                return broker.getIp()+":"+broker.getMonitorPort();
            }
            @Override
            public String getPath(String pathTemplate,PartitionGroup partitionGroup,short partition,Subscribe condition) {
                return  String.format(pathTemplate, UrlEncoderUtil.encodeParam(CodeConverter.convertTopic(subscribe.getNamespace(),subscribe.getTopic()).getFullName(),
                        subscribe.getType()== SubscribeType.PRODUCER?subscribe.getApp().getCode():CodeConverter.convertApp(subscribe.getApp(),
                                subscribe.getSubscribeGroup()),String.valueOf(count)));
            }
        },"previewMessage" ," preview pending or last message");
        Map<String/*request key*/, String/*response*/> resultMap= brokerClusterQuery.get(resultFuture,TIMEOUT,TimeUnit.MILLISECONDS);
        SimplifiedBrokeMessage message;
        RestResponse<List<BrokerMessageInfo>> brokerMessageResponse;
        JavaType messagesListType = mapper.getTypeFactory().constructParametricType(List.class, BrokerMessageInfo.class);
        JavaType restListBrokerMessageType = mapper.getTypeFactory().constructParametricType(RestResponse.class, messagesListType);
        try {
            for (Map.Entry<String, String> response : resultMap.entrySet()) {
                brokerMessageResponse = mapper.readValue(response.getValue(), restListBrokerMessageType);
                if (brokerMessageResponse.getCode()== RestResponseCode.SUCCESS.getCode()) {
                    if(!NullUtil.isEmpty(brokerMessageResponse.getData())){
                        for (BrokerMessageInfo m : brokerMessageResponse.getData()) {
                            message = simpleBrokerMessageConvert(m,messageDecodeType);
                            simplifiedBrokeMessages.add(message);
                        }
                    }else{
                        logger.info(String.format("%s preview message request success,but empty message",response.getKey()));
                    }
                }else{
                    logger.info(String.format("%s preview message request failed",response.getKey()));
                }
            }
        }catch (Exception e){
            logger.error("parse broker message error", e);
            throw new ServiceException(INTERNAL_SERVER_ERROR,"Message can't be parse", e);
        }
        return simplifiedBrokeMessages;
    }

    @Override
    public List<SimplifiedBrokeMessage> previewNewestMessage(long topicId, String topic,String app, int count) {

        return null;
    }

    @Override
    public List<BrokerMessageInfo> viewMessage(Subscribe subscribe,String messageDecodeType ,String partition, String index, int count) {
        Map.Entry<PartitionGroup, Broker> brokerEntry = leaderService.findPartitionLeaderBrokerDetail(subscribe.getNamespace().getCode(),subscribe.getTopic().getCode(),Integer.valueOf(partition));
        Broker broker = brokerEntry.getValue();
        String path="getPartitionMessageByIndex";
        String[] args=new String[7];
        args[0]=broker.getIp();
        args[1]=String.valueOf(broker.getMonitorPort());
        args[2]=CodeConverter.convertTopic(subscribe.getNamespace(),subscribe.getTopic()).getFullName();
        args[3]=subscribe.getApp().getCode();
        args[4]=partition;
        args[5]=index;
        args[6]=String.valueOf(count);
        RestResponse<List<BrokerMessageInfo>> restResponse = httpRestService.get(path,BrokerMessageInfo.class,true,args);
        if (restResponse != null && restResponse.getData() != null) {
            return decodeBrokerMessage(restResponse.getData(),messageDecodeType);
        }
        return null;
    }

    /**
     * decode message by type
     * @param decodeType  decode type
     *
     **/
    public List<BrokerMessageInfo> decodeBrokerMessage(List<BrokerMessageInfo> msgs,String decodeType){
        for(BrokerMessageInfo m:msgs){
            if(m.getBody()!=null){
                compliantPreviewDecode(m,decodeType);
            }
        }
        return msgs;
    }

    @Override
    public Long getPartitionIndexByTime(Subscribe subscribe,String partition, String timestamp) {
        Map.Entry<PartitionGroup, Broker> brokerEntry = leaderService.findPartitionLeaderBrokerDetail(subscribe.getNamespace().getCode(),subscribe.getTopic().getCode(),Integer.valueOf(partition));
        Broker broker = brokerEntry.getValue();
        String path="getTopicAppPartitionIndexByTime";
        String[] args=new String[6];
        args[0]=broker.getIp();
        args[1]=String.valueOf(broker.getMonitorPort());
        args[2]=CodeConverter.convertTopic(subscribe.getNamespace(),subscribe.getTopic()).getFullName();
        args[3]=subscribe.getApp().getCode();
        args[4]=partition;
        args[5]=timestamp;
        RestResponse<Long> restResponse = httpRestService.get(path,Long.class,false,args);
        if (restResponse != null && restResponse.getData() != null) {
            return restResponse.getData();
        }
        return null;
    }


    @Override
    public SimplifiedBrokeMessage download(String ip, int port, String topic, String app, short partition, long index) {
        return null;
    }

    @Override
    public void sendMessage(ProducerSendMessage sendMessage) {
        Application application = applicationService.findByCode(sendMessage.getApp());
        if (application == null) {
            throw new RuntimeException("application not exist");
        }

        TopicName topicName = TopicName.parse(sendMessage.getTopic(), sendMessage.getNamespace());
        List<PartitionGroupReplica> partitionGroupReplicas = replicaServerService.findByTopicAndGroup(topicName.getCode(), topicName.getNamespace(), 0);

        if (CollectionUtils.isEmpty(partitionGroupReplicas)) {
            throw new RuntimeException("topic not exist");
        }

        Broker broker = null;

        try {
            broker = brokerNameServerService.findById(partitionGroupReplicas.get(0).getBrokerId());
        } catch (Exception e) {
            logger.error("find broker exception, brokerId: {}", partitionGroupReplicas.get(0).getBrokerId(), e);
            throw new RuntimeException("topic not exist");
        }

        if (broker == null) {
            throw new RuntimeException("broker not exist");
        }

        List<ApplicationToken> applicationTokens = null;
        try {
            applicationTokens = appTokenNameServerService.findByApp(sendMessage.getApp());
        } catch (Exception e) {
            logger.error("find token exception, app: {}", sendMessage.getApp(), e);
            throw new RuntimeException("topic not exist");
        }

        if (CollectionUtils.isEmpty(applicationTokens)) {
            ApplicationToken applicationToken = new ApplicationToken();
            applicationToken.setApplication(new Identity(application.getId(), application.getCode()));
            try {
                applicationTokenService.add(applicationToken);
                applicationTokens = Arrays.asList(applicationToken);
            } catch (Exception e) {
                logger.error("add token exception, app: {}", sendMessage.getApp(), e);
                throw new RuntimeException("token not exist");
            }
        }

        String[] messages = sendMessage.getMessage().split("\n");
        KeyValue attributes = OMS.newKeyValue();
        attributes.put(OMSBuiltinKeys.ACCOUNT_KEY, applicationTokens.get(0).getToken());
        attributes.put(JoyQueueBuiltinKeys.IO_THREADS, 1);
        MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint(String.format("oms:joyqueue://%s@%s:%s/console", sendMessage.getApp(), broker.getIp(), broker.getPort()), attributes);
        ExtensionProducer producer = (ExtensionProducer) messagingAccessPoint.createProducer();

        try {
            producer.start();
            for (String message : messages) {
                if (StringUtils.isBlank(message)) {
                    continue;
                }
                Message produceMessage = producer.createMessage(topicName.getFullName(), message);
                producer.send(produceMessage);
            }
        } finally {
            producer.stop();
        }
    }


    /**
     * compliant old version broker decode
     *
     **/
    public void compliantPreviewDecode(BrokerMessageInfo message, String messageDecodeType){

        try {
            message.setBody(messagePreviewService.preview(messageDecodeType,Base64.getDecoder().decode(message.getBody())));
        }catch(Throwable e){
            if(logger.isDebugEnabled()) {
                logger.debug("may old broker", e);
            }
            try {
                message.setBody(messagePreviewService.preview(messageDecodeType, message.getBody().getBytes(Charset.forName("utf-8"))));
            }catch (Throwable ex){
                logger.debug("incorrect message format", ex);
                throw new ServiceException(INTERNAL_SERVER_ERROR,"Message can't be parse");
            }
        }
    }

    /**
     * @param messageDecodeType  message deserialize type
     **/
    private SimplifiedBrokeMessage simpleBrokerMessageConvert(BrokerMessageInfo m,String messageDecodeType) {
        SimplifiedBrokeMessage message = new SimplifiedBrokeMessage();
        message.setId(m.getPartition() + "-" + m.getMsgIndexNo());
        message.setSendTime(m.getStartTime());
        message.setStoreTime(m.getStoreTime());
        message.setBusinessId(m.getBusinessId());
        if(m.getBody()!=null) {
            compliantPreviewDecode(m,messageDecodeType);
            message.setBody(m.getBody());
        }
        message.setAttributes(m.getAttributes());
        message.setFlag(m.isAck());
        return message;
    }
}
