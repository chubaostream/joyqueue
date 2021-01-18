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
package org.joyqueue.api.Impl;


import org.joyqueue.api.OpenAPIService;
import org.joyqueue.convert.CodeConverter;
import org.joyqueue.exception.ServiceException;
import org.joyqueue.model.ListQuery;
import org.joyqueue.model.PageResult;
import org.joyqueue.model.Pagination;
import org.joyqueue.model.QPageQuery;
import org.joyqueue.model.domain.Application;
import org.joyqueue.model.domain.ApplicationToken;
import org.joyqueue.model.domain.ApplicationUser;
import org.joyqueue.model.domain.BaseModel;
import org.joyqueue.model.domain.Broker;
import org.joyqueue.model.domain.BrokerGroup;
import org.joyqueue.model.domain.BrokerMonitorRecord;
import org.joyqueue.model.domain.Consumer;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.domain.Namespace;
import org.joyqueue.model.domain.PartitionOffset;
import org.joyqueue.model.domain.Producer;
import org.joyqueue.model.domain.SlimApplication;
import org.joyqueue.model.domain.SlimTopic;
import org.joyqueue.model.domain.Subscribe;
import org.joyqueue.model.domain.SubscribeType;
import org.joyqueue.model.domain.Topic;
import org.joyqueue.model.domain.TopicPubSub;
import org.joyqueue.model.domain.User;
import org.joyqueue.model.exception.BusinessException;
import org.joyqueue.model.query.QBroker;
import org.joyqueue.model.query.QBrokerGroup;
import org.joyqueue.model.query.QTopic;
import org.joyqueue.monitor.PartitionAckMonitorInfo;
import org.joyqueue.monitor.PartitionLeaderAckMonitorInfo;
import org.joyqueue.monitor.PendingMonitorInfo;
import org.joyqueue.service.ApplicationService;
import org.joyqueue.service.ApplicationTokenService;
import org.joyqueue.service.ApplicationUserService;
import org.joyqueue.service.BrokerGroupService;
import org.joyqueue.service.BrokerMonitorService;
import org.joyqueue.service.BrokerService;
import org.joyqueue.service.ConsumeOffsetService;
import org.joyqueue.service.ConsumerService;
import org.joyqueue.service.LeaderService;
import org.joyqueue.service.ProducerService;
import org.joyqueue.service.TopicService;
import org.joyqueue.sync.ApplicationInfo;
import org.joyqueue.sync.SyncService;
import org.joyqueue.util.LocalSession;
import org.joyqueue.util.NullUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service("openAPIService")
public class OpenAPIServiceImpl implements OpenAPIService {
    private final Logger logger = LoggerFactory.getLogger(OpenAPIServiceImpl.class);
    @Autowired
    private TopicService topicService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private ProducerService producerService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private BrokerGroupService brokerGroupService;

    @Autowired
    private BrokerService brokerService;

    @Autowired
    private ConsumeOffsetService consumeOffsetService;

    @Autowired
    private BrokerMonitorService brokerMonitorService;

    @Autowired
    private LeaderService leaderService;

    @Autowired
    private ApplicationTokenService applicationTokenService;
    @Autowired
    private ApplicationUserService applicationUserService;
    private Random random = new Random();
    private static final long MINUTES_MS = 60 * 1000;


    @Override
    public PageResult<TopicPubSub> findTopicPubSubInfo(Pagination pagination) throws Exception {
        QPageQuery<QTopic> qPageQuery = new QPageQuery();
        qPageQuery.setQuery(new QTopic()); //empty
        qPageQuery.setPagination(pagination);
        PageResult<Topic> topicPageResult = topicService.search(qPageQuery);
        List<Topic> topics = topicPageResult.getResult();
        List<TopicPubSub> pubSubs = new ArrayList(topics.size());
        for (Topic topic : topics) {
            try {
                pubSubs.add(findTopicPubsub(topic));
            }catch(Exception e){
                logger.error(String.format("Find Topic PubSub Info, topic:%s", topic.getName()), e);
            }
        }
        PageResult<TopicPubSub> topicPubSubPageResult = new PageResult<>();
        topicPubSubPageResult.setPagination(topicPageResult.getPagination());
        topicPubSubPageResult.setResult(pubSubs);
        return topicPubSubPageResult;
    }


