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

import org.joyqueue.broker.joyqueue0.command.BrokerCluster;

import java.util.List;
import java.util.Map;

public class GetClusterEntity {
    private Map<String, TopicEntity> topicMapper;
    private List<BrokerCluster> brokerClusters;

    public GetClusterEntity() {

    }

    public GetClusterEntity(Map<String, TopicEntity> topicMapper, List<BrokerCluster> brokerClusters) {
        this.topicMapper = topicMapper;
        this.brokerClusters = brokerClusters;
    }

    public void setTopicMapper(Map<String, TopicEntity> topicMapper) {
        this.topicMapper = topicMapper;
    }

    public void setBrokerClusters(List<BrokerCluster> brokerClusters) {
        this.brokerClusters = brokerClusters;
    }

    public Map<String, TopicEntity> getTopicMapper() {
        return topicMapper;
    }

    public List<BrokerCluster> getBrokerClusters() {
        return brokerClusters;
    }
}