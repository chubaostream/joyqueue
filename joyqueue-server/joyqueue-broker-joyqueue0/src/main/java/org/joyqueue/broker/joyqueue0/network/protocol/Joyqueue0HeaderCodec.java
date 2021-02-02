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
package org.joyqueue.broker.joyqueue0.network.protocol;

import org.joyqueue.broker.joyqueue0.converter.QosConverter;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Header;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.domain.QosLevel;
import org.joyqueue.network.transport.codec.Codec;
import org.joyqueue.network.transport.command.Direction;
import org.joyqueue.network.transport.exception.TransportException;

/**
 * jmq协议头编解码器
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class Joyqueue0HeaderCodec implements Codec {

    // VERSION + FLAG + REQUESTID + COMMANDTYPE + SENDTIME + STATUS + ERRORMSG
    //public static final int HEADER_LENGTH = 1 + 1 + 4 + 1 + 8 + 1 + 2;
    public static final int HEADER_LENGTH = 1 + 1 + 4 + 1 + 8;

    @Override
    public Joyqueue0Header decode(ByteBuf buffer) throws TransportException.CodecException {
        if (!buffer.isReadable(HEADER_LENGTH)) {
            return null;
        }

        byte version = buffer.readByte();
        byte identity = buffer.readByte();
        int requestId = buffer.readInt();
        int type = buffer.readUnsignedByte();
        long sendTime = buffer.readLong();
        short status = 0;
        String error = null;
        Direction direction = Direction.valueOf(identity & 0x1);
        // 将jmq2qos 转换成jmq4qos
        QosLevel qosLevel = QosConverter.toQosLevel((identity >> 1) & 0x3);

        if (direction.equals(Direction.RESPONSE)) {
            // 1个字节的状态码
            status = buffer.readUnsignedByte();
            // 2个字节的异常长度
            // 异常信息
            try {
                error = Serializer.readString(buffer, 2);
            } catch (Exception e) {
                throw new TransportException.CodecException(e.getMessage());
            }
        }

        return new Joyqueue0Header(version, qosLevel, direction, requestId, type, sendTime, status, error);
    }


    @Override
    public void encode(Object payload, ByteBuf buffer) throws TransportException.CodecException {
        Joyqueue0Header header = (Joyqueue0Header) payload;
        // 响应类型
        // 将jmq2qos 转换成jmq4qos
        byte identity = (byte) ((header.getDirection().ordinal() & 0x1) | ((QosConverter.toAcknowledge(header.getQosLevel().ordinal())).value() << 1 & 0x6));
        buffer.writeByte(header.getVersion());
        buffer.writeByte(identity);
        buffer.writeInt(header.getRequestId());
        buffer.writeByte(header.getType());
        buffer.writeLong(header.getTime());
        buffer.writeByte(header.getStatus());
        try {
            Serializer.write(header.getError(), buffer, 2);
        } catch (Exception e) {
            throw new TransportException.CodecException(e.getMessage());
        }
    }
}