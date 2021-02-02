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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.joyqueue.domain.AppToken;
import org.joyqueue.domain.Broker;
import org.joyqueue.domain.Config;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.domain.Namespace;
import org.joyqueue.domain.PartitionGroup;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Replica;
import org.joyqueue.domain.Topic;
import org.joyqueue.domain.TopicName;
import org.joyqueue.model.PageResult;
import org.joyqueue.model.QPageQuery;
import org.joyqueue.nsr.model.AppTokenQuery;
import org.joyqueue.nsr.model.BrokerQuery;
import org.joyqueue.nsr.model.ConfigQuery;
import org.joyqueue.nsr.model.ConsumerQuery;
import org.joyqueue.nsr.model.DataCenterQuery;
import org.joyqueue.nsr.model.PartitionGroupQuery;
import org.joyqueue.nsr.model.ProducerQuery;
import org.joyqueue.nsr.model.ReplicaQuery;
import org.joyqueue.nsr.model.TopicQuery;
import org.joyqueue.nsr.service.AppTokenService;
import org.joyqueue.nsr.service.BrokerService;
import org.joyqueue.nsr.service.ConfigService;
import org.joyqueue.nsr.service.ConsumerService;
import org.joyqueue.nsr.service.DataCenterService;
import org.joyqueue.nsr.service.NamespaceService;
import org.joyqueue.nsr.service.PartitionGroupReplicaService;
import org.joyqueue.nsr.service.PartitionGroupService;
import org.joyqueue.nsr.service.ProducerService;
import org.joyqueue.nsr.service.TopicService;
import org.joyqueue.toolkit.service.Service;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.vertx.core.http.HttpHeaders.CONTENT_ENCODING;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author wylixiaobin
 * Date: 2018/9/20
 */
public class ManageServer extends Service {
    private static final Logger logger = LoggerFactory.getLogger(ManageServer.class);

    public static final String APPLICATION_JSON = "application/json";
    public static final String ENCODING="utf-8";

    private HttpServer server;
    private Vertx vertx;
    private TopicService topicService;
    private ProducerService producerService;
    private ConsumerService consumerService;
    private BrokerService brokerService;
    private ConfigService configService;
    private DataCenterService dataCenterService;
    private AppTokenService appTokenService;
    private NamespaceService namespaceService;
    private PartitionGroupService partitionGroupService;
    private PartitionGroupReplicaService partitionGroupReplicaService;
    private int managerPort = 0;
    public ManageServer(TopicService topicService,
                        ProducerService producerService,
                        ConsumerService consumerService,
                        BrokerService brokerService,
                        ConfigService configService,
                        AppTokenService appTokenService,
                        DataCenterService dataCenterService,
                        NamespaceService namespaceService,
                        PartitionGroupService partitionGroupService,
                        PartitionGroupReplicaService partitionGroupReplicaService) {
        this.topicService = topicService;
        this.producerService = producerService;
        this.consumerService = consumerService;
        this.brokerService = brokerService;
        this.configService = configService;
        this.appTokenService = appTokenService;
        this.dataCenterService = dataCenterService;
        this.namespaceService = namespaceService;
        this.partitionGroupService = partitionGroupService;
        this.partitionGroupReplicaService = partitionGroupReplicaService;
    }

    public void setManagerPort(int managerPort) {
        this.managerPort = managerPort;
    }

