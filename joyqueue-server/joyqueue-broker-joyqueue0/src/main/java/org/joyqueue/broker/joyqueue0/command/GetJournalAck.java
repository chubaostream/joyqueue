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
package org.joyqueue.broker.joyqueue0.command;

import com.google.common.base.Preconditions;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;
import org.joyqueue.network.transport.command.Releasable;
import org.joyqueue.toolkit.buffer.RByteBuffer;

/**
 * 复制，请求数据应答
 */
public class GetJournalAck extends Joyqueue0Payload implements Releasable {
    // 偏移量
    private long offset;
    private boolean insync;
    // 数据
    private RByteBuffer buffer;

    private long checksum;

    public GetJournalAck offset(final long offset) {
        setOffset(offset);
        return this;
    }

    public GetJournalAck buffer(final RByteBuffer buffer) {
        setBuffer(buffer);
        return this;
    }

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public boolean getInsync() {
        return insync;
    }

    public void setInsync(boolean insync) {
        this.insync = insync;
    }

    public RByteBuffer getBuffer() {
        return this.buffer;
    }

    public void setBuffer(RByteBuffer buffer) {
        this.buffer = buffer;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    @Override
    public void release() {
        if (buffer != null) {
            buffer.release();
        }
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(offset > 0, "offset must be greater than or equal 0");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_JOURNAL_ACK.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetJournalAck{");
        sb.append("offset=").append(offset);
        sb.append(",inSync=").append(insync);
        if (buffer != null) {
            sb.append(", size=").append(buffer.remaining());
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        GetJournalAck that = (GetJournalAck) o;

        if (offset != that.offset) {
            return false;
        }
        if (insync != that.insync) {
            return false;
        }
        if (buffer != null ? !buffer.equals(that.buffer) : that.buffer != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (buffer != null ? buffer.hashCode() : 0);
        return result;
    }
}