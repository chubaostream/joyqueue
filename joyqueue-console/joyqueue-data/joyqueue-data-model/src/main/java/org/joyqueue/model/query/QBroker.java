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
package org.joyqueue.model.query;

import org.joyqueue.model.Query;
import org.joyqueue.model.domain.Identity;

import java.util.List;

/**
 * Created by lining on 16-11-28.
 */
public class QBroker implements Query {

    private String retryType;
    private String ip;

    private int brokerId;

    private Identity group;

    @Deprecated
    private long brokerGroupId;

    private String keyword;

    @Deprecated
    private List<Integer> brokerGroupIds;

    private List<Integer> notInBrokerIds;

    private List<Integer> inBrokerIds;

    private String externalIp;

    public String getRetryType() {
        return retryType;
    }

    public void setRetryType(String retryType) {
        this.retryType = retryType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public long getBrokerGroupId() {
        return brokerGroupId;
    }

    public void setBrokerGroupId(long brokerGroupId) {
        this.brokerGroupId = brokerGroupId;
    }

    public List<Integer> getBrokerGroupIds() {
        return brokerGroupIds;
    }

    public void setBrokerGroupIds(List<Integer> brokerGroupIds) {
        this.brokerGroupIds = brokerGroupIds;
    }

    public Identity getGroup() {
        return group;
    }

    public void setGroup(Identity group) {
        this.group = group;
    }

    public List<Integer> getNotInBrokerIds() {
        return notInBrokerIds;
    }

    public void setNotInBrokerIds(List<Integer> notInBrokerIds) {
        this.notInBrokerIds = notInBrokerIds;
    }

    public List<Integer> getInBrokerIds() {
        return inBrokerIds;
    }

    public void setInBrokerIds(List<Integer> inBrokerIds) {
        this.inBrokerIds = inBrokerIds;
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }
}
