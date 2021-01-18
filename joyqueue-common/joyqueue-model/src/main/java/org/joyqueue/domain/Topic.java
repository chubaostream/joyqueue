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
package org.joyqueue.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author wylixiaobin
 * Date: 2018/8/17
 */
public class Topic implements Serializable {
    /**
     * 主题名称
     */
    protected TopicName name;
    /**
     * 主题队列数
     */
    protected short partitions;
    /**
     * 主题类型
     */
    protected Type type = Type.TOPIC;

    /**
     * 优先级队列
     */
    protected Set<Short> priorityPartitions = new TreeSet();

    /**
     * 策略
     */
    protected TopicPolicy policy;

    public TopicName getName() {
        return name;
    }

    public void setName(TopicName name) {
        this.name = name;
    }

    public short getPartitions() {
        return partitions;
    }

    public void setPartitions(short partitions) {
        this.partitions = partitions;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Set<Short> getPriorityPartitions() {
        return priorityPartitions;
    }

    public void setPriorityPartitions(Set<Short> priorityPartitions) {
        this.priorityPartitions = priorityPartitions;
    }

    public void setPolicy(TopicPolicy policy) {
        this.policy = policy;
    }

    public TopicPolicy getPolicy() {
        return policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Topic)) return false;

        Topic topic = (Topic) o;
        return partitions == topic.partitions &&
                Objects.equals(name, topic.name) &&
                type == topic.type &&
                Objects.equals(priorityPartitions, topic.priorityPartitions) &&
                Objects.equals(policy, topic.policy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, partitions, type, priorityPartitions, policy);
    }

    @Override
    public String toString() {
        return "Topic{" +
                "name=" + name +
                ", partitions=" + partitions +
                ", type=" + type +
                ", priorityPartitions=" + priorityPartitions +
                ", policy=" + policy +
                '}';
    }

    public static class TopicPolicy implements Serializable {
        private Long storeMaxTime;
        private Boolean storeCleanKeepUnconsumed;
        private Integer produceArchiveTps = -1;
        private Integer consumeArchiveTps = -1;
        private Map<String, String> params;

        public Long getStoreMaxTime() {
            return storeMaxTime;
        }

        public void setStoreMaxTime(Long storeMaxTime) {
            this.storeMaxTime = storeMaxTime;
        }

        public void setStoreCleanKeepUnconsumed(Boolean storeCleanKeepUnconsumed) {
            this.storeCleanKeepUnconsumed = storeCleanKeepUnconsumed;
        }

        public Boolean getStoreCleanKeepUnconsumed() {
            return storeCleanKeepUnconsumed;
        }

        public Integer getProduceArchiveTps() {
            return produceArchiveTps;
        }

        public void setProduceArchiveTps(Integer produceArchiveTps) {
            this.produceArchiveTps = produceArchiveTps;
        }

        public Integer getConsumeArchiveTps() {
            return consumeArchiveTps;
        }

        public void setConsumeArchiveTps(Integer consumeArchiveTps) {
            this.consumeArchiveTps = consumeArchiveTps;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public String getParam(String key) {
            if (params == null) {
                return null;
            }
            return params.get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicPolicy that = (TopicPolicy) o;
            return Objects.equals(storeMaxTime, that.storeMaxTime) &&
                    Objects.equals(storeCleanKeepUnconsumed, that.storeCleanKeepUnconsumed) &&
                    Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(storeMaxTime, storeCleanKeepUnconsumed, params);
        }
    }

    /**
     * 主题类型
     */
    public enum Type implements Serializable {
        /**
         * 主题
         */
        TOPIC((byte) 0, "普通主题"),
        /**
         * 广播
         */
        @Deprecated
        BROADCAST((byte) 1, "广播"),
        /**
         * 顺序队列
         */
        SEQUENTIAL((byte) 2, "顺序主题");


        private final byte code;
        private final String name;

        Type(byte code, String name) {
            this.code = code;
            this.name = name;
        }

        public byte code() {
            return this.code;
        }


        public String getName() {
            return this.name;
        }


        public static Type valueOf(final byte value) {
            for (Type type : Type.values()) {
                if (value == type.code) {
                    return type;
                }
            }
            return TOPIC;
        }
    }
}
