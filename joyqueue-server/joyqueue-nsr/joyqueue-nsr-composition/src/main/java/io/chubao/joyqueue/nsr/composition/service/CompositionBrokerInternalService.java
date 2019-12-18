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
package io.chubao.joyqueue.nsr.composition.service;

import io.chubao.joyqueue.domain.Broker;
import io.chubao.joyqueue.model.PageResult;
import io.chubao.joyqueue.model.QPageQuery;
import io.chubao.joyqueue.nsr.composition.config.CompositionConfig;
import io.chubao.joyqueue.nsr.model.BrokerQuery;
import io.chubao.joyqueue.nsr.service.internal.BrokerInternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CompositionBrokerInternalService
 * author: gaohaoxiang
 * date: 2019/8/12
 */
public class CompositionBrokerInternalService implements BrokerInternalService {

    protected static final Logger logger = LoggerFactory.getLogger(CompositionBrokerInternalService.class);

    private CompositionConfig config;
    private BrokerInternalService igniteBrokerService;
    private BrokerInternalService journalkeeperBrokerService;

    public CompositionBrokerInternalService(CompositionConfig config, BrokerInternalService igniteBrokerService,
                                            BrokerInternalService journalkeeperBrokerService) {
        this.config = config;
        this.igniteBrokerService = igniteBrokerService;
        this.journalkeeperBrokerService = journalkeeperBrokerService;
    }

    @Override
    public Broker getByIpAndPort(String brokerIp, Integer brokerPort) {
        if (config.isReadIgnite()) {
            return igniteBrokerService.getByIpAndPort(brokerIp, brokerPort);
        } else {
            try {
                return journalkeeperBrokerService.getByIpAndPort(brokerIp, brokerPort);
            } catch (Exception e) {
                logger.error("getByIpAndPort exception, brokerIp: {}, brokerPort: {}", brokerIp, brokerPort, e);
                return igniteBrokerService.getByIpAndPort(brokerIp, brokerPort);
            }
        }
    }

    @Override
    public List<Broker> getByRetryType(String retryType) {
        if (config.isReadIgnite()) {
            return igniteBrokerService.getByRetryType(retryType);
        } else {
            try {
                return journalkeeperBrokerService.getByRetryType(retryType);
            } catch (Exception e) {
                logger.error("getByRetryType exception, retryType: {}", retryType, e);
                return igniteBrokerService.getByRetryType(retryType);
            }
        }
    }

    @Override
    public List<Broker> getByIds(List<Integer> ids) {
        if (config.isReadIgnite()) {
            return igniteBrokerService.getByIds(ids);
        } else {
            try {
                return journalkeeperBrokerService.getByIds(ids);
            } catch (Exception e) {
                logger.error("getByIds exception, ids: {}", ids, e);
                return igniteBrokerService.getByIds(ids);
            }
        }
    }

    @Override
    public Broker update(Broker broker) {
        Broker result = null;
        if (config.isWriteIgnite()) {
            result = igniteBrokerService.update(broker);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperBrokerService.update(broker);
            } catch (Exception e) {
                logger.error("update journalkeeper exception, params: {}", broker, e);
            }
        }
        return result;
    }

    @Override
    public Broker getById(int id) {
        if (config.isReadIgnite()) {
            return igniteBrokerService.getById(id);
        } else {
            try {
                return journalkeeperBrokerService.getById(id);
            } catch (Exception e) {
                logger.error("getById exception, id", id, e);
                return igniteBrokerService.getById(id);
            }
        }
    }

    @Override
    public Broker add(Broker broker) {
        Broker result = null;
        if (config.isWriteIgnite()) {
            result = igniteBrokerService.add(broker);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperBrokerService.add(broker);
            } catch (Exception e) {
                logger.error("addOrUpdate journalkeeper exception, params: {}", broker, e);
            }
        }
        return result;
    }

    @Override
    public void delete(int id) {
        if (config.isWriteIgnite()) {
            igniteBrokerService.delete(id);
        }
        if (config.isWriteJournalkeeper()) {
            try {
                journalkeeperBrokerService.delete(id);
            } catch (Exception e) {
                logger.error("deleteById journalkeeper exception, params: {}", id, e);
            }
        }
    }

    @Override
    public List<Broker> getAll() {
        if (config.isReadIgnite()) {
            return igniteBrokerService.getAll();
        } else {
            try {
                return journalkeeperBrokerService.getAll();
            } catch (Exception e) {
                logger.error("getAll exception", e);
                return igniteBrokerService.getAll();
            }
        }
    }

    @Override
    public PageResult<Broker> search(QPageQuery<BrokerQuery> pageQuery) {
        if (config.isReadIgnite()) {
            return igniteBrokerService.search(pageQuery);
        } else {
            try {
                return journalkeeperBrokerService.search(pageQuery);
            } catch (Exception e) {
                logger.error("search exception, pageQuery: {}", pageQuery, e);
                return igniteBrokerService.search(pageQuery);
            }
        }
    }
}
