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
package com.jd.journalq.network.codec;

import com.jd.journalq.network.command.CommandType;
import com.jd.journalq.network.transport.codec.PayloadCodec;
import com.jd.journalq.network.transport.command.Header;
import com.jd.journalq.network.transport.command.Payload;
import com.jd.journalq.network.transport.command.Types;
import io.netty.buffer.ByteBuf;

/**
 * @author wylixiaobin
 * Date: 2018/10/12
 */
public class NullPayloadCodec implements PayloadCodec, Types {
    @Override
    public Object decode(Header header, ByteBuf buffer) throws Exception {
        return null;
    }
    @Override
    public int[] types() {
        return new int[]{CommandType.BOOLEAN_ACK};
    }
    @Override
    public void encode(Payload payload, ByteBuf buffer) throws Exception {

    }
}
