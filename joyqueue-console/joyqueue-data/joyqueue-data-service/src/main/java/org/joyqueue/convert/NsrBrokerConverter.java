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
package org.joyqueue.convert;


import org.joyqueue.model.domain.Broker;
import org.joyqueue.model.domain.Identity;

/**
 * Created by wangxiaofei1 on 2019/1/2.
 */
public class NsrBrokerConverter extends Converter<Broker, org.joyqueue.domain.Broker> {
    @Override
    protected org.joyqueue.domain.Broker forward(Broker broker) {
        org.joyqueue.domain.Broker nsrBroker = new org.joyqueue.domain.Broker();
        nsrBroker.setId(Long.valueOf(String.valueOf(broker.getId())).intValue());
        if (broker.getIp() != null) {
            nsrBroker.setIp(broker.getIp());
        }
        nsrBroker.setPort(broker.getPort());
        if (broker.getRetryType() != null) {
            nsrBroker.setRetryType(broker.getRetryType());
        }
        if(broker.getDataCenter() != null) {
            nsrBroker.setDataCenter(broker.getDataCenter().getCode());
        }
        if (broker.getPermission() != null) {
            nsrBroker.setPermission(org.joyqueue.domain.Broker.PermissionEnum.value(broker.getPermission()));
        }
        nsrBroker.setExternalIp(broker.getExternalIp());
        nsrBroker.setExternalPort(broker.getExternalPort());
        nsrBroker.setNode(broker.getNode());
        return nsrBroker;
    }

    @Override
    protected Broker backward(org.joyqueue.domain.Broker nsrBroker) {
        Broker broker = new Broker();
        broker.setId(nsrBroker.getId());
        broker.setIp(nsrBroker.getIp());
        broker.setPort(nsrBroker.getPort());
        broker.setPermission(nsrBroker.getPermission().getName());
        broker.setExternalIp(nsrBroker.getExternalIp());
        broker.setExternalPort(nsrBroker.getExternalPort());
        broker.setNode(nsrBroker.getNode());
        broker.setRetryType(nsrBroker.getRetryType());
        if (broker.getDataCenter() != null) {
            broker.setDataCenter(new Identity(nsrBroker.getDataCenter()));
        }
        return broker;
    }
}
