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

import org.joyqueue.toolkit.time.SystemClock;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端TP性能统计
 */
public class ClientTPStat {
    // ip
    private String ip;
    // 主题
    private String topic;
    // 应用
    private String app;
    // 开始时间
    private long startTime;
    // 结束时间
    private long endTime;
    // 生产性能
    private ClientTpOriginals produce;
    // 接收性能
    private ClientTpOriginals receive;
    // 消费性能
    private ClientTpOriginals consume;
    // 发送重试性能
    private ClientTpOriginals retry;

    // avg time of ping response
    // key   - BrokerGroup(String)
    // value - Double
    private Map<Object, Object> pingAvgResponseTime = new HashMap<Object, Object>();

    // cpu usage percent
    private double cpuUsage = 0.0;

    // memory usage percent
    private double memUsage = 0.0;

    // load in one minute
    private double oneMinLoad = 0.0;

    public ClientTPStat() {
    }

    public ClientTPStat(String topic, String app) {
        this.topic = topic;
        this.app = app;
        this.startTime = SystemClock.now();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public ClientTpOriginals getProduce() {
        return produce;
    }

    public void setProduce(ClientTpOriginals produce) {
        this.produce = produce;
    }

    public ClientTpOriginals getReceive() {
        return receive;
    }

    public void setReceive(ClientTpOriginals receive) {
        this.receive = receive;
    }

    public ClientTpOriginals getConsume() {
        return consume;
    }

    public void setConsume(ClientTpOriginals consume) {
        this.consume = consume;
    }

    public ClientTpOriginals getRetry() {
        return retry;
    }

    public void setRetry(ClientTpOriginals retry) {
        this.retry = retry;
    }

    public Map<Object, Object> getPingAvgResponseTime() {
        return pingAvgResponseTime;
    }

    public void setPingAvgResponseTime(Map<Object, Object> pingAvgResponseTime) {
        this.pingAvgResponseTime = pingAvgResponseTime;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemUsage() {
        return memUsage;
    }

    public void setMemUsage(double memUsage) {
        this.memUsage = memUsage;
    }

    public double getOneMinLoad() {
        return oneMinLoad;
    }

    public void setOneMinLoad(double oneMinLoad) {
        this.oneMinLoad = oneMinLoad;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("{");
        sb.append("\"startTime\":").append(startTime);
        sb.append(", \"endTime\":").append(endTime);
        if (ip != null) {
            sb.append(", \"ip\":\"").append(ip).append('\"');
        }
        if (topic != null) {
            sb.append(", \"topic\":\"").append(topic).append('\"');
        }
        if (app != null) {
            sb.append(", \"app\":\"").append(app).append('\"');
        }
        if (produce != null) {
            sb.append(", \"produce\":").append(produce);
        }
        if (receive != null) {
            sb.append(", \"receive\":").append(receive);
        }
        if (consume != null) {
            sb.append(", \"consume\":").append(consume);
        }
        if (retry != null) {
            sb.append(", \"retry\":").append(retry);
        }
        sb.append(", \"cpuUsage\":").append(cpuUsage);
        sb.append(", \"memUsage\":").append(memUsage);
        sb.append(", \"oneMinLoad\":").append(oneMinLoad);
        sb.append(", \"pingAvgResponseTime\":").append(pingAvgResponseTime);
        sb.append('}');
        return sb.toString();
    }
}
