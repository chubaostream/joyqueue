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
package org.joyqueue.broker.joyqueue0.network.codec;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.GetMessage;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.domain.TopicName;
import org.joyqueue.network.session.ConsumerId;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;

/**
 * getMessageCodec
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class GetMessageCodec implements Joyqueue0PayloadCodec, Type {

    @Override
    public Object decode(Header header, ByteBuf buffer) throws Exception {
        GetMessage payload = new GetMessage();
        // 1字节消费者ID长度
        payload.setConsumerId(new ConsumerId(Serializer.readString(buffer, 1)));
        // 1字节主题长度
        payload.setTopic(TopicName.parse(Serializer.readString(buffer, 1)));
        // 2字节数量
        payload.setCount(buffer.readShort());
        // 4字节长轮询超时
        payload.setLongPull(buffer.readInt());
        // 队列和偏移量及扩展
        if (header.getVersion() >= PutMessageCodec.VERSION){
            // 2字节队列号
            payload.setQueueId(buffer.readShort());
            // 8字节偏移量
            payload.setOffset(buffer.readLong());
            // 扩展
            payload.setExpandMap(Serializer.readMap(buffer));
        }


        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) {

    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_MESSAGE.getCode();
    }
}