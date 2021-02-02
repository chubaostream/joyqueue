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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 集群模型
 */
public class BrokerCluster implements Serializable {
    private static final long serialVersionUID = 518558108264597594L;
    // 主题
    private String topic;
    // 分组
    private List<BrokerGroup> groups = new ArrayList<BrokerGroup>(); //构造对象时创建，优化同步判断
    // 标记
    private short queues;

    public BrokerCluster() {
    }

    public BrokerCluster(String address) {
        if (address != null && !address.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(address, ";");
            while (tokenizer.hasMoreTokens()) {
                groups.add(new BrokerGroup(tokenizer.nextToken()));
            }
        }
    }

    public BrokerCluster(String[] addresses) {
        if (addresses != null) {
            for (String address : addresses) {
                groups.add(new BrokerGroup(address));
            }
        }
    }

    public BrokerCluster(String topic, List<BrokerGroup> groups, short queues) {
        this.topic = topic;
        this.queues = queues;
        if (groups != null) {
            this.groups = groups;
        }
    }

    public BrokerCluster topic(final String topic) {
        setTopic(topic);
        return this;
    }

    public BrokerCluster queues(final short queues) {
        setQueues(queues);
        return this;
    }

    public BrokerCluster groups(final List<BrokerGroup> groups) {
        setGroups(groups);
        return this;
    }

    public BrokerCluster groups(final BrokerGroup... groups) {
        if (groups != null) {
            setGroups(Arrays.asList(groups));
        }
        return this;
    }

    public List<BrokerGroup> getGroups() {
        return this.groups;
    }

    public void setGroups(List<BrokerGroup> groups) {
        this.groups = groups;
    }

    public short getQueues() {
        return this.queues;
    }

    public void setQueues(short queues) {
        this.queues = queues;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * 获取分组
     *
     * @param broker
     */
    public BrokerGroup getGroup(String broker) {
        for (BrokerGroup brokerGroup : groups) {
            if (brokerGroup.getBroker(broker) != null) {
                return brokerGroup;
            }
        }

        return null;
    }

    /**
     * 根据权限获取
     *
     * @param permission 权限
     */
    public List<BrokerGroup> getGroups(Permission permission) {
        List<BrokerGroup> result = new ArrayList<BrokerGroup>();
        for (BrokerGroup group : groups) {
            if (group.getPermission().contain(permission)) {
                result.add(group);
            }
        }
        return result;
    }

    /**
     * 包含Broker
     *
     * @param broker Broker名称
     */
    public boolean contain(String broker) {
        return getGroup(broker) != null;
    }

    /**
     * 包含组
     *
     * @param group 分组
     */
    public boolean contain(BrokerGroup group) {
        for (BrokerGroup bg : groups) {
            if (bg.getGroup().equals(group.getGroup())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据分组查找分组
     *
     * @param group 分组
     * @return 原有的分组
     */
    public BrokerGroup getGroup(BrokerGroup group) {
        if (group == null) {
            return null;
        }
        for (BrokerGroup bg : groups) {
            if (bg.equals(group)) {
                return bg;
            }
        }
        return null;
    }

    /**
     * 添加组
     *
     * @param group 分组
     */
    public void addGroup(BrokerGroup group) {
        if (group != null) {
            this.groups.add(group);
        }
    }

    /**
     * 移除组
     *
     * @param group 分组
     */
    public void removeGroup(BrokerGroup group) {
        if (group != null) {
            this.groups.remove(group);
        }
    }

    public BrokerCluster clone() {
        BrokerCluster target = new BrokerCluster();
        target.setTopic(topic);
        target.setQueues(queues);
        for (BrokerGroup group : groups) {
            target.addGroup(group.clone());
        }
        return target;
    }

    /*
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(groups.get(i).toString());
        }
        return sb.toString();
    }
    */

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BrokerCluster cluster = (BrokerCluster) o;

        if (groups != null ? !groups.equals(cluster.groups) : cluster.groups != null) {
            return false;
        }
        if (topic != null ? !topic.equals(cluster.topic) : cluster.topic != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = topic != null ? topic.hashCode() : 0;
        result = 31 * result + (groups != null ? groups.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BrokerCluster:");
        sb.append("{topic:" + topic);
        sb.append(", groups:[");
        for (BrokerGroup brokerGroup : groups) {
            sb.append("group:" + brokerGroup.getGroup());
            sb.append(", brokers:[");
            for (Joyqueue0Broker broker : brokerGroup.getBrokers()) {
                sb.append("{id:" + broker.getId());
                sb.append(", ip:" + broker.getIp());
                sb.append(", permission:" + broker.getPermission() + "}");
            }
            sb.append("]");
        }
        sb.append("]}");

        return sb.toString();
    }
}