package org.joyqueue.broker.joyqueue0.command;

import org.joyqueue.broker.joyqueue0.network.Joyqueue0Payload;

/**
 * 健康检查，要判断读写权限
 */
public abstract class GetHealth extends Joyqueue0Payload {
    // 主题
    protected String topic;
    // 应用
    protected String app;
    // 客户端所在数据中心
    protected byte dataCenter;


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

    public byte getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(byte dataCenter) {
        this.dataCenter = dataCenter;
    }
}