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

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;
import org.joyqueue.message.BrokerMessage;

import java.util.Arrays;

/**
 * 重试应答
 *
 * @author Jame.HU
 * @version V1.0
 */
public class GetRetryAck extends Joyqueue0Payload {
    // 存储的消息
    protected BrokerMessage[] messages;

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_RETRY_ACK.getCode();
    }

    public GetRetryAck messages(final BrokerMessage[] messages) {
        this.messages = messages;
        return this;
    }

    public BrokerMessage[] getMessages() {
        return messages;
    }

    public void setMessages(BrokerMessage[] messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetRetryAckPayload{");
        sb.append("messages=").append(Arrays.toString(messages));
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

        GetRetryAck that = (GetRetryAck) o;

        if (!Arrays.equals(messages, that.messages)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (messages != null ? Arrays.hashCode(messages) : 0);
        return result;
    }
}
