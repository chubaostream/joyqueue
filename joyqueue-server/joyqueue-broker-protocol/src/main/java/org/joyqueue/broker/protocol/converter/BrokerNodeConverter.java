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
package org.joyqueue.broker.protocol.converter;

import org.joyqueue.domain.Broker;
import org.joyqueue.domain.DataCenter;
import org.joyqueue.network.domain.BrokerNode;
import org.apache.commons.lang3.StringUtils;

/**
 * BrokerNodeConverter
 *
 * author: gaohaoxiang
 * date: 2018/12/3
 */
public class BrokerNodeConverter {

    public static BrokerNode convertBrokerNode(Broker broker) {
        return convertBrokerNode(broker, null, null);
    }

    public static BrokerNode convertBrokerNode(Broker broker, DataCenter brokerDataCenter, String region) {
        return convertBrokerNode(broker, brokerDataCenter, region, 0);
    }

    public static BrokerNode convertBrokerNode(Broker broker, DataCenter brokerDataCenter, String region, int weight) {
        BrokerNode result = new BrokerNode();
        result.setId(broker.getId());
        // If externalIp is blank, set host as ip value
        result.setHost(broker.getRealIp());
        result.setPort(broker.getRealPort());
        result.setDataCenter(brokerDataCenter == null ? null : brokerDataCenter.getRegion());

        if (StringUtils.isBlank(region) || brokerDataCenter == null) {
            result.setNearby(true);
        } else {
            result.setNearby(StringUtils.equalsIgnoreCase(brokerDataCenter.getRegion(), region));
        }
        result.setWeight(weight);

        if (Broker.PermissionEnum.FULL.equals(broker.getPermission())) {
            result.setReadable(true);
            result.setWritable(true);
        } else if (Broker.PermissionEnum.READ.equals(broker.getPermission())) {
            result.setReadable(true);
        } else if (Broker.PermissionEnum.WRITE.equals(broker.getPermission())) {
            result.setWritable(true);
        }

        return result;
    }
}