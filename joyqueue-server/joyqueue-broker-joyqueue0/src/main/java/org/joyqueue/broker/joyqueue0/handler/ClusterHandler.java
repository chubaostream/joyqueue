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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.Joyqueue0Consts;
import org.joyqueue.broker.joyqueue0.command.*;
import org.joyqueue.broker.joyqueue0.config.Joyqueue0Config;
import org.joyqueue.broker.joyqueue0.converter.TopicConverter;
import org.joyqueue.broker.joyqueue0.entity.GetClusterEntity;
import org.joyqueue.broker.joyqueue0.entity.TopicEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.cluster.ClusterNameService;
import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.domain.*;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.network.transport.exception.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 集群信息处理
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/27
 */
public class ClusterHandler implements Joyqueue0CommandHandler, Type, BrokerContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(ClusterHandler.class);
    public static final String CONNECTION_DC_KEY = "connection_data_center";
    public static final String UNKNOWN_DC="Unknown";
    private  final short DEFAULT_BROKER_GROUP_WEIGHT =0;
    // 非就近机房 broker group 权重
    private  final short DEFAULT_NOT_NEAR_BY_BROKER_GROUP_WEIGHT =1;
    // for broker group more than DEFAULT_BROKER_GROUP_SUM_WEIGHT
    private  final short DEFAULT_BROKER_GROUP_FACTOR =3;
    // 总权重
    private  final short DEFAULT_BROKER_GROUP_SUM_WEIGHT =100;
    private BrokerConfig brokerConfig;
    private ClusterNameService clusterNameService;
    private BrokerContext brokerContext;
    private Joyqueue0Config config;

    private Cache<String, GetClusterEntity> clusterCache;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
        this.brokerConfig = brokerContext.getBrokerConfig();
        this.clusterNameService = brokerContext.getClusterNameService();
        this.config = new Joyqueue0Config(brokerContext.getPropertySupplier());
        this.clusterCache = newClusterCache();
    }

    protected Cache<String, GetClusterEntity> newClusterCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(config.getClusterBodyCacheExpireTime(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        GetClusterAck getClusterAck = new GetClusterAck();
        try {
            GetCluster getCluster = (GetCluster) command.getPayload();
            DataCenter clientDataCenter = transport.attr().get(CONNECTION_DC_KEY);
            if (Objects.isNull(clientDataCenter)) {
                String ip = getIpFromTransport(transport);
                clientDataCenter = clusterNameService.getNameService().getDataCenter(ip);
                if (Objects.nonNull(clientDataCenter)) {
                    transport.attr().set(CONNECTION_DC_KEY, clientDataCenter);
                }
            }
            if (logger.isDebugEnabled()) {
                String ip = getIpFromTransport(transport);
                logger.info("client ip {},data center {}", ip, clientDataCenter == null ? UNKNOWN_DC : clientDataCenter.getRegion());
            }
            GetClusterEntity getClusterEntity = getCluster(getCluster.getApp(), clientDataCenter);
            //TODO 设置客户端数据中心
            getClusterAck.setDataCenter((byte) 0);
            getClusterAck.setInterval(config.getClusterBodyCacheUpdateInterval());
            getClusterAck.setMaxSize(brokerConfig.getFrontendConfig().getFrameMaxSize());
            getClusterAck.setClusters(getClusterEntity.getBrokerClusters());
            getClusterAck.setAllTopicConfigStrings(JSON.toJSONString(getClusterEntity.getTopicMapper(), SerializerFeature.DisableCircularReferenceDetect));
            if (logger.isDebugEnabled()) {
                logger.debug("cluster, address: {}, app: {}, metadata: {}", transport, getCluster.getApp(), JSON.toJSONString(getClusterAck));
            }
        }catch (Throwable e){
            logger.info("Get cluster exception",e);
            return BooleanAck.build(JoyQueueCode.CN_UNKNOWN_ERROR.getCode(), e.getMessage());
        }
        return new Command(getClusterAck);
    }

    /**
     *  Get Ip from transport
     **/
    public String getIpFromTransport(Transport transport){
        return ((InetSocketAddress) transport.remoteAddress()).getHostString();
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CLUSTER.getCode();
    }

    protected GetClusterEntity getCluster(String app, DataCenter clientDataCenter) {
        if (!config.getClusterBodyCacheEnable()) {
            return doGetCluster(app,clientDataCenter);
        }
        String appDCClusterMetadataKey=String.format("%s.%s",app,clientDataCenter==null?UNKNOWN_DC:clientDataCenter.getRegion());
        try {
            return clusterCache.get(appDCClusterMetadataKey, () -> {
                return doGetCluster(app, clientDataCenter);
            });
        } catch (Exception e) {
            logger.error("get cluster exception, app: {}", app, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据topic和app获得相关broker
     * 判断app是否有消费或生产关系
     * 判断broker存储相关，设置新权限
     * 实体转换到BrokerCluster
     *
     * @param app
     * @param clientDataCenter  client data center may null
     * @return
     */
    protected GetClusterEntity doGetCluster(String app, DataCenter clientDataCenter) {
        String[] appGroup = app.split(Joyqueue0Consts.APP_GROUP_SPLIT);
        Map<TopicName, TopicConfig> appTopicMapper = clusterNameService.getTopicConfigByApp(app, Subscription.Type.CONSUMPTION);
        Map<TopicName, TopicConfig> appTopicProducerMapper = clusterNameService.getTopicConfigByApp(appGroup[0], Subscription.Type.PRODUCTION);

        Map<TopicName, TopicConfig> allAppTopicMapper = Maps.newHashMap();
        if (MapUtils.isNotEmpty(appTopicMapper)) {
            allAppTopicMapper.putAll(appTopicMapper);
        }
        if (MapUtils.isNotEmpty(appTopicProducerMapper)) {
            allAppTopicMapper.putAll(appTopicProducerMapper);
        }

        if (MapUtils.isEmpty(allAppTopicMapper)) {
            logger.warn("Get cluster of app {} return empty", app);
            return new GetClusterEntity();
        }

        List<BrokerCluster> brokerClusters = Lists.newLinkedList();
        Map<String, TopicEntity> topicMapper = Maps.newHashMap();

        for (Map.Entry<TopicName, TopicConfig> entry : allAppTopicMapper.entrySet()) {
            TopicConfig topicConfig = entry.getValue();

            Producer producer = clusterNameService.getNameService().getProducerByTopicAndApp(entry.getKey(), app);
            Consumer consumer = clusterNameService.getNameService().getConsumerByTopicAndApp(entry.getKey(), app);

            if (producer == null && consumer == null) {
                continue;
            }

            Producer.ProducerPolicy producerPolicy = null;
            Consumer.ConsumerPolicy consumerPolicy = null;

            if (producer != null) {
                if (producer.getProducerPolicy() == null) {
                    producerPolicy = brokerContext.getProducerPolicy();
                } else {
                    producerPolicy = producer.getProducerPolicy();
                }
                // process near by produce

            }
            if (consumer != null) {
                if (consumer.getConsumerPolicy() == null) {
                    consumerPolicy = brokerContext.getConsumerPolicy();
                } else {
                    consumerPolicy = consumer.getConsumerPolicy();
                }
            }
            BrokerCluster brokerCluster = convertToBrokerCluster(topicConfig, producerPolicy);
            mayUpdateTopicBrokerGroupWeight(brokerCluster,producerPolicy,app,clientDataCenter);
            brokerClusters.add(brokerCluster);
            topicMapper.put(topicConfig.getName().getFullName(), TopicConverter.toEntity(topicConfig, app, consumer, consumerPolicy, producer, producerPolicy));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Get cluster of app {}, topicMapper is {}, brokerClusters is {}",
                    app, topicMapper, brokerClusters);
        }
        return new GetClusterEntity(topicMapper, brokerClusters);
    }

    protected BrokerCluster convertToBrokerCluster(TopicConfig topicConfig, Producer.ProducerPolicy producerPolicy) {
        Collection<PartitionGroup> partitionGroups = topicConfig.getPartitionGroups().values();
        BrokerCluster brokerCluster = new BrokerCluster();
        brokerCluster.setTopic(topicConfig.getName().getFullName());

        for (PartitionGroup partitionGroup : partitionGroups) {
            if (brokerCluster.getQueues() == 0) {
                brokerCluster.setQueues((short) partitionGroup.getPartitions().size());
            }
            if (partitionGroup.getLeader().equals(-1)) {
                continue;
            }
            BrokerGroup brokerGroup = convertToBrokerGroup(partitionGroup, producerPolicy);
            brokerCluster.addGroup(brokerGroup);
        }
        //

        return brokerCluster;
    }

    /**
     * Update  broker group weight if weight not be set and near by of produce config is true,
     *
     * @param topicCluster  topic cluster
     * @param producerPolicy  producer all, can't be null
     * @param clientDataCenter  data center
     *
     **/
    private void mayUpdateTopicBrokerGroupWeight(BrokerCluster topicCluster, Producer.ProducerPolicy producerPolicy, String app, DataCenter clientDataCenter){
        if(Objects.isNull(producerPolicy)||Objects.isNull(clientDataCenter)||Objects.isNull(producerPolicy.getNearby())) {
            return;
        }
        // near by is open and no predefined broker group weight
        if(producerPolicy.getNearby()&&!hasPredefinedWeight(producerPolicy)&& CollectionUtils.isNotEmpty(topicCluster.getGroups())){
            // calculate weight for broker group
          int remainingWeight=topicCluster.getGroups().size()>DEFAULT_BROKER_GROUP_SUM_WEIGHT?DEFAULT_BROKER_GROUP_FACTOR*topicCluster.getGroups().size():DEFAULT_BROKER_GROUP_SUM_WEIGHT;
          List<BrokerGroup> unsetWeightGroup=new ArrayList();
          for(BrokerGroup bg:topicCluster.getGroups()){
              Permission groupPerm= bg.getPermission();
              if(groupPerm.contain(Permission.WRITE)){
                 Joyqueue0Broker jmq2Broker= getBrokerGroupLeader(bg);
                 if(Objects.nonNull(jmq2Broker)){
                    DataCenter brokerDataCenter= clusterNameService.getNameService().getDataCenter(jmq2Broker.getIp());
                    if(Objects.isNull(brokerDataCenter)||!clientDataCenter.getRegion().equals(brokerDataCenter.getRegion())){
                        // diff with producer client data center,use mini weight for group
                        bg.setWeight(DEFAULT_NOT_NEAR_BY_BROKER_GROUP_WEIGHT);
                        remainingWeight-=DEFAULT_NOT_NEAR_BY_BROKER_GROUP_WEIGHT;
                    }else{
                        unsetWeightGroup.add(bg);
                    }
                 }else{
                    logger.info("Must be something wrong,current broker group {}", JSON.toJSONString(bg));
                 }
              }
          }
          if(unsetWeightGroup.size()>0){
              int avg=remainingWeight/unsetWeightGroup.size();
              for(int i=0;i<unsetWeightGroup.size()-1;i++){
                 BrokerGroup unsetBrokerGroup= unsetWeightGroup.get(i);
                 unsetBrokerGroup.setWeight((short) avg);
                 remainingWeight-=avg;
              }
              // set last unset broker group
              unsetWeightGroup.get(unsetWeightGroup.size()-1).setWeight((short)remainingWeight);
          }
          if(logger.isDebugEnabled()){
              StringBuilder builder=new StringBuilder();
              for(BrokerGroup bg:topicCluster.getGroups()){
                  builder.append(bg.getGroup()).append(":").append(bg.getWeight()).append(";");
              }
              logger.debug("app {},client data center {}, produce weight {}",app,clientDataCenter.getCode(),builder.toString());
          }
        }
    }

    /**
     * Get broker group leader broker
     **/
    public Joyqueue0Broker getBrokerGroupLeader(BrokerGroup brokerGroup){
        for(Joyqueue0Broker b:brokerGroup.getBrokers()){
            if(b.getRole()==ClusterRole.MASTER){
                return b;
            }
        }
        return null;
    }

    /**
     * Check has pre defined weight
     *
     **/
    private boolean hasPredefinedWeight(Producer.ProducerPolicy  producerPolicy){
        if(producerPolicy.getWeight()!=null&&producerPolicy.getWeight().size()>0){
            for(Short w: producerPolicy.getWeight().values()){
                if(!w.equals(DEFAULT_BROKER_GROUP_WEIGHT)){
                    return true;
                }
            }
        }
        return false;
    }




    protected BrokerGroup convertToBrokerGroup(PartitionGroup partitionGroup, Producer.ProducerPolicy producerPolicy) {
        Map<Integer, Broker> brokers = partitionGroup.getBrokers();
        BrokerGroup brokerGroup = new BrokerGroup();
        brokerGroup.setPermission(Permission.FULL);
        brokerGroup.setWeight(DEFAULT_BROKER_GROUP_WEIGHT);

        if (producerPolicy != null) {
            Map<String, Short> weights = producerPolicy.getWeight();
            if (MapUtils.isNotEmpty(weights)) {
                Short weight = ObjectUtils.defaultIfNull(weights.get(String.valueOf(partitionGroup.getGroup())), DEFAULT_BROKER_GROUP_WEIGHT);
                brokerGroup.setWeight(weight);
            }
        }

        brokerGroup.setBrokerType(Joyqueue0Broker.BrokerType.JOYQUEUE0);
        brokerGroup.setGroup(String.valueOf(partitionGroup.getGroup()));

        if (MapUtils.isNotEmpty(brokers)) {
            for (Map.Entry<Integer, Broker> entry : brokers.entrySet()) {
                Broker broker = entry.getValue();
                if(!config.getClusterBodyWithSlave() && !partitionGroup.getLeader().equals(broker.getId())) {
                    continue;
                }

                Joyqueue0Broker newBroker = new Joyqueue0Broker();
                newBroker.setId(broker.getId());
                newBroker.setIp(StringUtils.isNotBlank(broker.getExternalIp()) ? broker.getExternalIp() : broker.getIp());
                newBroker.setPort(broker.getExternalPort() > 0 ? broker.getExternalPort() : broker.getPort());
                newBroker.setType(Joyqueue0Broker.BrokerType.JOYQUEUE0);
                newBroker.setLocation(Joyqueue0Broker.Location.IDC);
                newBroker.setRole(partitionGroup.getLeader().equals(broker.getId()) ? ClusterRole.MASTER : ClusterRole.SLAVE);
                newBroker.setRetryType(broker.getRetryType().equals(Broker.DEFAULT_RETRY_TYPE) ? RetryType.REMOTE : RetryType.DB);
                newBroker.setSyncMode(SyncMode.ASYNCHRONOUS);
                newBroker.setGroup(brokerGroup.getGroup() + "_" + brokerGroup.getGroup() + "_" + partitionGroup.getTerm());

                if (Broker.PermissionEnum.FULL.equals(broker.getPermission())) {
                    newBroker.setPermission(Permission.FULL);
                } else if (Broker.PermissionEnum.WRITE.equals(broker.getPermission())) {
                    newBroker.setPermission(Permission.WRITE);
                } else if (Broker.PermissionEnum.READ.equals(broker.getPermission())) {
                    newBroker.setPermission(Permission.READ);
                } else if (Broker.PermissionEnum.NONE.equals(broker.getPermission())) {
                    newBroker.setPermission(Permission.NONE);
                } else {
                    newBroker.setPermission(Permission.NONE);
                }
                brokerGroup.addBroker(newBroker);
            }
        }

        return brokerGroup;
    }
}