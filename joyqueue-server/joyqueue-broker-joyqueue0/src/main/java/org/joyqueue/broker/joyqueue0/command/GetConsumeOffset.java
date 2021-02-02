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
package org.joyqueue.broker.joyqueue0.command;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;

/**
 * 合并消费位置
 *
 * @author lindeqiang
 */
public class GetConsumeOffset extends Joyqueue0Payload {
    // offset文件数据内容
    protected String offset;
    protected boolean isSlaveConsume;

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public boolean isSlaveConsume() {
        return isSlaveConsume;
    }

    public void setSlaveConsume(boolean slaveConsume) {
        isSlaveConsume = slaveConsume;
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

        GetConsumeOffset that = (GetConsumeOffset) o;

        if (offset != null ? !offset.equals(that.offset) : that.offset != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (offset != null ? offset.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return offset;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CONSUMER_OFFSET.getCode();
    }
}
