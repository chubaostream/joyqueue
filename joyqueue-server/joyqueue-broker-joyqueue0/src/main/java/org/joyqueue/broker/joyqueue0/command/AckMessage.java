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

import java.util.Arrays;

/**
 * 应答消息
 */
public class AckMessage extends Joyqueue0Payload {
    public static byte FROM_CLIENT=1;
    // 消费者ID
    private ConsumerId consumerId;
    // 消息位置
    private MessageLocation[] locations;
    // 确认来源，默认来源于客户端
    private byte source = FROM_CLIENT;

    @Override
    public int type() {
        return Joyqueue0CommandType.ACK_MESSAGE.getCode();
    }

    public byte getSource() {
        return source;
    }

    public void setSource(byte source) {
        this.source = source;
    }

    public AckMessage consumerId(final ConsumerId consumerId) {
        setConsumerId(consumerId);
        return this;
    }

    public AckMessage locations(final MessageLocation[] locations) {
        setLocations(locations);
        return this;
    }

    public ConsumerId getConsumerId() {
        return this.consumerId;
    }

    public void setConsumerId(ConsumerId consumerId) {
        this.consumerId = consumerId;
    }

    public MessageLocation[] getLocations() {
        return this.locations;
    }

    public void setLocations(MessageLocation[] locations) {
        this.locations = locations;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(consumerId != null, "consumer ID can not be null");
        Preconditions.checkArgument(locations != null && locations.length > 0 , "locations can not be null");


    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AckMessage{");
        sb.append("consumerId=").append(consumerId);
        sb.append(", locations=").append(Arrays.toString(locations));
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

        AckMessage that = (AckMessage) o;

        if (consumerId != null ? !consumerId.equals(that.consumerId) : that.consumerId != null) {
            return false;
        }
        if (!Arrays.equals(locations, that.locations)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = consumerId != null ? consumerId.hashCode() : 0;
        result = 31 * result + (locations != null ? Arrays.hashCode(locations) : 0);
        return result;
    }
}