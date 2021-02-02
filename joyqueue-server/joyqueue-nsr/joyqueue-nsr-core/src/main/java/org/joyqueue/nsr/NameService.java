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
package org.joyqueue.nsr;

import org.joyqueue.domain.AllMetadata;
import org.joyqueue.domain.AppToken;
import org.joyqueue.domain.Broker;
import org.joyqueue.domain.ClientType;
import org.joyqueue.domain.Config;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Replica;
import org.joyqueue.domain.Subscription;
import org.joyqueue.domain.Topic;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.event.NameServerEvent;
import org.joyqueue.toolkit.concurrent.EventListener;
import org.joyqueue.toolkit.lang.LifeCycle;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * name service interface
 *
 * @author lixiaobin6
 * @date 2018/9/4
 */
public interface NameService extends LifeCycle {


    /**
     * 订阅
     */
    TopicConfig subscribe(Subscription subscription, ClientType clientType);

    /**
     * 订阅(mqtt)
     */
    List<TopicConfig> subscribe(List<Subscription> subscriptions, ClientType clientType);

    /**
     * 取消订阅
     */
    void unSubscribe(Subscription subscription);

    /**
     * 批量取消订阅
     */
    void unSubscribe(List<Subscription> subscriptions);

    /**
     * 是否有订阅
     *
     * @param app
     * @param subscribe
     * @return
     */
    boolean hasSubscribe(String app, Subscription.Type subscribe);

    /**
     * raft选举结果通知nameserver
     *
     * @param topic
     * @param partitionGroup partitionGroup
     * @param leaderBrokerId leaderBrokerId
     * @param isrId          isrId
     * @param termId         termId
     */
    void leaderReport(TopicName topic, int partitionGroup, int leaderBrokerId, Set<Integer> isrId, int termId);

    /**
     * broker启动获取该broker上所有的topic相关配置信息
     *
     * @param brokerId
     */
    Broker getBroker(int brokerId);


    /**
     * 获取所有Broker
     *
     * @return
     */
    @Deprecated
    List<Broker> getAllBrokers();

    /**
     * 添加主题
     */
    void addTopic(Topic topic, List<PartitionGroup> partitionGroups);

    /**
     * 获取topicConfig
     *
     * @param topic
     * @return
     */
    TopicConfig getTopicConfig(TopicName topic);

    /**
     * 获取所有主题名
     *
     * @return
     */
    Set<String> getAllTopicCodes();

    /**
     * 获取app订阅的topic
     *
     * @param app
     * @param subscription
     * @return
     */
    Set<String> getTopics(String app, Subscription.Type subscription);

    /**
     * 获取topicConfig
     *
     * @param brokerId
     * @return
     */
    Map<TopicName, TopicConfig> getTopicConfigByBroker(Integer brokerId);

    /**
     * 注册broker(用户启动时候)
     *
     * @param brokerId
     * @param brokerIp
     * @return
     */
    Broker register(Integer brokerId, String brokerHost, String brokerIp, Integer port, String node);

    /**
     * 获取ProducerConfig信息
     *
     * @param topic
     * @param app
     * @return
     */
    Producer getProducerByTopicAndApp(TopicName topic, String app);

    /**
     * 获取ProducerConfig信息
     *
     * @param topic
     * @param app
     * @return
     */
    Consumer getConsumerByTopicAndApp(TopicName topic, String app);

    /**
     * 根据app获取topic信息
     *
     * @param subscribeApp
     * @return
     */
    Map<TopicName, TopicConfig> getTopicConfigByApp(String subscribeApp, Subscription.Type subscribe);

    /**
     * 根据ip获取dataCenter
     *
     * @param ip
     * @return
     */
    DataCenter getDataCenter(String ip);

    /**
     * 获取k-v配置
     *
     * @param group
     * @param key
     * @return
     */
    String getConfig(String group, String key);

    /**
     * 获取k-v配置
     *
     * @return
     */
    List<Config> getAllConfigs();

    /**
     *
     * @param retryType
     * @return
     */
    List<Broker> getBrokerByRetryType(String retryType);

    /**
     * 根据topic该topic相关的获取消费配置
     *
     * @param topic
     * @return
     */
    List<Consumer> getConsumerByTopic(TopicName topic);

    /**
     * get producer
     * @param topic
     * @return
     */
    List<Producer> getProducerByTopic(TopicName topic);

    /**
     * get replica
     * @param brokerId
     * @return
     */
    List<Replica> getReplicaByBroker(Integer brokerId);

    /**
     * get app token
     * @param app
     * @param token
     * @return
     */
    AppToken getAppToken(String app, String token);

    /**
     * 返回所有元数据
     * @return
     */
    AllMetadata getAllMetadata();

    /**
     * add listener
     * @param listener
     */
    void addListener(EventListener<NameServerEvent> listener);

    /**
     * remove listener
     * @param listener
     */
    void removeListener(EventListener<NameServerEvent> listener);

    /**
     * add event
     * @param event
     */
    void addEvent(NameServerEvent event);
}
