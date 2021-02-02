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
package org.joyqueue.nsr.network.command;

import org.joyqueue.network.transport.command.JoyQueuePayload;

/**
 * @author wylixiaobin
 * Date: 2019/1/27
 */
public class Register extends JoyQueuePayload {
    private Integer brokerId;
    private String brokerHost;
    private String brokerIp;
    private Integer port;
    private String node;

    public Register brokerId(Integer brokerId) {
        this.brokerId = brokerId;
        return this;
    }
    public Register brokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
        return this;
    }
    public Register brokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
        return this;
    }
    public Register port(Integer port){
        this.port = port;
        return this;
    }

    public Register node(String node){
        this.node = node;
        return this;
    }

    public Integer getBrokerId() {
        return brokerId;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public Integer getPort() {
        return port;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    @Override
    public int type() {
        return NsrCommandType.REGISTER;
    }

    @Override
    public String toString() {
        return "Register{" +
                "brokerId=" + brokerId +
                ", brokerIp='" + brokerIp + '\'' +
                ", port=" + port +
                '}';
    }
}
