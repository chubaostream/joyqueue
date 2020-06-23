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
import com.jd.joyqueue.broker.jmq2.command.Identity;
import com.jd.joyqueue.broker.jmq2.command.JMQ2Broker;
import com.jd.joyqueue.broker.jmq2.network.JMQ2PayloadCodec;
import com.jd.joyqueue.broker.jmq2.util.Serializer;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.toolkit.network.IpUtil;
import io.netty.buffer.ByteBuf;

/**
 * 认证解码器
 */
public class IdentityCodec implements JMQ2PayloadCodec, Type {

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        Identity payload = new Identity();
        // 6字节地址
        StringBuilder sb = new StringBuilder();
        IpUtil.toAddress(Serializer.readBytes(in, 6), sb);
        String address = sb.toString();

        JMQ2Broker broker = new JMQ2Broker(address.replaceAll("[.:]", "_"));
        broker.setAlias(Serializer.readString(in));
        broker.setDataCenter(in.readByte());
        payload.setBroker(broker);
        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) {

    }

    @Override
    public int type() {
        return JMQ2CommandType.IDENTITY.getCode();
    }
}