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
import org.joyqueue.broker.joyqueue0.command.AddConnection;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.session.ConnectionId;
import org.joyqueue.network.session.Language;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;

/**
 * 添加连接编解码器
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class AddConnectionCodec implements Joyqueue0PayloadCodec, Type {

    @Override
    public Object decode(Header header, ByteBuf buffer) throws Exception {
        AddConnection payload = new AddConnection();
        // 1字节链接ID长度
        payload.setConnectionId(new ConnectionId(Serializer.readString(buffer, 1)));
        // 1字节用户长度
        payload.setUser(Serializer.readString(buffer, 1));
        // 1字节密码长度
        payload.setPassword(Serializer.readString(buffer, 1));
        // 1字节应用长度
        payload.setApp(Serializer.readString(buffer, 1));
        // 1字节语言
        payload.setLanguage(Language.valueOf(buffer.readUnsignedByte()));
        // 1字节版本长度
        payload.setClientVersion(Serializer.readString(buffer, 1));
        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) {

    }

    @Override
    public int type() {
        return Joyqueue0CommandType.ADD_CONNECTION.getCode();
    }
}