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

/**
 * 健康检查，要判断读写权限和生产者是否存在
 */
public class GetProducerHealth extends GetHealth {
    // 生产者ID
    private String producerId;

    public GetProducerHealth topic(String topic) {
        setTopic(topic);
        return this;
    }

    public GetProducerHealth app(String app) {
        setApp(app);
        return this;
    }

    public GetProducerHealth producerId(String producerId) {
        setProducerId(producerId);
        return this;
    }

    public GetProducerHealth dataCenter(byte dataCenter) {
        setDataCenter(dataCenter);
        return this;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_PRODUCER_HEALTH.getCode();
    }
}