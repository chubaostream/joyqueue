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
package org.joyqueue.nsr.service.test;

import org.joyqueue.domain.AppToken;
import org.joyqueue.domain.Broker;
import org.joyqueue.domain.ClientType;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Replica;
import org.joyqueue.domain.Subscription;
import org.joyqueue.domain.TopicConfig;
import org.joyqueue.domain.TopicName;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.config.NameServiceConfig;
import org.joyqueue.nsr.nameservice.NameServer;
import org.joyqueue.nsr.nameservice.ThinNameService;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.config.PropertySupplierAware;
import org.joyqueue.toolkit.io.Files;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.network.IpUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wylixiaobin
 * Date: 2018/8/20
 */

public class NameServiceTest {
    private static NameService nameService;
    private static NameServer nameServer;
    private static File dataRoot = new File(System.getProperty("java.io.tmpdir") + "/joyqueue/nsr_temp");

    @BeforeClass
    public static void init() throws Exception {
        System.out.println("before....................");
        Map properties = new HashMap();

        int nameServerPort = 50092;
        Files.deleteDirectory(dataRoot);
        Files.createDirectory(dataRoot);
        properties.put("application.data.path", dataRoot);
        properties.put("nameserver.ignite.storage.defaultDataRegion.name", "joyqueueRegion");
        properties.put("nameserver.nsr.manage.port", nameServerPort + 1);
        properties.put("nameserver.nsr.service.port", nameServerPort);
        properties.put("nameserver.nameserver.transport.ioThreads", "8");
        properties.put("nameservice.serverAddress", IpUtil.getLocalIp() + ":" + nameServerPort);
        properties.put("nameserver.ignite.discoverySpi.localPort", "48600");
        properties.put("nameserver.ignite.discoverySpi.localPortRange", "20");
        properties.put("nameserver.ignite.communicationSpi.localPort", nameServerPort + 2);


        NameServiceConfig nameServiceConfig = new NameServiceConfig(new PropertySupplier.MapSupplier(properties));

        nameServer = new NameServer();
        nameServer.setSupplier(new PropertySupplier.MapSupplier(properties));
        nameServer.start();

        nameService = new ThinNameService(nameServiceConfig);
        if (nameService instanceof PropertySupplierAware) {
            ((PropertySupplierAware) nameService).setSupplier(new PropertySupplier.MapSupplier(properties));
        }
        nameService.start();

    }

    @AfterClass
    public static void after() {
        Close.close(nameService);
        Close.close(nameServer);

        Files.deleteDirectory(dataRoot);
        System.out.println("after....................");
    }

    @Test
    public void subscribe() {
        String app = "jm";
        String topic = "__group_coordinators";
        TopicConfig topicConfig = nameService.subscribe(new Subscription(TopicName.parse(topic), app, Subscription.Type.CONSUMPTION), ClientType.JOYQUEUE);
        topicConfig = nameService.subscribe(new Subscription(TopicName.parse(topic), app, Subscription.Type.CONSUMPTION), ClientType.JOYQUEUE);
        System.out.println("success");
    }


    @Test
    public void unSubscribe() {
        String app = "joyqueue";
        String topic = "__group_coordinators";
        nameService.unSubscribe(new Subscription(TopicName.parse(topic), app, Subscription.Type.CONSUMPTION));
        System.out.println("success");

    }

    @Test
    public void hasSubscribe() {
        String app = "joyqueue2";
        String topic = "__group_coordinators";
        boolean have = nameService.hasSubscribe(app, Subscription.Type.CONSUMPTION);
        have = nameService.hasSubscribe(app, Subscription.Type.CONSUMPTION);
        System.out.println(have);
    }

    @Test
    public void leaderReport() {
        nameService.getBroker(1);
        String topic = "lxb_test";
        int partitionGroup = 0;
        int leaderBrokerId = 1550553501;
        Set<Integer> isrId = new HashSet<>();
        int termId = 0;

        nameService.leaderReport(TopicName.parse(topic), partitionGroup, leaderBrokerId, isrId, termId);
        nameService.leaderReport(TopicName.parse(topic), partitionGroup, leaderBrokerId, isrId, termId);
        System.out.println(termId);

    }

    @Test
    public void getBroker() throws InterruptedException {
        Broker broker = nameService.getBroker(1550058289);
        broker = nameService.getBroker(1550058289);
        System.out.println(broker);

    }

    @Test
    public void getAllBrokers() {
        List<Broker> brokers = nameService.getAllBrokers();
        brokers = nameService.getAllBrokers();
        System.out.println(Arrays.toString(brokers.toArray()));
    }

    @Test
    public void addProducer() {
    }