    @Override
    public void doStart() throws Exception {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        /** topic  **/
        router.post("/topic/getByCode").handler(routingContext -> {
            try{
                TopicQuery topicQuery = JSON.parseObject(routingContext.getBodyAsString(), TopicQuery.class);
                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(topicService.getTopicByCode(topicQuery.getNamespace(), topicQuery.getCode())));
            }catch (Exception e){
                logger.error("topic add error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/getById").handler(routingContext -> {
            try{
                String bodyTxt = routingContext.getBodyAsString();
                Topic topic = topicService.getById(JSON.parseObject(bodyTxt,String.class));
                routingContext.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(topic));
            }catch (Exception e){
                logger.error("topic add error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/findUnsubscribedByQuery").handler(routingContext -> {
            try{
                String bodyTxt = routingContext.getBodyAsString();
                PageResult<Topic> pageResult = topicService.findUnsubscribedByQuery(JSON.parseObject(bodyTxt,new TypeReference<QPageQuery<TopicQuery>>(){}));
                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(pageResult));
            }catch (Exception e){
                logger.error("topic findByQuery error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/search").handler(routingContext -> {
            try{
                String bodyTxt = routingContext.getBodyAsString();
                PageResult<Topic> pageResult = topicService.search(JSON.parseObject(bodyTxt,new TypeReference<QPageQuery<TopicQuery>>(){}));
                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(pageResult));
            }catch (Exception e){
                logger.error("topic findByQuery error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/add").handler(routingContext -> {
            try{
                String bodyTxt = routingContext.getBodyAsString();
                JSONObject json = JSONObject.parseObject(bodyTxt);
                Topic topic = JSONObject.parseObject(json.getString("topic"), Topic.class);
                List<PartitionGroup> list = JSONObject.parseArray(json.getString("partitionGroups"), PartitionGroup.class);
                topicService.addTopic(topic, list);
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("topic add error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/remove").handler(routingContext -> {
            try{
                topicService.removeTopic(JSONObject.parseObject(routingContext.getBodyAsString(), Topic.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("topic remove error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/update").handler(routingContext -> {
            try{
                topicService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Topic.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("topic update error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/addPartitionGroup").handler(routingContext -> {
            try{
                topicService.addPartitionGroup(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("topic addPartitionGroup error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/removePartitionGroup").handler(routingContext -> {
            try{
            topicService.removePartitionGroup(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class));
            routingContext.response().end("success");
            }catch (Exception e){
                logger.error("topic removePartitionGroup error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/updatePartitionGroup").handler(routingContext -> {
            try {
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(topicService.updatePartitionGroup(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class))));
            } catch (Exception e) {
                logger.error("topic updatePartitionGroup error,request[{}]", routingContext.getBodyAsString(), e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/leaderChange").handler(routingContext -> {
            try{
                topicService.leaderChange(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class));
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end("success");
            }catch (Exception e){
                logger.error("topic leaderChange error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/topic/getPartitionGroup").handler(routingContext -> {
            try {
                JSONObject json = JSONObject.parseObject(routingContext.getBodyAsString());
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(topicService.getPartitionGroup(json.getString("namespace"), json.getString("topic"), json.getJSONArray("groups").toArray())));
            } catch (Exception e) {
                logger.error("topic getPartitionGroup error,request[{}]", routingContext.getBodyAsString(), e);
                routingContext.fail(e);
            }
        });


        /** producer  **/
        router.post("/producer/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(producerService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("producer getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/producer/add").handler(routingContext -> {
            try {
                producerService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Producer.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("producer add error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/producer/update").handler(routingContext -> {
            try{
            producerService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Producer.class));
            routingContext.response().end("success");
            }catch (Exception e){
                logger.error("producer update error,request",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/producer/remove").handler(routingContext -> {
            try {
                producerService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Producer.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("producer remove error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/producer/getByTopic").handler(routingContext -> {
            try{
                ProducerQuery producerQuery = JSON.parseObject(routingContext.getBodyAsString(), ProducerQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                    end(JSONArray.toJSONString(producerService.getByTopic(TopicName.parse(producerQuery.getTopic(), producerQuery.getNamespace()))));
            }catch (Exception e){
                logger.error("producer list error",e);
                routingContext.fail(e);
            }
        });
        router.post("/producer/getByTopicAndApp").handler(routingContext -> {
            try{
                ProducerQuery producerQuery = JSON.parseObject(routingContext.getBodyAsString(), ProducerQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(producerService.getByTopicAndApp(TopicName.parse(producerQuery.getTopic(), producerQuery.getNamespace()), producerQuery.getApp())));
            }catch (Exception e){
                logger.error("producer getlist error request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });

        router.post("/producer/getByApp").handler(routingContext -> {
            try {
                ProducerQuery producerQuery = JSON.parseObject(routingContext.getBodyAsString(), ProducerQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(producerService.getByApp(producerQuery.getApp())));
            } catch (Exception e){
                logger.error("producer findByQuery error",e);
                routingContext.fail(e);
            }
        });

        /** consumer **/
        router.post("/consumer/getById").handler(routingContext -> {
            try {
                Consumer consumer = consumerService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class));
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(consumer));
            }catch (Exception e){
                logger.error("consumer getById error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/getByTopicAndApp").handler(routingContext -> {
            try {
                ConsumerQuery consumerQuery = JSON.parseObject(routingContext.getBodyAsString(), ConsumerQuery.class);
                Consumer consumer = consumerService.getByTopicAndApp(TopicName.parse(consumerQuery.getTopic(), consumerQuery.getNamespace()), consumerQuery.getApp());
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(consumer));
            }catch (Exception e){
                logger.error("consumer getByTopicAndApp error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/add").handler(routingContext -> {
            try{
            consumerService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Consumer.class));
            routingContext.response().end("success");
            }catch (Exception e){
                logger.error("consumer add error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/update").handler(routingContext -> {
            try {
                consumerService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Consumer.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("consumer update error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/remove").handler(routingContext -> {
            try {
                consumerService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Consumer.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("consumer remove error,request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/getByTopic").handler(routingContext -> {
            try{
                ConsumerQuery consumerQuery = JSON.parseObject(routingContext.getBodyAsString(), ConsumerQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(consumerService.getByTopic(TopicName.parse(consumerQuery.getTopic(), consumerQuery.getNamespace()))));
            }catch (Exception e){
                logger.error("consumer getByTopic error request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/getByApp").handler(routingContext -> {
            try{
                ConsumerQuery consumerQuery = JSON.parseObject(routingContext.getBodyAsString(), ConsumerQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(consumerService.getByApp(consumerQuery.getApp())));
            }catch (Exception e){
                logger.error("consumer getByApp error request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/consumer/list").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).
                        end(JSONArray.toJSONString(consumerService.getAll()));
            }catch (Exception e){
                logger.error("consumer list error request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });

        /** config **/
        router.post("/config/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(configService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("config getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/config/getByGroupAndKey").handler(routingContext -> {
            try{
                ConfigQuery configQuery = JSON.parseObject(routingContext.getBodyAsString(), ConfigQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(configService.getByGroupAndKey(configQuery.getGroup(), configQuery.getKey())));
            }catch (Exception e){
                logger.error("config getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/config/add").handler(routingContext -> {
            try{
            configService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Config.class));
            routingContext.response().end("success");
            }catch (Exception e){
                logger.error("config add error,requset[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/config/update").handler(routingContext -> {
            try {
                configService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Config.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("config update error request[{}]",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/config/remove").handler(routingContext -> {
            try {
                configService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Config.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("config remove [{}] error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/config/list").handler(routingContext -> {
            try {
                List<Config> configs = configService.getAll();
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(configs));
            }catch (Exception e){
                logger.error("config config [{}] error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });

        /**  broker **/
        router.post("/broker/getById").handler(routingContext -> {
            try {
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(brokerService.getById(JSON.parseObject(routingContext.getBodyAsString(),Integer.class))));
            }catch (Exception e){
                logger.error("broker  getById error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/getByIds").handler(routingContext -> {
            try {
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(brokerService.getByIds(JSON.parseArray(routingContext.getBodyAsString(),Integer.class))));
            }catch (Exception e){
                logger.error("broker  getById error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/add").handler(routingContext -> {
            try{
            brokerService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Broker.class));
            routingContext.response().end("success");
            }catch (Exception e){
                logger.error("broker[{}] add error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/update").handler(routingContext -> {
            try{
                brokerService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Broker.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("broker[{}] add error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/remove").handler(routingContext -> {
            try {
                brokerService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Broker.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("broker[{}] remove error",routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/list").handler(routingContext -> {
            try {
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSONArray.toJSONString(brokerService.getAll()));
            }catch (Exception e){
                logger.error("broker list error",e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/search").handler(routingContext -> {
            try {
                QPageQuery<BrokerQuery> pageQuery = JSONObject.parseObject(routingContext.getBodyAsString(), new TypeReference<QPageQuery<BrokerQuery>>(){});
                PageResult<Broker> pageResult= brokerService.search(pageQuery);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(pageResult));
            } catch (Exception e) {
                logger.error("broker findByQuery error",e);
                routingContext.fail(e);
            }
        });
        router.post("/broker/updateExternalIpByIp").handler(routingContext -> {
            try {
                BrokerQuery query = JSONObject.parseObject(routingContext.getBodyAsString(), new TypeReference<BrokerQuery>(){});
                Broker broker = brokerService.getByIpAndPort(query.getIp(), query.getPort());
                broker.setExternalIp(query.getExternalIp());
                broker.setExternalPort(query.getExternalPort());
                broker.setNode(query.getNode());
                brokerService.update(broker);
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("broker updateExternalIpByIp error", routingContext.getBodyAsString(),e);
                routingContext.fail(e);
            }
        });
        /**   apptoken    **/
        router.post("/apptoken/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(appTokenService.getById(JSON.parseObject(routingContext.getBodyAsString(),Long.class))));
            }catch (Exception e){
                logger.error("apptoken getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/apptoken/add").handler(routingContext -> {

            try {
                appTokenService.add(JSONObject.parseObject(routingContext.getBodyAsString(), AppToken.class));
                routingContext.response().end("success");
            }catch (Exception e){
            logger.error("apptoken add error",e);
            routingContext.fail(e);
            }
        });
        router.post("/apptoken/update").handler(routingContext -> {
            try {
                appTokenService.update(JSONObject.parseObject(routingContext.getBodyAsString(), AppToken.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("apptoken update error",e);
                routingContext.fail(e);
            }

        });
        router.post("/apptoken/remove").handler(routingContext -> {
            try {
                appTokenService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), AppToken.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
            logger.error("apptoken remove error",e);
            routingContext.fail(e);
        }
        });
        router.post("/apptoken/findByApp").handler(routingContext -> {
            try {
                AppTokenQuery appTokenQuery = JSONObject.parseObject(routingContext.getBodyAsString(), AppTokenQuery.class);
                List<AppToken> appTokenList= appTokenService.getByApp(appTokenQuery.getApp());
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(appTokenList));
            } catch (Exception e){
                logger.error("apptoken list error",e);
                routingContext.fail(e);
            }
        });
        router.post("/apptoken/findByAppAndToken").handler(routingContext -> {
            try {
                QPageQuery<AppTokenQuery> pageQuery = JSONObject.parseObject(routingContext.getBodyAsString(), new TypeReference<QPageQuery<AppTokenQuery>>(){});
                AppToken appToken = appTokenService.getByAppAndToken(pageQuery.getQuery().getApp(), pageQuery.getQuery().getToken());
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(appToken));
            } catch (Exception e){
                logger.error("apptoken findByQuery error",e);
                routingContext.fail(e);
            }
        });

        /** datacenter **/
        router.post("/datacenter/list").handler(routingContext -> {
            try {
                DataCenterQuery dataCenterQuery = JSONObject.parseObject(routingContext.getBodyAsString(), DataCenterQuery.class);
                List<DataCenter> dataCenters = dataCenterService.getAll();
                routingContext.response().putHeader(CONTENT_TYPE,APPLICATION_JSON).putHeader(CONTENT_ENCODING,ENCODING).end(JSON.toJSONString(dataCenters));
            } catch (Exception e) {
                logger.error("datacenter list error",e);
                routingContext.fail(e);
            }
        });
        router.post("/datacenter/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).putHeader(CONTENT_ENCODING,ENCODING)
                        .end(JSON.toJSONString(dataCenterService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("datacenter getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/datacenter/add").handler(routingContext -> {
            try {
                dataCenterService.add(JSONObject.parseObject(routingContext.getBodyAsString(), DataCenter.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("datacenter add error",e);
                routingContext.fail(e);
            }
        });
        router.post("/datacenter/update").handler(routingContext -> {
            try {
                dataCenterService.update(JSONObject.parseObject(routingContext.getBodyAsString(), DataCenter.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("datacenter update error",e);
                routingContext.fail(e);
            }

        });
        router.post("/datacenter/remove").handler(routingContext -> {
            try {
                dataCenterService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), DataCenter.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("datacenter remove error",e);
                routingContext.fail(e);
            }
        });

        /** namespace **/
        router.post("/namespace/list").handler(routingContext -> {
            try {
                List<Namespace> namespaces = namespaceService.getAll();
                routingContext.response().putHeader(CONTENT_TYPE,APPLICATION_JSON).end(JSON.toJSONString(namespaces));
            } catch (Exception e) {
                logger.error("namespace list error",e);
                routingContext.fail(e);
            }
        });
        router.post("/namespace/getByCode").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(namespaceService.getByCode(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("namespace getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/namespace/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(namespaceService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("namespace getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/namespace/add").handler(routingContext -> {
            try {
                namespaceService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Namespace.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("namespace add error",e);
                routingContext.fail(e);
            }
        });
        router.post("/namespace/update").handler(routingContext -> {
            try {
                namespaceService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Namespace.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("namespace update error",e);
                routingContext.fail(e);
            }

        });
        router.post("/namespace/remove").handler(routingContext -> {
            try {
                namespaceService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Namespace.class).getCode());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("namespace remove error",e);
                routingContext.fail(e);
            }
        });


        /** partitionGroup **/
        router.post("/partitiongroup/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(partitionGroupService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("partitiongroup getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/partitiongroup/add").handler(routingContext -> {
            try {
                partitionGroupService.add(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("partitiongroup add error",e);
                routingContext.fail(e);
            }
        });
        router.post("/partitiongroup/update").handler(routingContext -> {
            try {
                partitionGroupService.update(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("partitiongroup update error",e);
                routingContext.fail(e);
            }

        });
        router.post("/partitiongroup/remove").handler(routingContext -> {
            try {
                partitionGroupService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroup.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("partitiongroup remove error",e);
                routingContext.fail(e);
            }
        });
        router.post("/partitiongroup/getByTopicAndGroup").handler(routingContext -> {
            try {
                PartitionGroupQuery query = JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroupQuery.class);
                PartitionGroup partitionGroup = partitionGroupService.getByTopicAndGroup(TopicName.parse(query.getTopic(), query.getNamespace()), query.getGroup());
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(partitionGroup));
            } catch (Exception e){
                logger.error("partitiongroup getByTopicAndGroup error",e);
                routingContext.fail(e);
            }
        });
        router.post("/partitiongroup/getByTopic").handler(routingContext -> {
            try {
                PartitionGroupQuery query = JSONObject.parseObject(routingContext.getBodyAsString(), PartitionGroupQuery.class);
                List<PartitionGroup> result = partitionGroupService.getByTopic(TopicName.parse(query.getTopic(), query.getNamespace()));
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(JSON.toJSONString(result));
            } catch (Exception e){
                logger.error("partitiongroup getByTopicAndGroup error",e);
                routingContext.fail(e);
            }
        });


        /** replica **/
        router.post("/replica/getByBroker").handler(routingContext -> {
            try{
                ReplicaQuery replicaQuery = JSONObject.parseObject(routingContext.getBodyAsString(), ReplicaQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(partitionGroupReplicaService.getByBrokerId(replicaQuery.getBrokerId())));
            }catch (Exception e){
                logger.error("replica getByBroker error",e);
                routingContext.fail(e);
            }
        });
        router.post("/replica/getByTopic").handler(routingContext -> {
            try{
                ReplicaQuery replicaQuery = JSONObject.parseObject(routingContext.getBodyAsString(), ReplicaQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(partitionGroupReplicaService.getByTopic(TopicName.parse(replicaQuery.getTopic(), replicaQuery.getNamespace()))));
            }catch (Exception e){
                logger.error("replica getByTopic error",e);
                routingContext.fail(e);
            }
        });
        router.post("/replica/getByTopicAndGroup").handler(routingContext -> {
            try{
                ReplicaQuery replicaQuery = JSONObject.parseObject(routingContext.getBodyAsString(), ReplicaQuery.class);
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(partitionGroupReplicaService.getByTopicAndGroup(TopicName.parse(replicaQuery.getTopic(), replicaQuery.getNamespace()), replicaQuery.getGroup())));
            }catch (Exception e){
                logger.error("replica getByTopicAndGroup error",e);
                routingContext.fail(e);
            }
        });
        router.post("/replica/getById").handler(routingContext -> {
            try{
                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(JSON.toJSONString(partitionGroupReplicaService.getById(JSON.parseObject(routingContext.getBodyAsString(),String.class))));
            }catch (Exception e){
                logger.error("replica getById error",e);
                routingContext.fail(e);
            }
        });
        router.post("/replica/add").handler(routingContext -> {
            try {
                partitionGroupReplicaService.add(JSONObject.parseObject(routingContext.getBodyAsString(), Replica.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("replica add error",e);
                routingContext.fail(e);
            }
        });
        router.post("/replica/update").handler(routingContext -> {
            try {
                partitionGroupReplicaService.update(JSONObject.parseObject(routingContext.getBodyAsString(), Replica.class));
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("replica update error",e);
                routingContext.fail(e);
            }

        });
        router.post("/replica/remove").handler(routingContext -> {
            try {
                partitionGroupReplicaService.delete(JSONObject.parseObject(routingContext.getBodyAsString(), Replica.class).getId());
                routingContext.response().end("success");
            }catch (Exception e){
                logger.error("replica remove error",e);
                routingContext.fail(e);
            }
        });

        server.requestHandler(request -> router.accept(request)).listen(managerPort);
    }

    @Override
    public void doStop() {
        if (server != null) {
            server.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }


}
