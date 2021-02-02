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

import org.joyqueue.broker.monitor.stat.ConsumerStat;

/**
 * 消费监控
 *
 * author: gaohaoxiang
 * date: 2018/10/10
 */
public interface ConsumerMonitor {

    /**
     * 消费消息
     * @param topic
     * @param app
     * @param partitionGroup
     * @param partition
     * @param count
     * @param size
     * @param time
     */
    void onGetMessage(String topic, String app, int partitionGroup, short partition, long count, long size, double time);

    /**
     * 确认消费位置
     *
     * @param topic
     * @param app
     * @param partitionGroup
     * @param partition
     */
    void onAckMessage(String topic, String app, int partitionGroup, short partition);
    /**
     * 重试消息
     * @param topic
     * @param app
     * @param count
     * @param time
     */
    void onGetRetry(String topic, String app, long count, double time);

    /**
     * 添加重试
     * @param topic
     * @param app
     * @param count
     * @param time
     */
    void onAddRetry(String topic, String app, long count, double time);

    /**
     * 重试成功
     * @param topic
     * @param app
     * @param count
     */
    void onRetrySuccess(String topic, String app, long count);

    /**
     * 重试失败
     * @param topic
     * @param app
     * @param count
     */
    void onRetryFailure(String topic, String app, long count);

    /**
     * 消息位置被重置
     *
     * @param topic
     * @param app
     * @return
     */
    void onOffsetReset(String topic, String app, long count);

    /**
     * 消费监控统计
     *
     * @param topic
     * @param app
     * @return
     */
    ConsumerStat getConsumerStat(String topic, String app);
}