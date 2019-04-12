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
package com.jd.journalq.nsr.network.codec;

import com.jd.journalq.network.serializer.Serializer;
import com.jd.journalq.network.transport.command.Header;
import com.jd.journalq.network.transport.command.Type;
import com.jd.journalq.nsr.network.NsrPayloadCodec;
import com.jd.journalq.nsr.network.command.GetBrokerAck;
import com.jd.journalq.nsr.network.command.NsrCommandType;
import io.netty.buffer.ByteBuf;

/**
 * @author wylixiaobin
 * Date: 2019/1/27
 */
public class GetBrokerAckCodec implements NsrPayloadCodec<GetBrokerAck>, Type {
    @Override
    public GetBrokerAck decode(Header header, ByteBuf buffer) throws Exception {
        GetBrokerAck brokerAck = new GetBrokerAck();
        if(buffer.readBoolean()){
            brokerAck.broker(Serializer.readBroker(buffer));
        }
        return brokerAck;
    }

    @Override
    public void encode(GetBrokerAck payload, ByteBuf buffer) throws Exception {
        if(null==payload.getBroker()){
            buffer.writeBoolean(false);
            return;
        }
        // 1.boolean
        buffer.writeBoolean(true);
        Serializer.write(payload.getBroker(),buffer);
    }

    @Override
    public int type() {
        return NsrCommandType.GET_BROKER_ACK;
    }
}
