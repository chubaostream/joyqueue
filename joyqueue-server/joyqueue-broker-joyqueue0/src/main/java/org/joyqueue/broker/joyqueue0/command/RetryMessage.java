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
import org.joyqueue.message.MessageLocation;
import org.joyqueue.network.session.ConsumerId;

import java.util.List;

/**
 * 客户端重试消息
 */
public class RetryMessage extends Joyqueue0Payload {
    public static final short RETRY_PARTITION_ID = 255;
    // 消费者ID
    private ConsumerId consumerId;
    // 重试原因
    private String exception;
    // 位置
    private List<MessageLocation> locations;

    public ConsumerId getConsumerId() {
        return this.consumerId;
    }

    public void setConsumerId(ConsumerId consumerId) {
        this.consumerId = consumerId;
    }

    public List<MessageLocation> getLocations() {
        return this.locations;
    }

    public void setLocations(List<MessageLocation> locations) {
        this.locations = locations;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }


    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(consumerId != null, "consumer ID can not be null.");
        Preconditions.checkArgument(locations != null && locations.size() > 0, "locations can not be empty.");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.RETRY_MESSAGE.getCode();
    }

    @Override
    public String toString() {
        return "RetryMessage{" +
                "consumerId=" + consumerId +
                ", exception='" + exception + '\'' +
                ", locations=" + locations +
                '}';
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

        RetryMessage that = (RetryMessage) o;

        if (consumerId != null ? !consumerId.equals(that.consumerId) : that.consumerId != null) {
            return false;
        }
        if (exception != null ? !exception.equals(that.exception) : that.exception != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }
}