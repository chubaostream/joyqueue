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
package org.joyqueue.broker.joyqueue0.config;

import org.joyqueue.toolkit.config.PropertyDef;

/**
 * Joyqueue0ConfigKey
 * author: gaohaoxiang
 * date: 2019/11/12
 */
public enum Joyqueue0ConfigKey implements PropertyDef {

    CLUSTER_BODY_CACHE_ENABLE("joyqueue0.cluster.body.cache.enable", false, Type.BOOLEAN),
    CLUSTER_BODY_CACHE_EXPIRE_TIME("joyqueue0.cluster.body.cache.expire.time", 1000 * 30, Type.INT),
    CLUSTER_BODY_CACHE_UPDATE_INTERVAL("joyqueue0.cluster.body.update.interval", 1000 * 30, Type.INT),
    CLUSTER_BODY_WITH_SLAVE("joyqueue0.cluster.body.with.slave", false, Type.BOOLEAN),
    MESSAGE_BUSINESSID_REWRITE_PREFIX("joyqueue0.message.businessId.rewrite.", false, Type.BOOLEAN),

    ;


    private String name;
    private Object value;
    private PropertyDef.Type type;

    Joyqueue0ConfigKey(String name, Object value, PropertyDef.Type type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return type;
    }
}