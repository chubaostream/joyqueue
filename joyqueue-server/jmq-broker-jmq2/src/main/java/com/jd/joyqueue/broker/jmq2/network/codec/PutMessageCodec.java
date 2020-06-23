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
package com.jd.joyqueue.broker.jmq2.network.codec;

import com.jd.joyqueue.broker.jmq2.JMQ2CommandType;
import com.jd.joyqueue.broker.jmq2.command.PutMessage;
import com.jd.joyqueue.broker.jmq2.network.JMQ2PayloadCodec;
import com.jd.joyqueue.broker.jmq2.util.Serializer;
import org.joyqueue.network.session.ProducerId;
import org.joyqueue.network.session.TransactionId;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;
import io.netty.buffer.ByteBuf;

/**
 * 发送消息解码器
 */
public class PutMessageCodec implements JMQ2PayloadCodec, Type {

    public static final byte VERSION = 2;

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        PutMessage payload = new PutMessage();
        payload.setProducerId(new ProducerId(Serializer.readString(in)));
        String id = Serializer.readString(in);
        if (id != null && !id.isEmpty()) {
            payload.setTransactionId(new TransactionId(id));
        }

        // 需要解码为BrokerMessage类型
        payload.setMessages(Serializer.readMessages(in));
        // 队列ID
        if (header.getVersion() >= VERSION) {
            payload.setQueueId(in.readShort());
        }
        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) throws Exception {

    }

    @Override
    public int type() {
        return JMQ2CommandType.PUT_MESSAGE.getCode();
    }
}