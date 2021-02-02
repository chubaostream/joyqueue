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

import com.google.common.base.Preconditions;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;
import org.joyqueue.domain.TopicName;
import org.joyqueue.network.session.ConsumerId;

import java.util.Map;

/**
 * 请求消息
 */
public class GetMessage extends Joyqueue0Payload {
    // 消费者ID
    protected ConsumerId consumerId;
    // 数量
    protected short count;
    // 长轮询时间（毫秒）
    protected int longPull = 0;
    // 主题
    protected TopicName topic;
    // 消费者应答超时时间(服务端配置)
    protected int ackTimeout;
    // 队列号
    protected short queueId = 0;
    // 偏移量
    protected long offset = -1L;
    // 扩展
    protected Map<Object, Object> expandMap;

    public GetMessage count(final short count) {
        setCount(count);
        return this;
    }

    public GetMessage longPull(final int longPull) {
        setLongPull(longPull);
        return this;
    }

    public GetMessage consumerId(final ConsumerId consumerId) {
        setConsumerId(consumerId);
        return this;
    }

    public GetMessage topic(final TopicName topic) {
        setTopic(topic);
        return this;
    }

    public GetMessage ackTimeout(final int ackTimeout) {
        setAckTimeout(ackTimeout);
        return this;
    }

    public GetMessage queueId(final short queueId) {
        setQueueId(queueId);
        return this;
    }

    public GetMessage offset(final long offset) {
        setOffset(offset);
        return this;
    }


    public short getQueueId() {
        return queueId;
    }

    public void setQueueId(short queueId) {
        this.queueId = queueId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public ConsumerId getConsumerId() {
        return this.consumerId;
    }

    public void setConsumerId(ConsumerId consumerId) {
        this.consumerId = consumerId;
    }

    public short getCount() {
        return this.count;
    }

    public void setCount(short count) {
        this.count = count;
    }

    public int getLongPull() {
        return this.longPull;
    }

    public void setLongPull(int longPull) {
        this.longPull = longPull;
    }

    public TopicName getTopic() {
        return topic;
    }

    public void setTopic(TopicName topic) {
        this.topic = topic;
    }

    public int getAckTimeout() {
        return ackTimeout;
    }

    public void setAckTimeout(int ackTimeout) {
        this.ackTimeout = ackTimeout;
    }

    public void setExpandMap(Map<Object, Object> expandMap) {
        this.expandMap = expandMap;
    }

    public Map<Object, Object> getExpandMap() {
        return expandMap;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(consumerId != null, "consumer ID can not be null.");
        Preconditions.checkArgument(count > -1, "count must bigger than -1.");
        Preconditions.checkArgument(topic != null, "topic can not be null.");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_MESSAGE.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetMessage{");
        sb.append("consumerId=").append(consumerId);
        sb.append(", count=").append(count);
        sb.append(", longPull=").append(longPull);
        sb.append(", topic='").append(topic).append('\'');
        sb.append('}');
        return sb.toString();
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

        GetMessage that = (GetMessage) o;

        if (count != that.count) {
            return false;
        }
        if (longPull != that.longPull) {
            return false;
        }
        if (consumerId != null ? !consumerId.equals(that.consumerId) : that.consumerId != null) {
            return false;
        }
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) {
            return false;
        }
        if (queueId != that.queueId) {
            return false;
        }
        if (offset != that.offset) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
        result = 31 * result + (int) count;
        result = 31 * result + longPull;
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        return result;
    }
}