    @Test
    public void addConsumer() {
    }

    @Test
    public void addTopic() {
    }

    @Test
    public void getTopicConfig() {
        String topic = "__group_coordinators";
        TopicConfig topicConfig = nameService.getTopicConfig(TopicName.parse(topic));
        topicConfig = nameService.getTopicConfig(TopicName.parse("lxb_test1"));
        System.out.println(topicConfig);
    }

    @Test
    public void getAllTopics() {
        Set<String> topicConfigs = nameService.getAllTopicCodes();
        topicConfigs = nameService.getAllTopicCodes();
        System.out.println(Arrays.toString(topicConfigs.toArray()));
    }

    @Test
    public void getTopics() {
        String app = "joyqueue";
        String topic = "__group_coordinators";
        Set<String> topicConfigs = nameService.getTopics(app, Subscription.Type.PRODUCTION);
        topicConfigs = nameService.getTopics(app, Subscription.Type.PRODUCTION);
        System.out.println(Arrays.toString(topicConfigs.toArray()));
    }

    @Test
    public void getTopicConfigByBroker() {
        Map<TopicName, TopicConfig> topicConfigs = nameService.getTopicConfigByBroker(1550058289);
        topicConfigs = nameService.getTopicConfigByBroker(1550058289);
        System.out.println(topicConfigs);
    }

    @Test
    public void register() {
        Broker broker = nameService.register(null, IpUtil.getLocalIp(), IpUtil.getDefaultLocalHost(), 50088, IpUtil.getNode());
        System.out.println(broker);
    }

    @Test
    public void getProducerByTopicAndApp() {
        String app = "joyqueue";
        String topic = "__group_coordinators";
        Producer producer = nameService.getProducerByTopicAndApp(TopicName.parse(topic), app);
        producer = nameService.getProducerByTopicAndApp(TopicName.parse(topic), app);
        System.out.println(producer);
    }

    @Test
    public void getConsumerByTopicAndApp() {
        String app = "poslp";
        String topic = "watch/login_state/2BD434B859B4FED39DCA90B2B09AAF57";
        Consumer consumer = nameService.getConsumerByTopicAndApp(TopicName.parse(topic), app);
        System.out.println(consumer);
    }

    @Test
    public void getTopicConfigByApp() {
        String app = "joyqueue";
        Map<TopicName, TopicConfig> topicConfigs = nameService.getTopicConfigByApp(app, Subscription.Type.CONSUMPTION);
        topicConfigs = nameService.getTopicConfigByApp(app, Subscription.Type.CONSUMPTION);
        System.out.println(topicConfigs);
    }

    @Test
    public void getDataCenter() {
        DataCenter dataCenter = nameService.getDataCenter(IpUtil.getLocalIp());
        dataCenter = nameService.getDataCenter(IpUtil.getLocalIp());
        System.out.println(dataCenter);
    }

    @Test
    public void getConfig() {
        System.out.println(nameService.getConfig("aa", "bb"));
        System.out.println(nameService.getConfig("aa", "bb"));
    }

    @Test
    public void getAllConfigs() {
        List list3 = nameService.getAllConfigs();
        List list2 = nameService.getAllBrokers();
        List list1 = nameService.getAllBrokers();

        System.out.println(Arrays.toString(list1.toArray()));
    }

    @Test
    public void getBrokerByRetryType() {
        List<Broker> list = nameService.getBrokerByRetryType(Broker.DEFAULT_RETRY_TYPE);
        list = nameService.getBrokerByRetryType(Broker.DEFAULT_RETRY_TYPE);
        System.out.println(Arrays.toString(list.toArray()));
    }

    @Test
    public void getConsumerByTopic() {
        String topic = "lxb_test";
        List<Consumer> list = nameService.getConsumerByTopic(TopicName.parse(topic));
        System.out.println(Arrays.toString(list.toArray()));

    }

    @Test
    public void getProducerByTopic() {
        String topic = "__group_coordinators";
        List<Producer> list = nameService.getProducerByTopic(TopicName.parse(topic));
        List<Producer> list2 = nameService.getProducerByTopic(TopicName.parse(topic));
        System.out.println(Arrays.toString(list.toArray()));
    }

    @Test
    public void getReplicaByBroker() {
        List<Replica> list = nameService.getReplicaByBroker(1550058289);
        List<Replica> list2 = nameService.getReplicaByBroker(1550058289);
        System.out.println(Arrays.toString(list.toArray()));
    }

    @Test
    public void getAppToken() {
        AppToken appToken = nameService.getAppToken("poslp", "2e2b2eb5-5c2e-49b0-8589-22aa71ca0c99");
        System.out.println(appToken);
    }
}
