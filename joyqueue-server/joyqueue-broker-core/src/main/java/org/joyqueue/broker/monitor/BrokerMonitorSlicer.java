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
package org.joyqueue.broker.monitor;

import org.joyqueue.broker.monitor.stat.AppStat;
import org.joyqueue.broker.monitor.stat.PartitionGroupStat;
import org.joyqueue.broker.monitor.stat.PartitionStat;
import org.joyqueue.broker.monitor.stat.TopicStat;
import org.joyqueue.toolkit.service.Service;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * BrokerMonitorSlicer
 *
 * author: gaohaoxiang
 * date: 2019/6/12
 */
public class BrokerMonitorSlicer extends Service implements Runnable {

    private BrokerMonitor brokerMonitor;
    private ScheduledExecutorService sliceThread;

    public BrokerMonitorSlicer(BrokerMonitor brokerMonitor) {
        this.brokerMonitor = brokerMonitor;
    }

    @Override
    protected void validate() throws Exception {
        this.sliceThread = Executors.newScheduledThreadPool(1);
    }

    @Override
    protected void doStart() throws Exception {
        this.sliceThread.scheduleWithFixedDelay(this, 0, 1000 * 60, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doStop() {
        this.sliceThread.shutdown();
    }

    @Override
    public void run() {
        for (Map.Entry<String, TopicStat> topicEntry : brokerMonitor.getBrokerStat().getTopicStats().entrySet()) {
            TopicStat topicStat = topicEntry.getValue();
            topicStat.getEnQueueStat().slice();
            topicStat.getDeQueueStat().slice();

            for (Map.Entry<Integer, PartitionGroupStat> partitionGroupStatEntry : topicStat.getPartitionGroupStatMap().entrySet()) {
                partitionGroupStatEntry.getValue().getEnQueueStat().slice();
                partitionGroupStatEntry.getValue().getDeQueueStat().slice();
                partitionGroupStatEntry.getValue().getReplicationStat().getReplicaStat().slice();
                partitionGroupStatEntry.getValue().getReplicationStat().getAppendStat().slice();

                for (Map.Entry<Short, PartitionStat> partitionStatEntry : partitionGroupStatEntry.getValue().getPartitionStatMap().entrySet()) {
                    partitionStatEntry.getValue().getEnQueueStat().slice();
                    partitionStatEntry.getValue().getDeQueueStat().slice();
                }
            }

            for (Map.Entry<String, AppStat> appEntry : topicStat.getAppStats().entrySet()) {
                // topic
                AppStat appStat = appEntry.getValue();
                appStat.getProducerStat().getEnQueueStat().slice();
                appStat.getConsumerStat().getDeQueueStat().slice();
                appStat.getConsumerStat().getOffsetResetStat().slice();

                // app slice
                for (Map.Entry<Integer, PartitionGroupStat> partitionGroupEntry : appStat.getPartitionGroupStatMap().entrySet()) {
                    partitionGroupEntry.getValue().getEnQueueStat().slice();
                    partitionGroupEntry.getValue().getDeQueueStat().slice();
                    for (Map.Entry<Short, PartitionStat> partitionEntry : partitionGroupEntry.getValue().getPartitionStatMap().entrySet()) {
                        partitionEntry.getValue().getEnQueueStat().slice();
                        partitionEntry.getValue().getDeQueueStat().slice();
                    }
                }

                // producer slice
                for (Map.Entry<Integer, PartitionGroupStat> partitionGroupEntry : appStat.getProducerStat().getPartitionGroupStatMap().entrySet()) {
                    partitionGroupEntry.getValue().getEnQueueStat().slice();
                    partitionGroupEntry.getValue().getDeQueueStat().slice();
                    for (Map.Entry<Short, PartitionStat> partitionEntry : partitionGroupEntry.getValue().getPartitionStatMap().entrySet()) {
                        partitionEntry.getValue().getEnQueueStat().slice();
                        partitionEntry.getValue().getDeQueueStat().slice();
                    }
                }

                // consumer slice
                for (Map.Entry<Integer, PartitionGroupStat> partitionGroupEntry : appStat.getConsumerStat().getPartitionGroupStatMap().entrySet()) {
                    partitionGroupEntry.getValue().getEnQueueStat().slice();
                    partitionGroupEntry.getValue().getDeQueueStat().slice();
                    for (Map.Entry<Short, PartitionStat> partitionEntry : partitionGroupEntry.getValue().getPartitionStatMap().entrySet()) {
                        partitionEntry.getValue().getEnQueueStat().slice();
                        partitionEntry.getValue().getDeQueueStat().slice();
                    }
                }
            }
        }
        // period snapshot
        brokerMonitor.getBrokerStat().getJvmStat().snapshot();
        brokerMonitor.getBrokerStat().getReplicationStat().getAppendStat().slice();
        brokerMonitor.getBrokerStat().getReplicationStat().getReplicaStat().slice();

        brokerMonitor.getBrokerStat().getEnQueueStat().slice();
        brokerMonitor.getBrokerStat().getDeQueueStat().slice();
    }
}