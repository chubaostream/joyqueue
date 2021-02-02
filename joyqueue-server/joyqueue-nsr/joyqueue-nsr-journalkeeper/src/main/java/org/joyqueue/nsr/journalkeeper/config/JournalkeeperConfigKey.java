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
package org.joyqueue.nsr.journalkeeper.config;

import org.joyqueue.toolkit.config.PropertyDef;
import org.joyqueue.toolkit.network.IpUtil;

/**
 * JournalkeeperConfigKey
 * author: gaohaoxiang
 * date: 2019/8/14
 */
public enum JournalkeeperConfigKey implements PropertyDef {

    PREFIX("nameserver.journalkeeper", null, PropertyDef.Type.STRING),
    ROLE("nameserver.journalkeeper.role", "VOTER", PropertyDef.Type.STRING),
    LOCAL("nameserver.journalkeeper.local", IpUtil.getLocalIp(), PropertyDef.Type.STRING),
    NODES("nameserver.journalkeeper.nodes", null, PropertyDef.Type.STRING),
    WAIT_LEADER_TIMEOUT("nameserver.journalkeeper.waitLeaderTimeout", 1000 * 60 * 5, PropertyDef.Type.INT),
    WORKING_DIR("nameserver.journalkeeper.working.dir", null, PropertyDef.Type.STRING),
    INIT_FILE("nameserver.journalkeeper.init.file", "/metadata/sql/schema_tpaas.sql", PropertyDef.Type.STRING),

    SNAPSHOT_INTERVAL_SEC("nameserver.journalkeeper.snapshot.interval.sec", 0, PropertyDef.Type.INT),

    JOURNAL_RETENTION_MIN_KEY("nameserver.journalkeeper.journal.retention.min", 0, PropertyDef.Type.INT),

    RPC_TIMEOUT("nameserver.journalkeeper.rpc.timeout", 1000 * 60 * 1, PropertyDef.Type.INT),
    EXECUTE_TIMEOUT("nameserver.journalkeeper.execute.timeout", 1000 * 3, PropertyDef.Type.INT),

    FLUSH_INTERVAL("nameserver.journalkeeper.flush.interval", 50, PropertyDef.Type.INT),

    STATE_BATCH_SIZE("nameserver.journalkeeper.state.batch.size", 1024 * 1024 * 1, PropertyDef.Type.INT),

    METRIC_ENABLE("nameserver.journalkeeper.metric.enable", false, PropertyDef.Type.BOOLEAN),
    METRIC_PRINT_INTERVAL("nameserver.journalkeeper.metric.print.interval", 5, PropertyDef.Type.INT),

    ;

    public static final String NODE_SPLITTER = ",";
    public static final String DEFAULT_WORKING_DIR = "/metadata/journalkeeper";

    private String name;
    private Object value;
    private PropertyDef.Type type;

    JournalkeeperConfigKey(String name, Object value, Type type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }
}