    /**
     * @return topic pub/sub info
     **/
    TopicPubSub findTopicPubsub(Topic topic) throws Exception {
        List<Consumer> consumers = consumerService.findByTopic(topic.getCode(), topic.getNamespace().getCode());
        List<Producer> producers = producerService.findByTopic(topic.getNamespace().getCode(), topic.getCode());
        TopicPubSub pubSub = new TopicPubSub();

        List<String> ips = new ArrayList<>();
        List<Broker> brokers = leaderService.findLeaderBroker(topic.getCode(), topic.getNamespace().getCode());
        if (!NullUtil.isEmpty(brokers)) {
            String iport;
            for (Broker b : brokers) {
                iport = b.getIp() + ":" + b.getPort();
                ips.add(iport);
            }
        }
        SlimTopic slimTopic = new SlimTopic();
        slimTopic.setIps(ips);
        slimTopic.setCode(topic.getCode());
        pubSub.setTopic(slimTopic);
        List<String> consumerList = new ArrayList<>();
        Identity identity;
        for (Consumer consumer : consumers) {
            identity = consumer.getApp();
            if (!NullUtil.isEmpty(identity)) {
                consumerList.add(String.valueOf(identity.getCode()));
            }
        }
        pubSub.setConsumers(appsToApplication(consumerList));
        List<String> producerList = new ArrayList<>();
        for (Producer producer : producers) {
            identity = producer.getApp();
            if (!NullUtil.isEmpty(identity)) {
                producerList.add(String.valueOf(identity.getCode()));
            }
        }
        pubSub.setProducers(appsToApplication(producerList));
        return pubSub;
    }

    @Override
    public TopicPubSub findTopicPubSubInfo(String topic, String namespace) throws Exception {
        Topic topiC = new Topic();
        topiC.setCode(topic);
        topiC.setNamespace(new Namespace());
        topiC.getNamespace().setCode(namespace);
        return findTopicPubsub(topiC);
    }

    @Override
    public List<Consumer> queryConsumerTopicByApp(String app) throws Exception {
        return consumerService.findByApp(app);
    }

    @Override
    public List<Consumer> findConsumers(String topic, String namespace) throws Exception {
        List<Consumer> consumers = consumerService.findByTopic(topic, namespace);
        return consumers;
    }

    @Override
    public List<Producer> findProducers(String topic, String namespace) throws Exception {
        List<Producer> producers = producerService.findByTopic(namespace, topic);
        return producers;
    }

