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
package org.joyqueue.server.retry.db;

import com.jd.laf.extension.Extension;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.server.retry.api.MessageRetry;
import org.joyqueue.server.retry.api.RetryPolicyProvider;
import org.joyqueue.server.retry.model.RetryMessageModel;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Extension("DB")
public class CacheMessageRetry implements MessageRetry<Long> {
    private static final Logger logger = LoggerFactory.getLogger(CacheMessageRetry.class);

    private DBMessageRetry dbMessageRetry = new DBMessageRetry();

    private PropertySupplier propertySupplier;

    private RetryPolicyProvider retryPolicyProvider;

    @Override
    public void start() throws Exception {
        if (propertySupplier != null) {
            dbMessageRetry.setSupplier(propertySupplier);
        }

        // start dbMessageRetry
        dbMessageRetry.start();

        // TODO init cache server here
        logger.info("CacheMessageRetry is started");
    }

    @Override
    public void stop() {
        dbMessageRetry.stop();
        // TODO close cache server here
        logger.info("CacheMessageRetry is stopped");
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public void addRetry(List<RetryMessageModel> retryMessageModelList) throws JoyQueueException {
        // Generate and insert into DB
        dbMessageRetry.addRetry(retryMessageModelList);
        // TODO insert into cache
    }

    @Override
    public void retrySuccess(String topic, String app, Long[] messageIds) throws JoyQueueException {
        // Update DB
        dbMessageRetry.retrySuccess(topic, app, messageIds);
        // TODO delete cache
    }

    @Override
    public void retryError(String topic, String app, Long[] messageIds) throws JoyQueueException {
        dbMessageRetry.retryError(topic, app, messageIds);
    }

    @Override
    public void retryExpire(String topic, String app, Long[] messageIds) throws JoyQueueException {
        dbMessageRetry.retryExpire(topic, app, messageIds);
        // TODO delete cache
    }

    @Override
    public List<RetryMessageModel> getRetry(String topic, String app, short count, long startIndex) throws JoyQueueException {
        // TODO get from cache first
        // If not exists, get from DB
        return dbMessageRetry.getRetry(topic, app, count, startIndex);
    }

    @Override
    public int countRetry(String topic, String app) throws JoyQueueException {
        return dbMessageRetry.countRetry(topic, app);
    }

    @Override
    public void setRetryPolicyProvider(RetryPolicyProvider retryPolicyProvider) {
        this.retryPolicyProvider = retryPolicyProvider;
        dbMessageRetry.setRetryPolicyProvider(retryPolicyProvider);
    }

    @Override
    public void setSupplier(PropertySupplier supplier) {
        this.propertySupplier = supplier;
    }


}
