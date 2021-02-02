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
package org.joyqueue.broker.joyqueue0.entity;

import com.google.common.collect.Maps;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.Producer;
import org.joyqueue.domain.Topic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * topic实体
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/9/3
 */
public class TopicEntity {

    private String topic;
    private Integer importance;
    private Boolean archive;
    private Short queues;
    private Topic.Type type;
    private Set<String> groups = new HashSet<String>();
    private Map<String, Producer.ProducerPolicy> producers = Maps.newHashMap();
    private Map<String, Consumer.ConsumerPolicy> consumers = Maps.newHashMap();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Boolean getArchive() {
        return archive;
    }

    public void setArchive(Boolean archive) {
        this.archive = archive;
    }

    public Short getQueues() {
        return queues;
    }

    public void setQueues(Short queues) {
        this.queues = queues;
    }

    public Topic.Type getType() {
        return type;
    }

    public void setType(Topic.Type type) {
        this.type = type;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Map<String, Producer.ProducerPolicy> getProducers() {
        return producers;
    }

    public void setProducers(Map<String, Producer.ProducerPolicy> producers) {
        this.producers = producers;
    }

    public Map<String, Consumer.ConsumerPolicy> getConsumers() {
        return consumers;
    }

    public void setConsumers(Map<String, Consumer.ConsumerPolicy> consumers) {
        this.consumers = consumers;
    }
}