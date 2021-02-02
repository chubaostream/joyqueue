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
import org.joyqueue.message.BrokerMessage;

import java.util.List;

/**
 * 创建重试数据
 */
public class PutRetry extends Joyqueue0Payload {
    // 主题
    private String topic;
    // 应用
    private String app;
    // 重试原因
    private String exception;
    // 消息
    private List<BrokerMessage> messages;

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getApp() {
        return this.app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public List<BrokerMessage> getMessages() {
        return this.messages;
    }

    public void setMessages(List<BrokerMessage> messages) {
        this.messages = messages;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }


    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(topic != null && !topic.isEmpty(), "topic can not be empty");
        Preconditions.checkArgument(app != null && !app.isEmpty(), "app can not be empty");
        Preconditions.checkArgument(messages != null && messages.size() > 0, "message can not be empty");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.PUT_RETRY.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PutRetryPayload{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", app='").append(app).append('\'');
        sb.append(", exception='").append(exception).append('\'');
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

        PutRetry putRetry = (PutRetry) o;

        if (app != null ? !app.equals(putRetry.app) : putRetry.app != null) {
            return false;
        }
        if (exception != null ? !exception.equals(putRetry.exception) : putRetry.exception != null) {
            return false;
        }
        if (topic != null ? !topic.equals(putRetry.topic) : putRetry.topic != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }
}