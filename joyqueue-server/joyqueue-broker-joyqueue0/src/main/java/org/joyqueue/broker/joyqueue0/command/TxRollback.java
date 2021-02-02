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

/**
 * TxRollback
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class TxRollback extends Transaction {

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.ROLLBACK.getCode();
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(null != transactionId, "transactionId could not be null.");
        Preconditions.checkArgument(null != topic && !topic.isEmpty(), "topic could not be null or empty.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TxRollback that = (TxRollback) o;

        if (this.topic != null ? !this.topic.equals(that.getTopic()) : that.getTopic() == null) return false;
        if (this.transactionId != null ? !this.transactionId.equals(that.getTransactionId()) : that.getTransactionId() == null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + topic.hashCode();
        result = 31 * result + transactionId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TxRollback{" +
                "topic='" + topic + '\'' +
                "transactionId='" + transactionId.getTxId() + '\'' +
                '}';
    }
}
