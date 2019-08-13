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
package io.chubao.joyqueue.network.codec;

import io.chubao.joyqueue.domain.TopicConfig;
import io.chubao.joyqueue.network.command.CommandType;
import io.chubao.joyqueue.network.command.SubscribeAck;
import io.chubao.joyqueue.network.serializer.Serializer;
import io.chubao.joyqueue.network.transport.codec.PayloadCodec;
import io.chubao.joyqueue.network.transport.command.Header;
import io.chubao.joyqueue.network.transport.command.Type;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wylixiaobin
 * Date: 2018/10/16
 */
public class SubscribeAckCodec implements PayloadCodec<Header, SubscribeAck>, Type {
    @Override
    public Object decode(Header header, ByteBuf buffer) throws Exception {
        short configSize = buffer.readShort();
        List<TopicConfig> configs = new ArrayList();
        if (configSize > 0) {
            for (int i = 0; i < configSize; i++) {
                configs.add(Serializer.readTopicConfig(buffer, header.getVersion()));
            }
        }
        return new SubscribeAck().topicConfigs(configs);
    }

    @Override
    public int type() {
        return CommandType.SUBSCRIBE_ACK;
    }

    @Override
    public void encode(SubscribeAck payload, ByteBuf buffer) throws Exception {
        List<TopicConfig> topicConfigs = payload.getTopicConfigs();
        int configSize = topicConfigs == null ? 0 : topicConfigs.size();
        buffer.writeShort(configSize);
        if (configSize > 0) {
            for (TopicConfig config : topicConfigs) {
                Serializer.write(config, buffer);
            }
        }
    }
}