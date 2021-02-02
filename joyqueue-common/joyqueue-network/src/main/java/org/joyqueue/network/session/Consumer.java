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
package org.joyqueue.network.session;

/**
 * 消费者
 */
public class Consumer extends Joint {
    // ID
    private String id;
    // 连接ID
    private String connectionId;
    // 选择器
    private String selector;
    // 连接点
    private Joint joint;
    //最近一次拉取消息的时间
    private volatile long lastGetMessageTime;
    //重新平衡锁定队列的大小
    private volatile long rebalanceTime;
    //最大锁定队列数
    private volatile int maxLockQueues = 1;
    //消费者类型
    private ConsumeType type = ConsumeType.JOYQUEUE;

    public Consumer() {
    }

    public Consumer(String id, String topic, String app) {
        super(topic, app);
        this.id = id;
    }

    public Consumer(String id, String topic, String app, ConsumeType type) {
        this(id, topic, app);
        this.connectionId = id;
        this.type = type;
    }

    public Consumer(String topic, String app) {
        super(topic, app);
    }

    public Consumer(String id, String connectionId, String topic, String selector) {
        this.id = id;
        this.connectionId = connectionId;
        this.topic = topic;
        this.selector = selector;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getSelector() {
        return this.selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public long getLastGetMessageTime() {
        return lastGetMessageTime;
    }

    public void setLastGetMessageTime(long lastGetMessageTime) {
        this.lastGetMessageTime = lastGetMessageTime;
    }

    public long getRebalanceTime() {
        return rebalanceTime;
    }

    public void setRebalanceTime(long rebalanceTime) {
        this.rebalanceTime = rebalanceTime;
    }

    public int getMaxLockQueues() {
        return maxLockQueues;
    }

    public void setMaxLockQueues(int maxLockQueues) {
        this.maxLockQueues = maxLockQueues;
    }

    public ConsumeType getType() {
        return type;
    }

    public void setType(ConsumeType type) {
        this.type = type;
    }

    public Joint getJoint() {
        if (joint == null) {
            joint = new Joint(topic, app);
        }
        return joint;
    }

    public enum ConsumeType {
        JOYQUEUE,
        JOYQUEUE0,
        JMQ2,
        KAFKA,
        MQTT,
        INTERNAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Consumer consumer = (Consumer) o;

        if (connectionId != null ? !connectionId.equals(consumer.connectionId) : consumer.connectionId != null) {
            return false;
        }
        if (id != null ? !id.equals(consumer.id) : consumer.id != null) {
            return false;
        }
        if (selector != null ? !selector.equals(consumer.selector) : consumer.selector != null) {
            return false;
        }
        if (type != null ? !type.equals(consumer.type) : consumer.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (connectionId != null ? connectionId.hashCode() : 0);
        result = 31 * result + (selector != null ? selector.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Consumer{" +
                "id='" + id + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", selector='" + selector + '\'' +
                ", joint=" + joint +
                ", lastGetMessageTime=" + lastGetMessageTime +
                ", rebalanceTime=" + rebalanceTime +
                ", maxLockQueues=" + maxLockQueues +
                ", type=" + type +
                ", topic='" + topic + '\'' +
                ", app='" + app + '\'' +
                '}';
    }
}