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
package io.chubao.joyqueue.nsr.composition.service;

import io.chubao.joyqueue.domain.Replica;
import io.chubao.joyqueue.domain.TopicName;
import io.chubao.joyqueue.nsr.composition.config.CompositionConfig;
import io.chubao.joyqueue.nsr.service.internal.PartitionGroupReplicaInternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CompositionPartitionGroupReplicaInternalService
 * author: gaohaoxiang
 * date: 2019/8/12
 */
public class CompositionPartitionGroupReplicaInternalService implements PartitionGroupReplicaInternalService {

    protected static final Logger logger = LoggerFactory.getLogger(CompositionPartitionGroupReplicaInternalService.class);

    private CompositionConfig config;
    private PartitionGroupReplicaInternalService ignitePartitionGroupReplicaService;
    private PartitionGroupReplicaInternalService journalkeeperPartitionGroupReplicaService;

    public CompositionPartitionGroupReplicaInternalService(CompositionConfig config, PartitionGroupReplicaInternalService ignitePartitionGroupReplicaService,
                                                           PartitionGroupReplicaInternalService journalkeeperPartitionGroupReplicaService) {
        this.config = config;
        this.ignitePartitionGroupReplicaService = ignitePartitionGroupReplicaService;
        this.journalkeeperPartitionGroupReplicaService = journalkeeperPartitionGroupReplicaService;
    }

    @Override
    public List<Replica> getByTopic(TopicName topic) {
        if (config.isReadIgnite()) {
            return ignitePartitionGroupReplicaService.getByTopic(topic);
        } else {
            try {
                return journalkeeperPartitionGroupReplicaService.getByTopic(topic);
            } catch (Exception e) {
                logger.error("getByTopic exception, topic: {}", topic, e);
                return ignitePartitionGroupReplicaService.getByTopic(topic);
            }
        }
    }

    @Override
    public List<Replica> getByTopicAndGroup(TopicName topic, int groupNo) {
        if (config.isReadIgnite()) {
            return ignitePartitionGroupReplicaService.getByTopicAndGroup(topic, groupNo);
        } else {
            try {
                return journalkeeperPartitionGroupReplicaService.getByTopicAndGroup(topic, groupNo);
            } catch (Exception e) {
                logger.error("getByTopicAndGroup exception, topic: {}, groupNo: {}", topic, groupNo, e);
                return ignitePartitionGroupReplicaService.getByTopicAndGroup(topic, groupNo);
            }
        }
    }

    @Override
    public List<Replica> getByBrokerId(Integer brokerId) {
        if (config.isReadIgnite()) {
            return ignitePartitionGroupReplicaService.getByBrokerId(brokerId);
        } else {
            try {
                return journalkeeperPartitionGroupReplicaService.getByBrokerId(brokerId);
            } catch (Exception e) {
                logger.error("getByBrokerId exception, brokerId: {}", brokerId, e);
                return ignitePartitionGroupReplicaService.getByBrokerId(brokerId);
            }
        }
    }

    @Override
    public Replica getById(String id) {
        if (config.isReadIgnite()) {
            return ignitePartitionGroupReplicaService.getById(id);
        } else {
            try {
                return journalkeeperPartitionGroupReplicaService.getById(id);
            } catch (Exception e) {
                logger.error("getById exception", e);
                return ignitePartitionGroupReplicaService.getById(id);
            }
        }
    }

    @Override
    public List<Replica> getAll() {
        if (config.isReadIgnite()) {
            return ignitePartitionGroupReplicaService.getAll();
        } else {
            try {
                return journalkeeperPartitionGroupReplicaService.getAll();
            } catch (Exception e) {
                logger.error("getAll exception", e);
                return ignitePartitionGroupReplicaService.getAll();
            }
        }
    }

    @Override
    public Replica add(Replica replica) {
        Replica result = null;
        if (config.isWriteIgnite()) {
            result = ignitePartitionGroupReplicaService.add(replica);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperPartitionGroupReplicaService.add(replica);
            } catch (Exception e) {
                logger.error("add journalkeeper exception, params: {}", replica, e);
            }
        }
        return result;
    }

    @Override
    public Replica update(Replica replica) {
        Replica result = null;
        if (config.isWriteIgnite()) {
            result = ignitePartitionGroupReplicaService.update(replica);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperPartitionGroupReplicaService.update(replica);
            } catch (Exception e) {
                logger.error("update journalkeeper exception, params: {}", replica, e);
            }
        }
        return result;
    }

    @Override
    public void delete(String id) {
        if (config.isWriteIgnite()) {
            ignitePartitionGroupReplicaService.delete(id);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperPartitionGroupReplicaService.delete(id);
            } catch (Exception e) {
                logger.error("delete journalkeeper exception, params: {}", id, e);
            }
        }
    }
}
