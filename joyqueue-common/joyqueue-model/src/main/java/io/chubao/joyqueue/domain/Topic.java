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
package io.chubao.joyqueue.domain;

import java.io.Serializable;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Topic)) return false;
        Topic topic = (Topic) o;
        return partitions == topic.partitions &&
                Objects.equals(name, topic.name) &&
                type == topic.type &&
                Objects.equals(priorityPartitions, topic.priorityPartitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, partitions, type, priorityPartitions);
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
