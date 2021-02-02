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
import org.joyqueue.network.session.ProducerId;

/**
 * 添加生产者
 */
public class AddProducer extends Joyqueue0Payload {
    // 生产者ID
    private ProducerId producerId;
    // 主题
    private TopicName topic;

    public AddProducer topic(final TopicName topic) {
        setTopic(topic);
        return this;
    }

    public AddProducer producerId(final ProducerId producerId) {
        setProducerId(producerId);
        return this;
    }

    public ProducerId getProducerId() {
        return this.producerId;
    }

    public void setProducerId(ProducerId producerId) {
        this.producerId = producerId;
    }

    public TopicName getTopic() {
        return this.topic;
    }

    public void setTopic(TopicName topic) {
        this.topic = topic;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(producerId != null, "producer ID can not be null");
        Preconditions.checkArgument(topic != null, "topic can not be null");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.ADD_PRODUCER.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddProducer{");
        sb.append("producerId=").append(producerId);
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

        AddProducer that = (AddProducer) o;

        if (producerId != null ? !producerId.equals(that.producerId) : that.producerId != null) {
            return false;
        }
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = producerId != null ? producerId.hashCode() : 0;
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        return result;
    }
}