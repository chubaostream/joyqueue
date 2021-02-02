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
package org.joyqueue.nsr.sql.converter;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.joyqueue.domain.Broker;
import org.joyqueue.nsr.sql.domain.BrokerDTO;

import java.util.Collections;
import java.util.List;

/**
 * BrokerConverter
 * author: gaohaoxiang
 * date: 2019/8/16
 */
public class BrokerConverter {

    public static BrokerDTO convert(Broker broker) {
        if (broker == null) {
            return null;
        }
        BrokerDTO brokerDTO = new BrokerDTO();
        brokerDTO.setId(Long.valueOf(broker.getId()));
        brokerDTO.setIp(broker.getIp());
        brokerDTO.setPort(broker.getPort());
        brokerDTO.setDataCenter(broker.getDataCenter());
        brokerDTO.setRetryType(broker.getRetryType());
        brokerDTO.setPermission(broker.getPermission().getName());
        brokerDTO.setExternalIp(broker.getExternalIp());
        brokerDTO.setExternalPort(broker.getExternalPort());
        brokerDTO.setNode(broker.getNode());
        return brokerDTO;
    }

    public static Broker convert(BrokerDTO brokerDTO) {
        if (brokerDTO == null) {
            return null;
        }
        Broker broker = new Broker();
        broker.setId(Integer.valueOf(String.valueOf(brokerDTO.getId())));
        broker.setIp(brokerDTO.getIp());
        broker.setPort(brokerDTO.getPort());
        broker.setDataCenter(brokerDTO.getDataCenter());
        broker.setRetryType(brokerDTO.getRetryType());
        broker.setPermission(Broker.PermissionEnum.value(brokerDTO.getPermission()));
        broker.setExternalIp(brokerDTO.getExternalIp());
        broker.setExternalPort(brokerDTO.getExternalPort());
        broker.setNode(brokerDTO.getNode());
        return broker;
    }

    public static List<Broker> convert(List<BrokerDTO> brokerDTOList) {
        if (CollectionUtils.isEmpty(brokerDTOList)) {
            return Collections.emptyList();
        }
        List<Broker> result = Lists.newArrayListWithCapacity(brokerDTOList.size());
        for (BrokerDTO brokerDTO : brokerDTOList) {
            result.add(convert(brokerDTO));
        }
        return result;
    }
}
