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
import org.joyqueue.broker.joyqueue0.command.Identity;
import org.joyqueue.broker.joyqueue0.command.Joyqueue0Broker;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.toolkit.network.IpUtil;

/**
 * 认证解码器
 */
public class IdentityCodec implements Joyqueue0PayloadCodec, Type {

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        Identity payload = new Identity();
        // 6字节地址
        StringBuilder sb = new StringBuilder();
        IpUtil.toAddress(Serializer.readBytes(in, 6), sb);
        String address = sb.toString();

        Joyqueue0Broker broker = new Joyqueue0Broker(address.replaceAll("[.:]", "_"));
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
        return Joyqueue0CommandType.IDENTITY.getCode();
    }
}