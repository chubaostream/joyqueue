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
import org.joyqueue.broker.joyqueue0.command.ClientProfile;
import org.joyqueue.broker.joyqueue0.command.ClientTPStat;
import org.joyqueue.broker.joyqueue0.command.ClientTpOriginals;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端性能解码器
 */
public class ClientProfileCodec implements Joyqueue0PayloadCodec, Type {

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        ClientProfile payload = new ClientProfile();
        List<ClientTPStat> clientStats = new ArrayList<ClientTPStat>();
        int size = in.readInt();
        if (size > 0) {
            String app = Serializer.readString(in, 1, false);
            ClientTPStat clientStat;
            // 读取基本属性
            for (int i = 0; i < size; i++) {
                clientStat = new ClientTPStat();
                clientStat.setTopic(Serializer.readString(in, 1, false));
                clientStat.setApp(app);
                clientStat.setStartTime(in.readLong());
                clientStat.setEndTime(in.readLong());
                clientStat.setProduce(new ClientTpOriginals(in.readLong(), 0, in.readLong(), in.readLong(), in.readLong()));
                clientStat.setReceive(new ClientTpOriginals(in.readLong(), 0, 0, in.readLong(), in.readLong()));
                clientStat.setConsume(new ClientTpOriginals(in.readLong(), 0, in.readLong(), 0, in.readLong()));

                if (header.getVersion() >= 3) {
                    clientStat.setRetry(new ClientTpOriginals(in.readLong(), 0, in.readLong(), 0, in.readLong()));
                }

                clientStats.add(clientStat);
            }

            payload.setClientStats(clientStats);

            // 读取TP属性
            for (int i = 0; i < clientStats.size(); i++) {
                clientStat = clientStats.get(i);
                readStat(in, clientStat.getProduce());
                readStat(in, clientStat.getReceive());
                readStat(in, clientStat.getConsume());
                readStat(in, clientStat.getRetry());
            }
        }
        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) {

    }

    /**
     * 读取TP数据
     *
     * @param in   输入
     * @param stat 性能统计
     */
    protected void readStat(final ByteBuf in, final ClientTpOriginals stat) {
        if (stat == null) {
            return;
        }
        stat.setSuccess(in.readLong());
        stat.setTp999(in.readInt());
        stat.setTp99(in.readInt());
        stat.setTp90(in.readInt());
        stat.setTp50(in.readInt());
        stat.setMax(in.readInt());
        stat.setMin(in.readInt());
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.CLIENT_PROFILE.getCode();
    }
}