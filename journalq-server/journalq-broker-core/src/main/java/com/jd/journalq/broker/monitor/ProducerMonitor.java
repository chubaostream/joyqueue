/**
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
package com.jd.journalq.broker.monitor;

/**
 * 生产监控
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/10/10
 */
public interface ProducerMonitor {

    /**
     * 生产消息
     * @param topic
     * @param app
     * @param partitionGroup
     * @param partition
     * @param count
     * @param size
     * @param time
     */
    void onPutMessage(String topic, String app, int partitionGroup, short partition, long count, long size, long time);
}