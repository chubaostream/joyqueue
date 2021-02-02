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
package org.joyqueue.broker.joyqueue0.command;

/**
 * 消费位置重置.
 *
 * @author lindeqiang
 * @since 2016/9/14 10:28
 */
public class OffsetItem {
    // 主题
    private String topic;
    // 消费者
    private String app;
    // 队列ID
    private short queueId;
    // 消费者ID
    private String consumerId;
    // 队列偏移位置
    private long queueOffset;
    // 日志偏移量
    private long journalOffset;

    public OffsetItem topic(String topic) {
        setTopic(topic);
        return this;
    }

    public OffsetItem app(String app) {
        setApp(app);
        return this;
    }

    public OffsetItem queueId(short queueId) {
        setQueueId(queueId);
        return this;
    }

    public OffsetItem consumerId(String consumerId) {
        setConsumerId(consumerId);
        return this;
    }

    public OffsetItem queueOffset(long queueOffset) {
        setQueueOffset(queueOffset);
        return this;
    }

    public OffsetItem journalOffset(long journalOffset) {
        setJournalOffset(journalOffset);
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public short getQueueId() {
        return queueId;
    }

    public void setQueueId(short queueId) {
        this.queueId = queueId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public long getJournalOffset() {
        return journalOffset;
    }

    public void setJournalOffset(long journalOffset) {
        this.journalOffset = journalOffset;
    }
}
