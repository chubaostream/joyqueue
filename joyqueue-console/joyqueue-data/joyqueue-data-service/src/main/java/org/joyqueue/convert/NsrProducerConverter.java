/**
 * Copyright 2019 The JoyQueue Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.convert;

import org.joyqueue.domain.ClientType;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.domain.Namespace;
import org.joyqueue.model.domain.Producer;
import org.joyqueue.model.domain.ProducerConfig;
import org.joyqueue.model.domain.Topic;
import org.joyqueue.util.NullUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by wangxiaofei1 on 2019/1/2.
 */
public class NsrProducerConverter extends Converter<Producer, org.joyqueue.domain.Producer> {

    @Override
    protected org.joyqueue.domain.Producer forward(Producer producer) {
        org.joyqueue.domain.Producer nsrProducer = new org.joyqueue.domain.Producer();
        nsrProducer.setApp(producer.getApp().getCode());
        nsrProducer.setClientType(ClientType.valueOf(producer.getClientType()));
        nsrProducer.setTopic(CodeConverter.convertTopic(producer.getNamespace(), producer.getTopic()));
        if (!NullUtil.isEmpty(producer.getConfig())) {
            org.joyqueue.domain.Producer.ProducerPolicy producerPolicy = org.joyqueue.domain.Producer.ProducerPolicy.Builder.build()
                    .nearby(producer.getConfig().isNearBy())
                    .single(producer.getConfig().isSingle())
                    .blackList(producer.getConfig().getBlackList())
                    .archive(producer.getConfig().isArchive())
                    .weight(producer.getConfig().getWeight())
                    .timeout(producer.getConfig().getTimeout())
                    .qosLevel(producer.getConfig().getQosLevel())
                    .region(producer.getConfig().getRegion())
                    .create();
            if (producer.getConfig().getParams() != null) {
                producerPolicy.setParams(producer.getConfig().getParams());
            }
            nsrProducer.setProducerPolicy(producerPolicy);
            nsrProducer.setLimitPolicy(new org.joyqueue.domain.Producer.ProducerLimitPolicy(producer.getConfig().getLimitTps(), producer.getConfig().getLimitTraffic()));
        }
        return nsrProducer;
    }

    @Override
    protected Producer backward(org.joyqueue.domain.Producer nsrProducer) {
        Producer producer = new Producer();
        producer.setId(nsrProducer.getId());
        producer.setApp(new Identity(nsrProducer.getApp()));
        producer.setClientType(nsrProducer.getClientType().value());
        producer.setTopic(new Topic(nsrProducer.getTopic().getCode()));
        producer.setNamespace(new Namespace(nsrProducer.getTopic().getNamespace()));
        producer.getTopic().setNamespace(producer.getNamespace());
        ProducerConfig producerConfig = new ProducerConfig();
        org.joyqueue.domain.Producer.ProducerPolicy producerPolicy = nsrProducer.getProducerPolicy();
        org.joyqueue.domain.Producer.ProducerLimitPolicy limitPolicy = nsrProducer.getLimitPolicy();
        if (producerPolicy != null) {
            producerConfig.setTimeout(producerPolicy.getTimeOut());
            producerConfig.setNearBy(producerPolicy.getNearby());
            producerConfig.setBlackList(StringUtils.join(producerPolicy.getBlackList(), ","));
            producerConfig.setArchive(producerPolicy.getArchive());
            producerConfig.setSingle(producerPolicy.isSingle());
            producerConfig.setQosLevel(producerPolicy.getQosLevel());
            producerConfig.setRegion(producerPolicy.getRegion());
            Map<String, Short> map = producerPolicy.getWeight();
            if (map != null) {
                List<String> weightList = map.entrySet().stream().map(entry -> (entry.getKey() + ":" + entry.getValue())).collect(Collectors.toList());
                producerConfig.setWeight(StringUtils.join(weightList, ","));
            }
            if (producerPolicy.getParams() != null) {
                producerConfig.setParams(producerPolicy.getParams());
            }
        }
        if (limitPolicy != null) {
            producerConfig.setLimitTps(limitPolicy.getTps());
            producerConfig.setLimitTraffic(limitPolicy.getTraffic());
        }
        producer.setConfig(producerConfig);

        return producer;
    }
}
