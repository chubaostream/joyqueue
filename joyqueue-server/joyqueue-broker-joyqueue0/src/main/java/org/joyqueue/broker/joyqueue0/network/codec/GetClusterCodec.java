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
package org.joyqueue.broker.joyqueue0.network.codec;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.GetCluster;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Payload;
import org.joyqueue.network.transport.command.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取集群解码器
 */
public class GetClusterCodec implements Joyqueue0PayloadCodec, Type {

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        GetCluster payload = new GetCluster();
        payload.setApp(Serializer.readString(in));
        payload.setClientId(Serializer.readString(in));
        payload.setDataCenter(in.readByte());
        int count = in.readUnsignedShort();
        List<String> topics = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            // 1字节主题长度
            topics.add(Serializer.readString(in));
        }
        payload.setTopics(topics);
        if (header.getVersion() >= 2) {
            payload.setParameters(Serializer.readMap(in));
        }
        return payload;
    }

    @Override
    public void encode(Payload payload, ByteBuf buffer) throws Exception {

    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CLUSTER.getCode();
    }
}