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
 * 生产者
 */
public class Producer extends Joint {
    // 生产者ID
    private String id;
    // 连接ID
    private String connectionId;
    // 生产类型
    private ProducerType type = ProducerType.JOYQUEUE;

    public Producer() {
    }

    public Producer(String id, String topic, String app, ProducerType type) {
        super(topic, app);
        this.connectionId = id;
        this.id = id;
        this.type = type;
    }

    public Producer(String topic, ProducerType type) {
        this.topic = topic;
        this.type = type;
    }

    public Producer(String topic, String app, ProducerType type) {
        this.topic = topic;
        this.app = app;
        this.type = type;
    }

    public Producer(String id, String connectionId, String topic) {
        this.id = id;
        this.connectionId = connectionId;
        this.topic = topic;
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

    public void setType(ProducerType type) {
        this.type = type;
    }

    public ProducerType getType() {
        return type;
    }

    public enum ProducerType {
        JOYQUEUE,
        JMQ2,
        JOYQUEUE0,
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

        Producer producer = (Producer) o;

        if (connectionId != null ? !connectionId.equals(producer.connectionId) : producer.connectionId != null) {
            return false;
        }
        if (id != null ? !id.equals(producer.id) : producer.id != null) {
            return false;
        }
        if (type != null ?type.equals(producer.type) : producer.type != null ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (connectionId != null ? connectionId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Producer{" +
                "id='" + id + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", type=" + type +
                ", topic='" + topic + '\'' +
                ", app='" + app + '\'' +
                '}';
    }
}