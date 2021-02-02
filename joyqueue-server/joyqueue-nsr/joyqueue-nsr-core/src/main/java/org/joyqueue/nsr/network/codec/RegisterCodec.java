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
package org.joyqueue.nsr.network.codec;

import org.joyqueue.network.serializer.Serializer;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.nsr.network.NsrPayloadCodec;
import org.joyqueue.nsr.network.command.NsrCommandType;
import org.joyqueue.nsr.network.command.Register;
import io.netty.buffer.ByteBuf;

/**
 * @author wylixiaobin
 * Date: 2019/1/27
 */
public class RegisterCodec implements NsrPayloadCodec<Register>, Type {
    @Override
    public Register decode(Header header, ByteBuf buffer) throws Exception {
        int brokerId = buffer.readInt();
        return new Register().brokerId(brokerId>0?brokerId:null)
                .brokerIp(Serializer.readString(buffer))
                .brokerHost(Serializer.readString(buffer))
                .port(buffer.readInt())
                .node(Serializer.readString(buffer));
    }

    @Override
    public void encode(Register payload, ByteBuf buffer) throws Exception {
        buffer.writeInt(null==payload.getBrokerId()?0:payload.getBrokerId());
        Serializer.write(payload.getBrokerIp(),buffer);
        Serializer.write(payload.getBrokerHost(), buffer);
        buffer.writeInt(payload.getPort());
        Serializer.write(payload.getNode(), buffer);
    }

    @Override
    public int type() {
        return NsrCommandType.REGISTER;
    }
}
