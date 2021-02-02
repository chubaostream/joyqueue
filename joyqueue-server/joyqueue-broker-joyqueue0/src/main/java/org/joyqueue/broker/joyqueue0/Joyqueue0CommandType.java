/**
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
package org.joyqueue.broker.joyqueue0;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 命令类型.
 *
 * @author lindeqiang
 * @since 2016/7/20 10:31
 */
public enum Joyqueue0CommandType {

    // 发送消息
    PUT_MESSAGE(1),
    // 取消息
    GET_MESSAGE(2),
    // 取消息应答
    GET_MESSAGE_ACK(102),
    // 消费应答消息
    ACK_MESSAGE(3),
    // 重试消息
    RETRY_MESSAGE(4),
    // 存放重试消息
    PUT_RETRY(5),
    // 获取重试消息
    GET_RETRY(6),
    // 获取重试消息应答
    GET_RETRY_ACK(106),
    // 更新重试消息
    UPDATE_RETRY(7),
    // 获取重试条数
    GET_RETRY_COUNT(8),
    // 获取重试条数应答
    GET_RETRY_COUNT_ACK(108),
    // 事务准备
    PREPARE(10),
    // 事务提交
    COMMIT(11),
    // 事务回滚
    ROLLBACK(12),
    TX_PREPARE(13),
    TX_COMMIT(14),
    TX_ROLLBACK(15),
    TX_FEEDBACK(16),
    TX_FEEDBACK_ACK(116),
    // 心跳
    HEARTBEAT(30),
    // 获取集群
    GET_CLUSTER(31),
    // 获取集群应答
    GET_CLUSTER_ACK(131),
    // 获取生产健康状况
    GET_PRODUCER_HEALTH(32),
    // 获取消费健康状况
    GET_CONSUMER_HEALTH(37),
    //订阅
    SUBSCRIBE(38),
    //订阅响应
    SUBSCRIBE_ACK(39),
    //取消订阅
    UNSUBSCRIBE(40),
    GET_TOPICS(41),
    GET_TOPICS_ACK(42),
    AUTHORIZATION(63),
    // 增加连接
    ADD_CONNECTION(33),
    // 删除连接
    REMOVE_CONNECTION(133),
    // 增加生产者
    ADD_PRODUCER(34),
    // 增加生产者
    REMOVE_PRODUCER(134),
    // 删除消费者
    ADD_CONSUMER(35),
    // 删除消费者
    REMOVE_CONSUMER(135),
    // 客户端性能
    CLIENT_PROFILE(36),
    // 客户端性能应答
    CLIENT_PROFILE_ACK(136),
    // 复制身份
    IDENTITY(50),
    // 获取复制偏移量
    GET_OFFSET(51),
    // 获取复制偏移量应答
    GET_OFFSET_ACK(151),
    // 获取复制日志
    GET_JOURNAL(52),
    // 获取复制日志应答
    GET_JOURNAL_ACK(152),
    // 复制日志
    UPDATE_JOURNAL(53),
    // 复制日志应答
    UPDATE_JOURNAL_ACK(153),
    // Slave在线命令
    ONLINE(54),
    // 投票选举master
    VOTE(55),
    // 复制同步方式
    UPDATE_SYNC_MODE(56),
    // 复制增量命令
    INCREMENTAL(57),
    // 复制增量应答
    INCREMENTAL_ACK(157),
    // 同步消费位置
    GET_CONSUMER_OFFSET(58),
    // 同步消费位置确认
    GET_CONSUMER_OFFSET_ACK(158),
    // 重置消费位置
    RESET_CONSUMER_OFFSET(59),
    // 重置消费位置确认
    RESET_CONSUMER_OFFSET_ACK(159),
    // 获取checksum
    GET_CHECKSUM(60),
    // 获取checksum应答
    GET_CHECKSUM_ACK(160),
    // 获取chunk
    GET_CHUNK(61),
    // 获取chunk的应答
    GET_CHUNK_ACK(161),
    // 获取元数据
    GET_META_DATA(62),
    // 获取元数据应答
    GET_META_DATA_ACK(162),
    //下发Agent的系统指令
    SYSTEM_COMMAND(83),
    // 布尔应答
    BOOLEAN_ACK(100),

    ;

    private static final Map<Integer, Joyqueue0CommandType> TYPES;

    static {
        Map<Integer, Joyqueue0CommandType> types = Maps.newHashMap();
        for (Joyqueue0CommandType commandType : Joyqueue0CommandType.values()) {
            types.put(commandType.getCode(), commandType);
        }
        TYPES = types;
    }

    private int code;

    Joyqueue0CommandType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Joyqueue0CommandType valueOf(int type) {
        return TYPES.get(type);
    }

    public static boolean contains(int type) {
        return TYPES.containsKey(type);
    }
}
