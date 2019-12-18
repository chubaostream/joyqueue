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
package io.chubao.joyqueue.client.internal.producer.helper;

import com.google.common.collect.Lists;
import io.chubao.joyqueue.client.internal.metadata.domain.PartitionMetadata;
import io.chubao.joyqueue.client.internal.metadata.domain.TopicMetadata;
import io.chubao.joyqueue.client.internal.producer.PartitionSelector;
import io.chubao.joyqueue.client.internal.producer.domain.ProduceMessage;

import java.util.Iterator;
import java.util.List;

/**
 * ProducerHelper
 *
 * author: gaohaoxiang
 * date: 2018/12/26
 */
public class ProducerHelper {

    public static void setPartitions(List<ProduceMessage> messages, short partition) {
        for (ProduceMessage message : messages) {
            message.setPartition(partition);
        }
    }

    public static void clearPartitions(List<ProduceMessage> messages) {
        for (ProduceMessage message : messages) {
            clearPartition(message);
        }
    }

    public static void clearPartition(ProduceMessage message) {
        message.setPartition(ProduceMessage.NONE_PARTITION);
        message.setPartitionKey(ProduceMessage.NONE_PARTITION_KEY);
    }

    public static PartitionMetadata dispatchPartitions(List<ProduceMessage> messages, TopicMetadata topicMetadata, List<PartitionMetadata> partitions, PartitionSelector partitionSelector) {
        return partitionSelector.select(messages.get(0), topicMetadata, partitions);
    }

    public static List<PartitionMetadata> filterBlackList(List<PartitionMetadata> partitions, List<PartitionMetadata> blackPartitionList) {
        List<PartitionMetadata> newPartitions = Lists.newArrayList(partitions);
        Iterator<PartitionMetadata> newPartitionIterator = newPartitions.iterator();
        while (newPartitionIterator.hasNext()) {
            PartitionMetadata newPartition = newPartitionIterator.next();
            for (PartitionMetadata blackPartition : blackPartitionList) {
                if (blackPartition.getPartitionGroupId() == newPartition.getPartitionGroupId()) {
                    newPartitionIterator.remove();
                    break;
                }
            }
        }
        return newPartitions;
    }

}