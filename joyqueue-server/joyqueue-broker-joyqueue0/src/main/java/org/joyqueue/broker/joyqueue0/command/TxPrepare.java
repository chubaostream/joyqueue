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
import org.joyqueue.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * TxPrepare
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class TxPrepare extends Transaction {
    // 队列ID
    private short queueId;
    // 超时
    private int timeout;
    // 消息
    private List<Message> messages = new ArrayList<Message>();

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public short getQueueId() {
        return queueId;
    }

    public void setQueueId(short queueId) {
        this.queueId = queueId;
    }

    @Override
    public void validate() {
        if (transactionId == null) {
            throw new IllegalArgumentException("transactionId must note be null");
        }
        if (timeout <= 0) {
            throw new IllegalStateException("timeout must be greater than 0");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Prepare{");
        sb.append("transactionId=").append(transactionId);
        sb.append(", timeout=").append(timeout);
        sb.append(", messages=").append(messages);
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

        TxPrepare prepare = (TxPrepare) o;

        if (timeout != prepare.timeout) {
            return false;
        }
        if (messages != null ? !messages.equals(prepare.messages) : prepare.messages != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + timeout;
        result = 31 * result + (messages != null ? messages.hashCode() : 0);
        return result;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.PREPARE.getCode();
    }
}
