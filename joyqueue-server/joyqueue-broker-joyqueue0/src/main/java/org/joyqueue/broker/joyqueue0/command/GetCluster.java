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

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取集群
 */
public class GetCluster extends Joyqueue0Payload {
    // 应用
    private String app;
    // 客户端ID
    private String clientId;
    // 客户端所在数据中心
    private byte dataCenter;
    // 主题列表
    private List<String> topics;
    // 参数列表
    private Map<Object, Object> parameters = new HashMap<Object, Object>() {
    };


    public GetCluster() {
    }

    public GetCluster app(final String app) {
        setApp(app);
        return this;
    }

    public GetCluster clientId(final String clientId) {
        setClientId(clientId);
        return this;
    }

    public GetCluster dataCenter(final byte dataCenter) {
        setDataCenter(dataCenter);
        return this;
    }

    public GetCluster topics(final List<String> topics) {
        setTopics(topics);
        return this;
    }

    public GetCluster topics(final String... topics) {
        if (topics != null) {
            setTopics(Arrays.asList(topics));
        }
        return this;
    }

    public String getApp() {
        return this.app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getTopics() {
        return this.topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public byte getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(byte dataCenter) {
        this.dataCenter = dataCenter;
    }

    public Map<Object, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<Object, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetCluster{");
        sb.append("app='").append(app).append('\'');
        sb.append(", dataCenter=").append(dataCenter);
        sb.append(", topics=").append(topics);
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

        GetCluster that = (GetCluster) o;

        if (dataCenter != that.dataCenter) {
            return false;
        }
        if (app != null ? !app.equals(that.app) : that.app != null) {
            return false;
        }
        if (topics != null ? !topics.equals(that.topics) : that.topics != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + (topics != null ? topics.hashCode() : 0);
        result = 31 * result + (int) dataCenter;
        return result;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CLUSTER.getCode();
    }
}