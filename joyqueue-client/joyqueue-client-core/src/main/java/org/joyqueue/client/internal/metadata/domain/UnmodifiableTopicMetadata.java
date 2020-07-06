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
package org.joyqueue.client.internal.metadata.domain;

import org.joyqueue.domain.ConsumerPolicy;
import org.joyqueue.domain.ProducerPolicy;
import org.joyqueue.domain.TopicType;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.network.domain.BrokerNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * UnmodifiableTopicMetadata
 *
 * author: gaohaoxiang
 * date: 2019/1/22
 */
@Deprecated
public class UnmodifiableTopicMetadata extends TopicMetadata {

    public UnmodifiableTopicMetadata(String topic, ProducerPolicy producerPolicy, ConsumerPolicy consumerPolicy, TopicType type, List<PartitionGroupMetadata> partitionGroups,
                                     List<PartitionMetadata> partitions, Map<Short, PartitionMetadata> partitionMap, Map<Integer, PartitionGroupMetadata> partitionGroupMap, List<BrokerNode> brokers,
                                     List<BrokerNode> nearbyBrokers, Map<Integer, BrokerNode> brokerMap, Map<Integer, List<PartitionMetadata>> brokerPartitions,
                                     Map<Integer, List<PartitionGroupMetadata>> brokerPartitionGroups, boolean allAvailable, JoyQueueCode code) {
        super(topic, producerPolicy, consumerPolicy, type, partitionGroups, partitions, partitionMap, partitionGroupMap,
                brokers, nearbyBrokers, brokerMap, brokerPartitions, brokerPartitionGroups, allAvailable, code);
    }

    @Override
    public List<PartitionMetadata> getPartitions() {
        return Collections.unmodifiableList(super.getPartitions());
    }

    public List<PartitionGroupMetadata> getPartitionGroups() {
        return Collections.unmodifiableList(super.getPartitionGroups());
    }

    public List<BrokerNode> getBrokers() {
        return Collections.unmodifiableList(super.getBrokers());
    }
}