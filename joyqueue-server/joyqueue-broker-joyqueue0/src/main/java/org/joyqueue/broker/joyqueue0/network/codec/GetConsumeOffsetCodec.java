package org.joyqueue.broker.joyqueue0.network.codec;

import com.google.common.base.Charsets;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.GetConsumeOffset;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.codec.PayloadDecoder;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.toolkit.io.Compressors;
import org.joyqueue.toolkit.io.Zlib;

/**
 * 获取消费位置解码器
 */
public class GetConsumeOffsetCodec implements PayloadDecoder, Type {

    @Override
    public Object decode(Header header, final ByteBuf in) throws Exception {
        GetConsumeOffset payload = new GetConsumeOffset();
        // 4字节长度
        int len = in.readInt();
        if (len > 0) {
            byte[] data = new byte[len];
            in.readBytes(data);
            data = Compressors.decompress(data, 0, data.length, Zlib.INSTANCE);
            payload.setOffset(new String(data, Charsets.UTF_8));
        }
        if (in.isReadable()) {
            payload.setSlaveConsume(in.readBoolean());
        }

        return payload;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CONSUMER_OFFSET.getCode();
    }
}