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
package org.joyqueue.model.domain;

import org.joyqueue.model.domain.nsr.BaseNsrModel;

import java.util.HashMap;
import java.util.Map;

public class ProducerConfig extends BaseNsrModel {

    private String producerId;


    /**
     * 是否就近发送
     **/
    private Boolean nearBy;

    /**
     * 集群实例发送权重
     **/
    private String weight;

    /**
     * 是否归档
     */
    private Boolean archive;

    /**
     * 单个发送者
     **/
    private Boolean single = false;

    private String blackList;

    private Integer limitTps;

    private Integer limitTraffic;

    private Integer timeout;

    private Integer qosLevel;
    private String region;

    private Map<String, String> params;

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public Boolean isNearBy() {
        return nearBy;
    }

    public void setNearBy(boolean nearBy) {
        this.nearBy = nearBy;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public Boolean isSingle() {
        return single;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }

    public Boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public String getBlackList() {
        return blackList;
    }

    public void setBlackList(String blackList) {
        this.blackList = blackList;
    }

    public void setLimitTps(int limitTps) {
        this.limitTps = limitTps;
    }

    public Integer getLimitTps() {
        return limitTps;
    }

    public void setLimitTraffic(int limitTraffic) {
        this.limitTraffic = limitTraffic;
    }

    public Integer getLimitTraffic() {
        return limitTraffic;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Integer getQosLevel() {
        return qosLevel;
    }

    public void setQosLevel(Integer qosLevel) {
        this.qosLevel = qosLevel;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * 获取权重
     *
     * @return 权重
     */
    public Map<String, Short> weights() {
        if (weight == null || weight.isEmpty()) {
            return null;
        }
        Map<String, Short> map = new HashMap<String, Short>();
        String[] values = weight.split(",");
        String[] parts;
        for (String value : values) {
            parts = value.split(":");
            if (parts.length >= 2) {
                try {
                    map.put(parts[0], Short.parseShort(parts[1]));
                } catch (NumberFormatException e) {
                }
            }
        }

        return map;
    }
}
