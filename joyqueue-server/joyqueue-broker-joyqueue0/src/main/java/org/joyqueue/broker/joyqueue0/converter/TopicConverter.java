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
package org.joyqueue.broker.joyqueue0.converter;

import org.joyqueue.broker.joyqueue0.entity.TopicEntity;
import org.joyqueue.domain.*;

/**
 * topic转换器
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/9/3
 */
public class TopicConverter {

    public static TopicEntity toEntity(TopicConfig topicConfig, String app, Consumer consumer, Consumer.ConsumerPolicy consumerPolicy, Producer producer, Producer.ProducerPolicy producerPolicy) {
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setTopic(topicConfig.getName().getFullName());
        topicEntity.setImportance(2);
        topicEntity.setQueues(topicConfig.getPartitions());
        topicEntity.setType(topicConfig.getType());
        if (consumer != null) {
            if (consumer.getTopicType().equals(TopicType.BROADCAST)) {
                topicEntity.setType(Topic.Type.BROADCAST);
            }
        }
        topicEntity.getProducers().put(app, producerPolicy);
        topicEntity.getConsumers().put(app, consumerPolicy);
        return topicEntity;
    }
}