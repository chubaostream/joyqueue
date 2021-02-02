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
package org.joyqueue.broker.joyqueue0.command;

import com.google.common.base.Preconditions;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;
import org.joyqueue.domain.TopicName;
import org.joyqueue.network.session.ConsumerId;

/**
 * 添加消费者
 */
public class AddConsumer extends Joyqueue0Payload {
    // 消费者ID
    private ConsumerId consumerId;
    // 主题
    private TopicName topic;
    // 选择器
    private String selector;

    public AddConsumer topic(final TopicName topic) {
        setTopic(topic);
        return this;
    }

    public AddConsumer selector(final String selector) {
        setSelector(selector);
        return this;
    }

    public AddConsumer consumerId(final ConsumerId consumerId) {
        setConsumerId(consumerId);
        return this;
    }

    public ConsumerId getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(ConsumerId consumerId) {
        this.consumerId = consumerId;
    }

    public TopicName getTopic() {
        return topic;
    }

    public void setTopic(TopicName topic) {
        this.topic = topic;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(consumerId != null, "consumer ID can not be null");
        Preconditions.checkArgument(topic != null, "topic can not be null");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.ADD_CONSUMER.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddConsumer{");
        sb.append("consumerId=").append(consumerId);
        sb.append(", topic='").append(topic).append('\'');
        sb.append(", selector='").append(selector).append('\'');
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

        AddConsumer that = (AddConsumer) o;

        if (consumerId != null ? !consumerId.equals(that.consumerId) : that.consumerId != null) {
            return false;
        }
        if (selector != null ? !selector.equals(that.selector) : that.selector != null) {
            return false;
        }
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = consumerId != null ? consumerId.hashCode() : 0;
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (selector != null ? selector.hashCode() : 0);
        return result;
    }
}