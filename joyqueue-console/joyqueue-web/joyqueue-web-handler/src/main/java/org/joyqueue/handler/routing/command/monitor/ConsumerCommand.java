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
package org.joyqueue.handler.routing.command.monitor;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jd.laf.binding.annotation.Value;
import com.jd.laf.web.vertx.annotation.Body;
import com.jd.laf.web.vertx.annotation.Path;
import com.jd.laf.web.vertx.annotation.QueryParam;
import com.jd.laf.web.vertx.response.Response;
import com.jd.laf.web.vertx.response.Responses;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joyqueue.handler.Constants;
import org.joyqueue.handler.annotation.PageQuery;
import org.joyqueue.handler.error.ConfigException;
import org.joyqueue.handler.error.ErrorCode;
import org.joyqueue.handler.routing.command.NsrCommandSupport;
import org.joyqueue.model.Pagination;
import org.joyqueue.model.QPageQuery;
import org.joyqueue.model.domain.Consumer;
import org.joyqueue.model.domain.ConsumerConfig;
import org.joyqueue.model.domain.User;
import org.joyqueue.model.query.QConsumer;
import org.joyqueue.nsr.ConsumerNameServerService;
import org.joyqueue.service.ApplicationService;
import org.joyqueue.service.ApplicationUserService;
import org.joyqueue.service.ConsumerService;
import org.joyqueue.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.joyqueue.handler.routing.command.monitor.ProducerCommand.CAN_OPERATE_PROPERTY;


public class ConsumerCommand extends NsrCommandSupport<Consumer, ConsumerService, QConsumer> {
    private final Logger logger = LoggerFactory.getLogger(ConsumerCommand.class);
    @Value(nullable = false)
    private ApplicationService applicationService;
    @Value(nullable = false)
    private TopicService topicService;
    @Value(nullable = false)
    private ConsumerNameServerService consumerNameServerService;
    @Value(nullable = false)
    private ApplicationUserService applicationUserService;

    @Path("search")
    public Response pageQuery(@PageQuery QPageQuery<QConsumer> qPageQuery) throws Exception {
        QConsumer query = qPageQuery.getQuery();
        List<Consumer> consumers = new ArrayList<>(0);

        boolean appFlag = true;

        if (query.getApp() != null && query.getTopic() ==null) {
            consumers = service.findByApp(query.getApp().getCode());
        } else if (query.getTopic() != null && query.getApp() == null) {
            appFlag = false;
            consumers = service.findByTopic(query.getTopic().getCode(), query.getTopic().getNamespace().getCode());
        } else if (query.getApp() !=null && query.getTopic()!=null) {
            consumers.add(service.findByTopicAppGroup(query.getTopic().getNamespace().getCode(),query.getTopic().getCode(),query.getApp().getCode(),null));
        }

        if (CollectionUtils.isNotEmpty(consumers) && qPageQuery.getQuery() != null && StringUtils.isNotBlank(qPageQuery.getQuery().getKeyword())) {
            consumers = Lists.newArrayList(consumers);
            Iterator<Consumer> iterator = consumers.iterator();
            while (iterator.hasNext()) {
                Consumer consumer = iterator.next();
                if (appFlag) {
                    if (!consumer.getTopic().getCode().contains(qPageQuery.getQuery().getKeyword())) {
                        iterator.remove();
                    }
                } else {
                    if (!consumer.getApp().getCode().contains(qPageQuery.getQuery().getKeyword())) {
                        iterator.remove();
                    }
                }
            }
        }

        if (appFlag) {
            if (CollectionUtils.isNotEmpty(consumers) && session.getRole() != User.UserRole.ADMIN.value()) {
                Iterator<Consumer> iterator = consumers.iterator();
                while (iterator.hasNext()) {
                    Consumer consumer = iterator.next();
                    if (applicationUserService.findByUserApp(session.getCode(), consumer.getApp().getCode().split("\\.")[0]) == null) {
                        iterator.remove();
                    }
                }
            }
        }

        Pagination pagination = qPageQuery.getPagination();
        pagination.setTotalRecord(consumers.size());

        // 给producer添加是否可以操作属性
        return Responses.success(pagination, consumers.stream().map(consumer -> {
            JSONObject obj = (JSONObject) JSONObject.toJSON(consumer);
            if (session.getRole() == User.UserRole.ADMIN.value() ||
                    applicationUserService.findByUserApp(session.getCode(), consumer.getApp().getCode().split("\\.")[0]) != null) {
                obj.put(CAN_OPERATE_PROPERTY, true);
            } else {
                obj.put(CAN_OPERATE_PROPERTY, false);
            }
            return obj;
        }).collect(Collectors.toList()));
    }

