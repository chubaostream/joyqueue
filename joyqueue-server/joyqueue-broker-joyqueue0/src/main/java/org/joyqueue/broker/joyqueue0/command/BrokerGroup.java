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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 分组模型,第一个是Master，第二个是Slave，最后一个是Backup
 */
public class BrokerGroup implements Serializable {

    private static final long serialVersionUID = -7266001157722687953L;
    // 分组中的Broker列表
    private List<Joyqueue0Broker> brokers = new ArrayList<Joyqueue0Broker>();
    // Broker类型
    private Joyqueue0Broker.BrokerType brokerType;
    // 权限
    private Permission permission;
    // 分组
    private String group;
    // 生产者权重，用于分流
    private transient short weight;

    public BrokerGroup() {
    }

    public BrokerGroup(String address) {
        this(null, address, null);
    }

    public BrokerGroup(String group, String address) {
        this(group, address, null);
    }

    public BrokerGroup(String address, Map<String, Joyqueue0Broker> brokers) {
        this(null, address, brokers);
    }

    public BrokerGroup(String group, String address, Map<String, Joyqueue0Broker> brokers) {
        this.group = group;
        if (address != null && !address.isEmpty()) {
            // 按逗号分割
            StringTokenizer tokenizer = new StringTokenizer(address, ",");
            String part;
            Joyqueue0Broker broker = null;
            int count = 0;
            // 循环取地址
            while (tokenizer.hasMoreTokens()) {
                part = tokenizer.nextToken();
                // 查找Broker
                broker = null;
                if (brokers != null) {
                    broker = brokers.get(part);
                }
                // 不存在则创建
                if (broker == null) {
                    broker = new Joyqueue0Broker(part);
                    broker.setGroup(group);
                }
                // 第一个Broker为Master
                if (count == 0) {
                    broker.setRole(ClusterRole.MASTER);
                } else {
                    // 其它为Slave
                    broker.setRole(ClusterRole.SLAVE);
                }
                this.brokers.add(broker);
                count++;
            }
            // 大于两个，最后一个默认为Backup
            if (count > 2 && broker != null) {
                broker.setRole(ClusterRole.BACKUP);
            }
        }
    }

    public BrokerGroup(List<Joyqueue0Broker> brokers) {
        if (brokers != null) {
            this.brokers.addAll(brokers);
        }
    }

    public List<Joyqueue0Broker> getBrokers() {
        return this.brokers;
    }

    public Joyqueue0Broker.BrokerType getBrokerType() {
        if (brokerType == null && !brokers.isEmpty()) {
            brokerType = brokers.get(0).getType();
        }
        return this.brokerType;
    }

    public Permission getPermission() {
        if (permission == null) {
            Permission max = Permission.NONE;
            for (Joyqueue0Broker broker : brokers) {
                if (broker.getPermission().ordinal() > max.ordinal()) {
                    max = broker.getPermission();
                }
            }
            permission = max;
        }
        return this.permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public String getGroup() {
        if ((group == null || group.isEmpty()) && !brokers.isEmpty()) {
            group = brokers.get(0).getGroup();
        }
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setBrokerType(Joyqueue0Broker.BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    /**
     * 注册指定的broker到该broker组，并标记注册的broker所在组为当前组
     *
     * @param broker 要注册的broker
     */
    public void addBroker(Joyqueue0Broker broker) {
        if (broker != null) {
            if ((broker.getGroup() == null || broker.getGroup().isEmpty()) && (group != null && !group.isEmpty())) {
                broker.setGroup(group);
            }
            brokers.add(broker);
        }
    }

    /**
     * 从当前broker组移除指定的broker,并标记移除的broker所在组为null
     *
     * @param broker 要移除的broker
     */
    public void removeBroker(Joyqueue0Broker broker) {
        if (broker != null) {
            brokers.remove(broker);
        }
    }

    public Joyqueue0Broker getMaster() {
        for (Joyqueue0Broker broker : brokers) {
            if (broker != null && broker.getRole() == ClusterRole.MASTER) {
                return broker;
            }
        }

        return null;
    }


    /**
     * 获取Broker组中的所有Slave，不包含backup
     * <p>
     * 该方法不会返回null, 当Broker组中没有Slave, 返回一个空的List
     * </p>
     *
     * @return 包含在当前Broker组中所有slave的List
     */
    public List<Joyqueue0Broker> getSlaves() {
        List<Joyqueue0Broker> slaves = new ArrayList<Joyqueue0Broker>();

        for (Joyqueue0Broker broker : brokers) {
            if (broker != null && broker.getRole() == ClusterRole.SLAVE) {
                slaves.add(broker);
            }
        }

        return slaves;
    }

    /**
     * 获取broker组中的所有Backup
     * <p>
     * 该方法不会返回null, 当Broker组中没有backup返回一个空List
     * </p>
     *
     * @return 包含当前Broker组中所有的backup
     */
    public List<Joyqueue0Broker> getBackups() {
        List<Joyqueue0Broker> backups = new ArrayList<Joyqueue0Broker>();

        for (Joyqueue0Broker broker : brokers) {
            if (broker != null && broker.getRole() == ClusterRole.BACKUP) {
                backups.add(broker);
            }
        }

        return backups;
    }

    /**
     * 获取该Broker组中指定名称的Broker
     *
     * @param name 指定的Broker名称
     * @return 如果找到指定名称的broker，则返回；如果未找到，则返回null
     */
    public Joyqueue0Broker getBroker(String name) {
        for (Joyqueue0Broker broker : brokers) {
            if (broker == null) {
                continue;
            }

            if (broker != null && name.equals(broker.getName())) {
                return broker;
            }
        }

        return null;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    /**
     * 复制一份数据
     *
     * @return 复制数据
     */
    public BrokerGroup clone() {
        BrokerGroup target = new BrokerGroup();
        target.setGroup(group);
        target.setPermission(permission);
        for (Joyqueue0Broker broker : brokers) {
            target.addBroker(broker.clone());
        }
        return target;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BrokerGroup group = (BrokerGroup) o;
        if (!this.getGroup().equals(group.getGroup())) {
            return false;
        }
        if (brokers != null ? !brokers.equals(group.brokers) : group.brokers != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return brokers != null ? brokers.hashCode() : 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(group).append(":");
        sb.append(weight).append(":");
        for (int i = 0; i < brokers.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(brokers.get(i).getName());
            sb.append(",");
            sb.append(brokers.get(i).getPermission());
        }
        return sb.toString();
    }
}