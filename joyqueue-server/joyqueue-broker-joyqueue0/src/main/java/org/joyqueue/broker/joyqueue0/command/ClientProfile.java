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

import java.util.List;

/**
 * 客户端性能
 */
public class ClientProfile extends Joyqueue0Payload {
    private List<ClientTPStat> clientStats;

    public ClientProfile clientStat(List<ClientTPStat> clientStat) {
        setClientStats(clientStat);
        return this;
    }

    public List<ClientTPStat> getClientStats() {
        return clientStats;
    }

    public void setClientStats(List<ClientTPStat> clientStats) {
        this.clientStats = clientStats;
    }

    @Override
    public void validate() {
        super.validate();
        Preconditions.checkArgument(clientStats != null, "client stat can not be null.");
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.CLIENT_PROFILE.getCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClientProfile{");
        sb.append("clientStats=").append(clientStats);
        sb.append('}');
        return sb.toString();
    }
}
