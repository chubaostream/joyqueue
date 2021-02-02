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
package org.joyqueue.broker.joyqueue0.converter;

import org.joyqueue.domain.QosLevel;

public class QosConverter {

    public static QosLevel toQosLevel(int acknowledge) {
        switch (acknowledge) {
            case 1:
                return QosLevel.RECEIVE;
            case 2:
                return QosLevel.ONE_WAY;
            case 3:
                return QosLevel.PERSISTENCE;
            default:
                return QosLevel.REPLICATION;
        }
    }

    public static Acknowledge toAcknowledge(int qosLevel) {
        switch (qosLevel) {
            case 0:
                return Acknowledge.ACK_NO;
            case 1:
                return Acknowledge.ACK_RECEIVE;
            case 2:
                return Acknowledge.ACK_WRITE;
            case 3:
            default:
                return Acknowledge.ACK_FLUSH;
        }
    }


    public enum Acknowledge {
        /**
         * 写入Qos级别：
         * ONE_WAY: 客户端单向向Broker发送消息，无应答；
         * RECEIVE: Broker收到消息后应答；
         * PERSISTENCE：Broker将消息写入磁盘后应答；
         * REPLICATION：Broker将消息复制到集群大多数节点后应答，默认值；
         */

        /**
         * 刷盘后应答
         */
        ACK_FLUSH(0),
        /**
         * 接收到数据应答
         */
        ACK_RECEIVE(1),
        /**
         * 不应答
         */
        ACK_NO(2),

        /**
         * 写入后应答
         */
        ACK_WRITE(3);

        private int value;

        Acknowledge(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Acknowledge valueOf(final int value) {
            switch (value) {
                case 0:
                    return ACK_FLUSH;
                case 1:
                    return ACK_RECEIVE;
                case 2:
                    return ACK_NO;
                case 3:
                    return ACK_WRITE;
                default:
                    return ACK_FLUSH;
            }
        }
    }

}