    @Path("add")
    public Response add(@Body Consumer consumer) throws Exception {
        //validate unique
        Consumer currentConsumer = service.findByTopicAppGroup(consumer.getTopic().getNamespace().getCode(), consumer.getTopic().getCode(), consumer.getApp().getCode(), consumer.getSubscribeGroup());
        if (currentConsumer != null) {
            throw new ConfigException(ErrorCode.BadRequest, "consumer already exists!");
        }
        return super.add(consumer);
    }

    @Override
    @Path("delete")
    public Response delete(@QueryParam(Constants.ID) String id) throws Exception {
        Consumer consumer = service.findById(id);
        int count = service.delete(consumer);
        if (count <= 0) {
            throw new ConfigException(deleteErrorCode());
        }
        //afterDelete(model);
        return Responses.success();
    }

    @Path("queryByTopic")
    public Response queryByTopic(@Body QConsumer qConsumer) throws Exception {
        if (qConsumer.getTopic() == null || qConsumer.getTopic().getCode() == null) {
            return Responses.error(Response.HTTP_BAD_REQUEST, "Empty topic!");
        }
        String namespace = null;
        String topic = qConsumer.getTopic().getCode();
        if (null != qConsumer.getTopic().getNamespace()) {
            namespace = qConsumer.getTopic().getNamespace().getCode();
        }
        List<Consumer> consumers = service.findByTopic(topic, namespace);
        return Responses.success(consumers);
    }

    @Path("configAddOrUpdate")
    public Response configAddOrUpdate(@Body ConsumerConfig config) throws Exception {
        if (config != null) {
            Consumer consumer = service.findById(config.getConsumerId());
            mergeConsumerConfig(consumer, config);
            service.update(consumer);
        }
        return Responses.success();
    }

    private void mergeConsumerConfig(Consumer consumer, ConsumerConfig config) {
        if (consumer.getConfig() == null) {
            consumer.setConfig(config);
            return;
        }
        if (config.getAckTimeout() !=null) {
            consumer.getConfig().setAckTimeout(config.getAckTimeout());
        }
        if (config.getBackOffMultiplier() != null) {
            consumer.getConfig().setBackOffMultiplier(config.getBackOffMultiplier());
        }
        if (config.getBatchSize() != null) {
            consumer.getConfig().setBatchSize(config.getBatchSize());
        }
        if (config.getConcurrent() != null) {
            consumer.getConfig().setConcurrent(config.getConcurrent());
        }
        if (config.getDelay() != null) {
            consumer.getConfig().setDelay(config.getDelay());
        }
        if (config.getRetryDelay() != null) {
            consumer.getConfig().setRetryDelay(config.getRetryDelay());
        }
        if (config.getMaxRetrys() != null) {
            consumer.getConfig().setMaxRetrys(config.getMaxRetrys());
        }
        if (config.getMaxRetryDelay() != null) {
            consumer.getConfig().setMaxRetryDelay(config.getMaxRetryDelay());
        }
        if (config.getLimitTps() != null) {
            consumer.getConfig().setLimitTps(config.getLimitTps());
        }
        if (config.getLimitTraffic() != null) {
            consumer.getConfig().setLimitTraffic(config.getLimitTraffic());
        }
        if (StringUtils.isNotBlank(config.getRegion())) {
            consumer.getConfig().setRegion(config.getRegion());
        }
        if (StringUtils.isNotBlank(config.getBlackList())) {
            consumer.getConfig().setBlackList(config.getBlackList());
        }
        if (StringUtils.isNotBlank(config.getFilters())) {
            consumer.getConfig().setFilters(config.getFilters());
        }
        if (config.isArchive() != null) {
            consumer.getConfig().setArchive(config.isArchive());
        }
        if (config.isNearBy() != null) {
            consumer.getConfig().setNearBy(config.isNearBy());
        }
        if (config.isPaused() != null) {
            consumer.getConfig().setPaused(config.isPaused());
        }
        if (config.isRetry() != null) {
            consumer.getConfig().setRetry(config.isRetry());
        }
        if (config.isUseExponentialBackOff() != null) {
            consumer.getConfig().setUseExponentialBackOff(config.isUseExponentialBackOff());
        }
        if (MapUtils.isNotEmpty(config.getParams())) {
            if (consumer.getConfig().getParams() == null) {
                consumer.getConfig().setParams(config.getParams());
            }else  {
                consumer.getConfig().getParams().putAll(config.getParams());
            }
        }
        if (config.getOffsetMode() != null) {
            consumer.getConfig().setOffsetMode(config.getOffsetMode());
        }
        if (config.getExpireTime() != null) {
            consumer.getConfig().setExpireTime(config.getExpireTime());
        }
    }

