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

/**
 * 获取同步位置应答
 */
public class GetOffsetAck extends Joyqueue0Payload {
    // 偏移量
    private long offset;
    // 最大偏移量
    private long maxOffset;

    public GetOffsetAck() {

    }

    public GetOffsetAck(long offset, long maxOffset) {
        this.offset = offset;
        this.maxOffset = maxOffset;
    }

    public GetOffsetAck maxOffset(final long maxOffset) {
        setMaxOffset(maxOffset);
        return this;
    }

    public GetOffsetAck offset(final long offset) {
        setOffset(offset);
        return this;
    }

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getMaxOffset() {
        return this.maxOffset;
    }

    public void setMaxOffset(long maxOffset) {
        this.maxOffset = maxOffset;
    }

    @Override
    public void validate() {
        Preconditions.checkArgument(offset > 0, "offset must be greater than or equal 0");
        Preconditions.checkArgument(maxOffset > 0, "maxOffset must be greater than or equal 0");
        Preconditions.checkArgument(offset < maxOffset, "maxOffset must be greater than or equal offset");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_OFFSET_ACK.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetOffsetAck{");
        sb.append("offset=").append(offset);
        sb.append(", maxOffset=").append(maxOffset);
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

        GetOffsetAck that = (GetOffsetAck) o;

        if (maxOffset != that.maxOffset) {
            return false;
        }
        if (offset != that.offset) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (int) (maxOffset ^ (maxOffset >>> 32));
        return result;
    }
}