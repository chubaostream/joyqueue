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
 * 获取复制偏移量
 */
public class GetOffset extends Joyqueue0Payload {
    // 起始偏移量
    private long offset;
    // 优化
    private boolean optimized;

    public GetOffset optimized(final boolean optimized) {
        setOptimized(optimized);
        return this;
    }

    public GetOffset offset(final long offset) {
        setOffset(offset);
        return this;
    }

    public long getOffset() {
        return this.offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public boolean isOptimized() {
        return this.optimized;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(offset > 0, "offset must be greater than or equal 0");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_OFFSET.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetOffset{");
        sb.append("Offset=").append(offset);
        sb.append(", optimized=").append(optimized);
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

        GetOffset getOffset = (GetOffset) o;

        if (offset != getOffset.offset) {
            return false;
        }
        if (optimized != getOffset.optimized) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (optimized ? 1 : 0);
        return result;
    }
}