    /**
     * 同步producer
     * @return
     * @throws Exception
     */
//    @Path("syncMqttClient")
//    public Response syncMqttConsumers() throws Exception {
//        int successCount = 0;
//        int failCount = 0;
//        List<Consumer> consumerList = consumerNameServerService.syncConsumer(ClientType.MQTT.value());
//        Map<String, Topic> topicMap = new HashMap<>();
//        Map<String, Application> appMap = new HashMap<>();
//        for (Consumer consumer : consumerList) {
//            try {
//                Topic topic = topicMap.get(consumer.getNamespace().getCode() + TopicName.TOPIC_SEPARATOR + consumer.getTopic().getCode());
//                if (null == topic) {
//                    topic = topicService.findByCode(consumer.getNamespace().getCode(), consumer.getTopic().getCode());
//                    if (null == topic) {
//                        logger.error("namespace {} topic {} 不存在", consumer.getNamespace().getCode(), consumer.getTopic().getCode());
//                        failCount++;
//                        continue;
//                    }
//                    topicMap.put(consumer.getNamespace().getCode() + TopicName.TOPIC_SEPARATOR + consumer.getTopic().getCode(), topic);
//                }
//                consumer.getTopic().setId(topic.getId());
//                consumer.getNamespace().setId(topic.getNamespace().getId());
//                Application application = appMap.get(consumer.getApp().getCode());
//                if (null == application) {
//                    application = applicationService.findByCode(consumer.getApp().getCode());
//                    if (null == application) {
//                        logger.error("application {} 不存在", consumer.getApp().getCode());
//                        failCount++;
//                        continue;
//                    }
//                    appMap.put(application.getCode(), application);
//                }
//                consumer.getApp().setId(application.getId());
//                Consumer consumerExist = service.findByTopicAppGroup(consumer.getNamespace().getCode(), consumer.getTopic().getCode(), consumer.getApp().getCode(), consumer.getSubscribeGroup());
//                if (null != consumerExist) {
//                    consumer.setId(consumerExist.getId());
////                    consumer.setCreateBy(consumerExist.getCreateBy());
////                    consumer.setUpdateBy(new Identity(session));
//                    service.update(consumer);
//                } else {
////                    consumer.setCreateBy(new Identity(session));
////                    consumer.setUpdateBy(consumer.getCreateBy());
//                    service.add(consumer);
//                }
//                successCount++;
//            } catch (Exception e) {
//                failCount++;
//                logger.error("同步consumer[{}]异常",
//                        consumer.getNamespace().getCode() +
//                                TopicName.TOPIC_SEPARATOR +
//                                consumer.getTopic().getCode() +
//                                TopicName.TOPIC_SEPARATOR +
//                                consumer.getApp() +
//                                TopicName.TOPIC_SEPARATOR + consumer.getSubscribeGroup(), e);
//            }
//        }
//        return Responses.success("同步mqtt consumer成功" + successCount + "条,失败" + failCount + "条");
//    }

    @Path("findAllSubscribeGroups")
    public Response findAllSubscribeGroups() throws Exception {
        return Responses.success(service.findAllSubscribeGroups());
    }

    @Path("findAppsByTopic")
    public Response findAppsByTopic(@QueryParam(Constants.TOPIC)String topic) throws Exception {
        return Responses.success(service.findAppsByTopic(topic));
    }

}
