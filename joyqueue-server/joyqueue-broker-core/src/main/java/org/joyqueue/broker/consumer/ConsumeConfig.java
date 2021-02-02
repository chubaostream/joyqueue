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
package org.joyqueue.broker.consumer;

import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.broker.config.BrokerStoreConfig;
import org.joyqueue.toolkit.config.Property;
import org.joyqueue.toolkit.config.PropertySupplier;

/**
 * @author chengzhiliang on 2018/10/22.
 */
public class ConsumeConfig {
    private static final String CONSUME_POSITION_PATH = "/position";
    private PropertySupplier propertySupplier;
    private String consumePositionPath;
    private BrokerConfig brokerConfig;
    private BrokerStoreConfig brokerStoreConfig;

    public ConsumeConfig(PropertySupplier propertySupplier) {
        this.propertySupplier = propertySupplier;
        this.brokerConfig = new BrokerConfig(propertySupplier);
        this.brokerStoreConfig = new BrokerStoreConfig(propertySupplier);
    }

    public String getConsumePositionPath() {

        if (consumePositionPath == null || consumePositionPath.isEmpty()) {
            synchronized (this) {
                if (consumePositionPath == null) {
                    String prefix = "";
                    if (propertySupplier != null) {
                        Property property = propertySupplier.getProperty(Property.APPLICATION_DATA_PATH);
                        prefix = property == null ? prefix : property.getString();
                    }
                    consumePositionPath = prefix + CONSUME_POSITION_PATH;
                }

            }
        }
        return consumePositionPath;
    }

    public boolean getBroadcastIndexResetEnable() {
        return propertySupplier.getValue(ConsumeConfigKey.BROADCAST_INDEX_RESET_ENABLE);
    }

    public int getBroadcastIndexResetInterval() {
        return propertySupplier.getValue(ConsumeConfigKey.BROADCAST_INDEX_RESET_INTERVAL);
    }

    public int getBroadcastIndexResetTime() {
        return propertySupplier.getValue(ConsumeConfigKey.BROADCAST_INDEX_RESET_TIME);
    }

    public int getRetryRate(){
        return propertySupplier.getValue(ConsumeConfigKey.RETRY_RATE);
    }

    public int getIndexFlushInterval() {
        return propertySupplier.getValue(ConsumeConfigKey.INDEX_FLUSH_INTERVAL);
    }

    /**
     * Get consumer level config from
     *
     **/
    public int getRetryRate(String topic,String app){
        return PropertySupplier.getValue(propertySupplier,ConsumeConfigKey.RETRY_RATE_PREFIX.getName()+String.format("%s.%s",topic,app),
                ConsumeConfigKey.RETRY_RATE_PREFIX.getType(),ConsumeConfigKey.RETRY_RATE_PREFIX.getValue());
    }

    public boolean getRetryForceAck(String topic, String app) {
        return (boolean) propertySupplier.getValue(ConsumeConfigKey.RETRY_FORCE_ACK)
                || (boolean) PropertySupplier.getValue(propertySupplier,
                ConsumeConfigKey.RETRY_FORCE_ACK_PREFIX.getName() + String.format("%s.%s", topic, app),
                ConsumeConfigKey.RETRY_FORCE_ACK_PREFIX.getType(),
                ConsumeConfigKey.RETRY_FORCE_ACK_PREFIX.getValue());
    }

    public void setConsumePositionPath(String consumePositionPath) {
        this.consumePositionPath = consumePositionPath;
    }

    public boolean getLogDetail(String app) {
        return brokerConfig.getLogDetail(app);
    }

    public boolean useLegacyPartitionManager() {
        return propertySupplier.getValue(ConsumeConfigKey.USE_LEGACY_PARTITION_MANAGER);
    }
    public boolean useLegacyConcurrentConsumer() {
        return propertySupplier.getValue(ConsumeConfigKey.USE_LEGACY_CONCURRENT_CONSUMER);
    }

    public int getPartitionSelectRetryMax() {
        return propertySupplier.getValue(ConsumeConfigKey.PARTITION_SELECT_RETRY_MAX);
    }

    public boolean keepUnconsumed(String topic) {
        return brokerStoreConfig.keepUnconsumed(topic);
    }
}
