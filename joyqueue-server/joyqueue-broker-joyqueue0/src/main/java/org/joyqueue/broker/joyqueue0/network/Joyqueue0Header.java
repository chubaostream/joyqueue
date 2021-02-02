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
package org.joyqueue.broker.joyqueue0.network;

import org.joyqueue.domain.QosLevel;
import org.joyqueue.network.transport.command.Direction;
import org.joyqueue.network.transport.command.Header;
import org.joyqueue.toolkit.time.SystemClock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * jmq协议头
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/21
 */
public class Joyqueue0Header implements Header {

    private static final AtomicInteger requestIdGenerator = new AtomicInteger(0);

    public static final byte VERSION = 1;

    // 版本
    private byte version;
    // ack
    private QosLevel qosLevel;
    // 方向
    private Direction direction;
    // 请求id
    private int requestId;
    // 命令类型
    private int type;
    //请求或响应时间
    private long time;

    // 响应部分
    // 状态码
    private int status;
    // 错误信息
    private String error;

    public Joyqueue0Header() {}

    public Joyqueue0Header(Direction direction, int type) {
        this(direction, QosLevel.RECEIVE, type);
    }

    public Joyqueue0Header(Direction direction, QosLevel qosLevel, int type) {
        this.type = type;
        this.version = VERSION;
        this.direction = direction;
        this.qosLevel = qosLevel;
        this.time = SystemClock.now();
        this.requestId = generateRequestId();
    }

    public Joyqueue0Header(byte version, QosLevel qosLevel, Direction direction, int requestId, int type, long time, int status, String error) {
        this.version = version;
        this.qosLevel = qosLevel;
        this.direction = direction;
        this.requestId = requestId;
        this.type = type;
        this.time = time;
        this.status = status;
        this.error = error;
    }

    @Override
    public int getRequestId() {
        return requestId;
    }

    @Override
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = (byte) version;
    }

    @Override
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public void setQosLevel(QosLevel qosLevel) {
        this.qosLevel = qosLevel;
    }

    @Override
    public QosLevel getQosLevel() {
        return qosLevel;
    }

    public long getTime() {
        return time;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String getError() {
        return error;
    }

    protected int generateRequestId() {
        return requestIdGenerator.incrementAndGet();
    }

    @Override
    public String toString() {
        return "JMQHeader{" +
                "version=" + version +
                ", qosLevel=" + qosLevel +
                ", requestId=" + requestId +
                ", type=" + type +
                ", time=" + time +
                ", status=" + status +
                ", error='" + error + '\'' +
                '}';
    }
}