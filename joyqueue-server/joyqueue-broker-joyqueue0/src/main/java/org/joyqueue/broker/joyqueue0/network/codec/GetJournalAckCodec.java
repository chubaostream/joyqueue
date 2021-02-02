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
import org.joyqueue.broker.joyqueue0.command.GetJournalAck;
import org.joyqueue.broker.joyqueue0.network.WrappedByteBuf;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.codec.PayloadEncoder;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.toolkit.buffer.RByteBuffer;

/**
 * 获取日志响应编码器
 */
public class GetJournalAckCodec implements PayloadEncoder<GetJournalAck>, Type {

    @Override
    public void encode(GetJournalAck payload, ByteBuf out) throws Exception {
        out.writeLong(payload.getOffset());
        out.writeLong(payload.getChecksum());
        RByteBuffer buffer = payload.getBuffer();
        if (buffer != null) {
            out.writeInt(0);
        } else {
            out.writeInt(buffer.remaining());
            out.writeBytes(new WrappedByteBuf(buffer));
        }
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_JOURNAL_ACK.getCode();
    }
}