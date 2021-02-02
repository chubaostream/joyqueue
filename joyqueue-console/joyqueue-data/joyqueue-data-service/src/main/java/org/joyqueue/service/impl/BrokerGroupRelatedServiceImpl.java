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
package org.joyqueue.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.joyqueue.model.domain.BrokerGroupRelated;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.query.QBrokerGroupRelated;
import org.joyqueue.repository.BrokerGroupRelatedRepository;
import org.joyqueue.service.BrokerGroupRelatedService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分组服务实现
 * Created by chenyanying3 on 2018-10-18
 */
@Service("brokerGroupRelatedService")
public class BrokerGroupRelatedServiceImpl extends PageServiceSupport<BrokerGroupRelated, QBrokerGroupRelated, BrokerGroupRelatedRepository> implements BrokerGroupRelatedService {

    @Override
    public int updateGroupByGroupId(BrokerGroupRelated brokerGroupRelated) {
        return repository.updateGroupByGroupId(brokerGroupRelated);
    }

    @Override
    public int deleteByGroupId(long groupId) {
        return repository.deleteByGroupId(groupId);
    }

    @Override
    public Map<Long, Identity> findGroupByBrokerIds(List<Long> brokerIds) {
        List<BrokerGroupRelated> brokerGroupRelateds = repository.findByBrokerIds(brokerIds);
        if (CollectionUtils.isNotEmpty(brokerGroupRelateds)) {
            return brokerGroupRelateds.stream()
                    .filter(brokerGroupRelated -> brokerGroupRelated.getGroup() != null)
                    .collect(Collectors.toMap(BrokerGroupRelated::getId,
                            BrokerGroupRelated::getGroup));
        }
        return Collections.emptyMap();
    }
}