    @Override
    public Producer publish(Producer producer) throws Exception {
        Topic topic = topicService.findByCode(producer.getNamespace().getCode(), producer.getTopic().getCode());
        Application application = applicationService.findByCode(producer.getApp().getCode());
        if (NullUtil.isEmpty(topic) || NullUtil.isEmpty(application)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format("topic %s or app %s not exist!", producer.getTopic().getCode(), producer.getApp().getCode()));
        }
        producer.setTopic(topic);
        producer.setApp(new Identity(producer.getApp().getCode()));
        producerService.add(producer);
        return producerService.findByTopicAppGroup(producer.getNamespace().getCode(), producer.getTopic().getCode(), producer.getApp().getCode());
    }

    @Override
    public Consumer subscribe(Consumer consumer) throws Exception {
        Topic topic = topicService.findByCode(consumer.getNamespace().getCode(), consumer.getTopic().getCode());
        Application application = applicationService.findByCode(consumer.getApp().getCode());
        if (NullUtil.isEmpty(topic) || NullUtil.isEmpty(application)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format("topic %s or app %s not exist!", consumer.getTopic().getCode(), consumer.getApp().getCode()));
        }
        consumer.setTopic(topic);
        consumer.setNamespace(consumer.getNamespace());
        consumer.setApp(consumer.getApp());
        consumerService.add(consumer);
        return consumerService.findByTopicAppGroup(consumer.getNamespace().getCode(), consumer.getTopic().getCode(), consumer.getApp().getCode(), consumer.getSubscribeGroup());
    }

    @Override
    public boolean unPublish(Producer producer) throws Exception {
        List<Producer> producers = findProducers(producer.getTopic().getCode(), producer.getNamespace().getCode());
        List<Consumer> consumers = findConsumers(producer.getTopic().getCode(), producer.getNamespace().getCode());
        if (NullUtil.isEmpty(producers) || (producers.size() == 1 && consumers.size() > 0)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format("no subscribe or please unSubscribe all the consumers of topic %s before cancel publish",
                    producer.getTopic().getCode()));
        }
        Producer p = findProducer(producers, producer.getApp().getCode());
        if (NullUtil.isEmpty(p))
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format(" %s haven't publish to the topic %s ",
                    producer.getApp().getCode(), CodeConverter.convertTopic(producer.getNamespace(), producer.getTopic()).getFullName()));

        return producerService.delete(p) > 0 ? true : false;
    }

    @Override
    public Consumer uniqueSubscribe(Consumer consumer) throws Exception {
        String namespace = consumer.getNamespace() == null ? null : consumer.getNamespace().getCode();
        Topic topic = topicService.findByCode(namespace, consumer.getTopic().getCode());
        Application application = applicationService.findByCode(consumer.getApp().getCode());
        if (NullUtil.isEmpty(topic) || NullUtil.isEmpty(application)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format("topic %s or app %s not exist!", consumer.getTopic().getCode(), consumer.getApp().getCode()));
        }
        User user = LocalSession.getSession().getUser();
        ApplicationUser applicationUser = applicationUserService.findByUserApp(user.getCode(), consumer.getApp().getCode());
        if (NullUtil.isEmpty(applicationUser)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format("user %s app %s no permission!", user.getCode(), consumer.getApp().getCode()));
        }
        Consumer exist = consumerService.findByTopicAppGroup(namespace, consumer.getTopic().getCode(), consumer.getApp().getCode(), consumer.getSubscribeGroup());
        if (NullUtil.isNotEmpty(exist) && NullUtil.isNotEmpty(exist.getSubscribeGroup())) {
            return exist;
        }
        int group = random.nextInt((int) MINUTES_MS);
        consumer.setSubscribeGroup(String.valueOf(group));
        consumer.setTopic(topic);
        consumer.setNamespace(consumer.getNamespace());
        consumer.setApp(consumer.getApp());
        consumerService.add(consumer);
        return consumerService.findByTopicAppGroup(namespace, consumer.getTopic().getCode(), consumer.getApp().getCode(), consumer.getSubscribeGroup());
    }

    /**
     *  find the app producer
     *
     **/
    Producer findProducer(List<Producer> producers, String app) {
        for (Producer p : producers) {
            if (p.getApp().getCode().equals(app)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean unSubscribe(Consumer consumer) throws Exception {
        Consumer c = consumerService.findByTopicAppGroup(consumer.getNamespace().getCode(), consumer.getTopic().getCode(),
                consumer.getApp().getCode(), consumer.getSubscribeGroup());
        if (NullUtil.isEmpty(c))
            throw new ServiceException(ServiceException.BAD_REQUEST, String.format(" %s haven't subscribe to the topic %s ",
                    CodeConverter.convertApp(new Identity(consumer.getApp().getCode()), consumer.getSubscribeGroup()),
                    CodeConverter.convertTopic(consumer.getNamespace(), consumer.getTopic()).getFullName()));
        // check pending message

        return consumerService.delete(c) > 0 ? true : false;
    }

    @Override
    public Application syncApplication(Application application) throws Exception {
        User user = LocalSession.getSession().getUser();
        application.setErp(user.getCode());
        ApplicationInfo info = syncService.syncApp(application);
        if (NullUtil.isEmpty(info) || NullUtil.isEmpty(user)) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "sync application failed or illegal erp " + application.getErp());
        }
        info.setUser(new Identity(user));
        syncService.addOrUpdateApp(info);
        return applicationService.findByCode(info.getCode());
    }

    @Override
    public boolean delApplication(Application application) throws Exception {
        if (!NullUtil.isEmpty(consumerService.findByApp(application.getCode())) || !NullUtil.isEmpty(producerService.findByApp(application.getCode())))
            throw new ServiceException(ServiceException.BAD_REQUEST, "please unSubscribe/Publish  all  topics you have !");
        application = applicationService.findByCode(application.getCode());
        return applicationService.delete(application) > 0 ? true : false;
    }

    @Override
    public Topic createTopic(Topic topic, QBrokerGroup brokerGroup, Identity operator) throws Exception {
//        topic.setElectType(PartitionGroup.ElectType.raft);
        List<Broker> brokers = allocateBrokers(topic, brokerGroup);
        return createTopic(topic,brokers,operator);
    }

    @Override
    public Topic createTopic(Topic topic, List<Broker> select, Identity operator) throws Exception {
        if(Objects.isNull(select)||select.size()==0){
            throw new ServiceException(ServiceException.BAD_REQUEST, "select broker is empty");
        }
        //计算总数
        topic.setPartitions(topic.getPartitions() * select.size());
        topic.setBrokers(select);
        topicService.addWithBrokerGroup(topic);
        return topicService.findById(topic.getId());    }

    @Override
    public void removeTopic(String namespace, String topicCode) throws Exception {
        Topic topic = topicService.findByCode(namespace, topicCode);
        if (topic == null) {
            throw new BusinessException("topic does not exist");
        }
        if (topic.getCode().startsWith("shadow_")) {
            String sourceTopic = topic.getCode().substring("shadow_".length());
            List<Producer> producers = producerService.findByTopic("", topic.getCode());
            for (Producer producer : producers) {
                Producer srcProducer = producerService.findByTopicAppGroup("", sourceTopic, producer.getApp().getCode());
                if (srcProducer != null) {
                    if (srcProducer.getConfig().getParams() != null) {
                        srcProducer.getConfig().getParams().put("shadow", "false");
                        producerService.update(srcProducer);
                    }
                }
            }
        }
        topicService.delete(topic);
    }

    /**
     * Random allocate broker group and broker  for topic
     **/
    List<Broker> allocateBrokers(Topic topic, QBrokerGroup qBrokerGroup) throws Exception {
        //校验分组是否存在
        qBrokerGroup.setRole(0);//1管理员 0 普通用户
        List<BrokerGroup> brokerGroupList = brokerGroupService.findByQuery(new ListQuery<>(qBrokerGroup));
        if (brokerGroupList == null || brokerGroupList.size() == 0) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "broker group is empty");
        }
        BrokerGroup brokerGroup = brokerGroupList.get(0);
        topic.setBrokerGroup(brokerGroup); // broker group

        QBroker qBroker = new QBroker();
        qBroker.setGroup(new Identity(brokerGroup.getId(), brokerGroup.getCode()));
        List<Broker> brokers = brokerService.queryBrokerList(qBroker);

        if (brokers.size() == 0) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "select broker is empty");
        }
        //如果用户设置broker数量,则校验broker数量是否能满足
        if (topic.getBrokerNum() != 0 && topic.getBrokerNum() > brokers.size()) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "实际可用broker数量小于指定broker数量");
        }
        //如果用户没设置broker数量 默认是3个
        if (topic.getBrokerNum() == 0) {
            topic.setBrokerNum(3);
        }

        Random random = new Random();
        List<Broker> selectBroker = new ArrayList<>();
        int startId = random.nextInt(brokers.size());
        int endId = startId + topic.getBrokerNum();
        for (int i = startId; i < endId; i++) {
            Broker broker = brokers.get(i % brokers.size());
            if (selectBroker.contains(broker)) {
                continue;
            }
            selectBroker.add(broker);
        }
        return selectBroker;
    }

    @Override
    public List<PartitionAckMonitorInfo> findOffsets(Subscribe subscribe) {
        List<PartitionAckMonitorInfo> partitionAckMonitorInfos = new ArrayList<>();
        subscribe.setType(SubscribeType.CONSUMER);
        isLegalSubscribe(subscribe);
        List<PartitionLeaderAckMonitorInfo> partitionLeaderAckMonitorInfos = consumeOffsetService.offsets(subscribe);
        for (PartitionLeaderAckMonitorInfo p : partitionLeaderAckMonitorInfos) {
            if (p.isLeader()) {
                partitionAckMonitorInfos.add(p);
            }
        }
        return partitionAckMonitorInfos;
    }

    @Override
    public boolean resetOffset(Subscribe subscribe, short partition, long offset) {
        subscribe.setType(SubscribeType.CONSUMER);
        isLegalSubscribe(subscribe);
        return consumeOffsetService.resetOffset(subscribe, partition, offset);
    }

    @Override
    public List<PartitionAckMonitorInfo> timeOffset(Subscribe subscribe, long timeMs) {
        return consumeOffsetService.timeOffset(subscribe, timeMs);
    }

    @Override
    public boolean resetOffset(Subscribe subscribe, long timeMs) {
        subscribe.setType(SubscribeType.CONSUMER);
        isLegalSubscribe(subscribe);
        return consumeOffsetService.resetOffset(subscribe, timeMs);
    }

    @Override
    public boolean resetOffset(Subscribe subscribe, List<PartitionOffset> offsets) {
        subscribe.setType(SubscribeType.CONSUMER);
        isLegalSubscribe(subscribe);
        return consumeOffsetService.resetOffset(subscribe, offsets);
    }

    @Override
    public PendingMonitorInfo pending(Subscribe subscribe) {
        subscribe.setType(SubscribeType.CONSUMER);
        isLegalSubscribe(subscribe);
        BrokerMonitorRecord record = brokerMonitorService.find(subscribe, true);
        if (NullUtil.isEmpty(record) || NullUtil.isEmpty(record.getPending())) {
            throw new ServiceException(ServiceException.INTERNAL_SERVER_ERROR, "data not found");
        }
        return record.getPending();
    }

    @Override
    public int queryPartitionByTopic(String namespaceCode, String topicCode) throws Exception {
        Topic topic = topicService.findByCode(namespaceCode, topicCode);
        return topic.getPartitions();
    }


    /**
     *
     * @param apps  can't be null
     *
     **/
    List<SlimApplication> appsToApplication(List<String> apps) {
        if (NullUtil.isEmpty(apps)) return null;
        List<Application> applications = applicationService.findByCodes(apps);
        List<SlimApplication> slimApplications = new ArrayList<>();
        SlimApplication slimApplication;
        for (Application a : applications) {
            slimApplication = new SlimApplication();
            slimApplication.setCode(a.getCode());
            slimApplication.setOwner(a.getOwner());
            slimApplication.setDepartment(a.getDepartment());
            slimApplications.add(slimApplication);
        }
        return slimApplications;
    }


    @Override
    public List<ApplicationToken> add(ApplicationToken token) {
        String app = token.getApplication().getCode();
        Application application = applicationService.findByCode(app);
        if (NullUtil.isEmpty(application) || application.getStatus() == BaseModel.DELETED) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "app not exist");
        }
        token.setApplication(new Identity(application));
        try {
            applicationTokenService.add(token);
            return tokens(app);
        } catch (Exception e) {
            throw new ServiceException(ServiceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public List<ApplicationToken> tokens(String app) {
        try {
            return applicationTokenService.findByApp(app);
        } catch (Exception e) {
            throw new ServiceException(ServiceException.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @Override
    public BrokerMonitorRecord brokerMonitor(Subscribe subscribe, boolean active) {
        return brokerMonitorService.find(subscribe, active);
    }

    /**
     *
     *  Check the subscription legal or not
     *
     *  @return true if exist
     *
     **/
    private boolean isLegalSubscribe(Subscribe subscribe) {
        if (subscribe.getType() == SubscribeType.CONSUMER) {
            Consumer c = consumerService.findByTopicAppGroup(subscribe.getNamespace().getCode(), subscribe.getTopic().getCode(),
                    subscribe.getApp().getCode(), subscribe.getSubscribeGroup());
            if (NullUtil.isEmpty(c))
                throw new ServiceException(ServiceException.BAD_REQUEST, String.format(" %s haven't subscribe the topic %s ",
                        CodeConverter.convertApp(subscribe.getApp(), subscribe.getSubscribeGroup()), CodeConverter.convertTopic(subscribe.getNamespace(), subscribe.getTopic()).getFullName()));
        } else {
            Producer producer = producerService.findByTopicAppGroup(subscribe.getNamespace().getCode(), subscribe.getTopic().getCode(), subscribe.getApp().getCode());
            if (NullUtil.isEmpty(producer)) {
                throw new ServiceException(ServiceException.BAD_REQUEST, String.format(" %s haven't publish the topic %s ",
                        subscribe.getApp().getCode(), CodeConverter.convertTopic(subscribe.getNamespace(), subscribe.getTopic()).getFullName()));
            }
        }
        return true;
    }


}
