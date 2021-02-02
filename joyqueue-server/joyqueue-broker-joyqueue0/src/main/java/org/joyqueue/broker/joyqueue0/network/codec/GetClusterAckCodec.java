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
package org.joyqueue.broker.joyqueue0.network.codec;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.BrokerCluster;
import org.joyqueue.broker.joyqueue0.command.BrokerGroup;
import org.joyqueue.broker.joyqueue0.command.GetClusterAck;
import org.joyqueue.broker.joyqueue0.command.Joyqueue0Broker;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0Header;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.util.Serializer;
import io.netty.buffer.ByteBuf;
import org.joyqueue.network.transport.command.Type;

import java.util.List;

/**
 * 获取集群应答编码器
 */
public class GetClusterAckCodec implements Joyqueue0PayloadCodec<GetClusterAck>, Type {

    /**
     * <i>协议格式：</i>
     * <pre>
     *      +----------+----------+       —  +----------+----------+
     *      |  客户端机房(1字节)  |     /    |  主题长度(1字节)    |
     *      +----------+----------+          +----------+----------+
     *      |   条数(2字节)       |   /      |  主题(字节数组)     |
     *      +----------+----------+ -        +----------+----------+       -  +----------+----------+
     *      |   主题集群1         |          |	队列数(1字节)      |    /     |  分组权限(1字节)    |
     *      +----------+----------+          +----------+----------+          +----------+----------+    - - -
     *      +----------+----------+
     *      |   主题集群2         |  \       |	分组数量(1字节)    |  /       |  地址数量(1字节)    |  /       |     地址(6字节)     |
     *      +----------+----------+          +----------+----------+          +----------+----------+
     *      +----------+----------+
     *      |                     |    \     |     分组1           |          |      Broker1        |          |     角色
     *      (1字节)     |
     *      |                     |          +----------+----------+          +----------+----------+
     *      +----------+----------+
     *      |   主题集群n         |      \   |     分组2           |  \       |      Broker2        |  \       |     机房
     *      (4字节)     |
     *      |                     |          +----------+----------+          +----------+----------+    - - -
     *      +----------+----------+
     *      |                     |        \ |     分组n           |    \     |      Brokern        |
     *      +----------+----------+          +----------+----------+       -  +----------+----------+
     * </pre>
     *
     * @param out
     * @throws Exception
     */
    @Override
    public void encode(final GetClusterAck payload, final ByteBuf out) throws Exception {
        payload.validate();

        if (payload.getCacheBody() != null) {
            out.writeBytes(payload.getCacheBody());
        } else {
            out.writeByte(payload.getDataCenter());
            out.writeInt(payload.getInterval());
            out.writeInt(payload.getMaxSize());
            List<BrokerCluster> clusters = payload.getClusters();
            int count = clusters == null ? 0 : clusters.size();
            // 2字节条数
            out.writeShort(count);

            for (int i = 0; i < count; i++) {
                BrokerCluster cluster = clusters.get(i);
                List<BrokerGroup> groups = cluster.getGroups();
                // 1字节主题长度
                Serializer.write(cluster.getTopic(), out);
                // 1字节队列数
                out.writeByte(cluster.getQueues());
                // 1字节分组数量
                int groupCount = groups == null ? 0 : groups.size();
                out.writeByte(groupCount);
                for (int j = 0; j < groupCount; j++) {
                    BrokerGroup group = groups.get(j);
                    // 权限
                    out.writeByte(group.getPermission().ordinal());
                    // Broker数据
                    List<Joyqueue0Broker> brokers = group.getBrokers();
                    int brokerCount = brokers == null ? 0 : brokers.size();
                    // 1字节Broker数量
                    out.writeByte(brokerCount);
                    for (int k = 0; k < brokerCount; k++) {
                        Joyqueue0Broker broker = brokers.get(k);
                        // 1字节长度字符串
                        Serializer.write(broker.getName(), out);
                        // 1字节长度字符串
                        Serializer.write(broker.getAlias(), out);
                        // 1字节数据中心
                        out.writeByte(broker.getDataCenter());
                        // 1字节权限
                        out.writeByte(broker.getPermission().ordinal());
                    }
                }
            }
            // 兼容原来的协议，权重追加到最后
            for (int i = 0; i < count; i++) {
                BrokerCluster cluster = clusters.get(i);
                List<BrokerGroup> groups = cluster.getGroups();
                // 1字节分组数量
                int groupCount = groups == null ? 0 : groups.size();
                for (int j = 0; j < groupCount; j++) {
                    BrokerGroup group = groups.get(j);
                    // 权限
                    out.writeShort(group.getWeight());
                }
            }
            Serializer.write(payload.getAllTopicConfigStrings(), out, 4);
        }
    }

    @Override
    public Object decode(Joyqueue0Header header, ByteBuf buffer) throws Exception {
        return null;
    }

    @Override
    public int type() {
        return Joyqueue0CommandType.GET_CLUSTER_ACK.getCode();
    }
}