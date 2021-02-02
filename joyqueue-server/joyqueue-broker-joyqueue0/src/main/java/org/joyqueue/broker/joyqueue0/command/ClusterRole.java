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
package org.joyqueue.broker.joyqueue0.command;

/**
 * 集群角色
 */
public enum ClusterRole {
    /**
     * 未知
     */
    NONE,
    /**
     * 主
     */
    MASTER,
    /**
     * 从
     */
    SLAVE,
    /**
     * 备份，不参与选举
     */
    BACKUP;

    public static ClusterRole valueOf(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IndexOutOfBoundsException("Invalid ordinal");
        }
        return values()[ordinal];
    }

    /**
     * 根据端口来判断角色
     *
     * @param port 端口
     * @return 角色
     */
    public static ClusterRole getRoleByPort(int port) {
        if (port >= 50000) {
            // 通过端口规范获取
            boolean isEven = port % 2 == 0;
            if (isEven) {
                return ClusterRole.MASTER;
            } else if (port % 10000 <= 5535) {
                return ClusterRole.SLAVE;
            } else {
                return ClusterRole.BACKUP;
            }
        }
        return null;
    }

    /**
     * 根据别名获取角色
     *
     * @param alias 别名,命名规则BrokerGroupCode_m/s,如jmq1_m,jmq1_s;m表示master,s表示slave
     * @return 角色
     */
    public static ClusterRole getRoleByAlias(String alias) {
        if (alias == null || alias.isEmpty()) {
            return null;
        }
        // 通过别名获取
        char type = 'n';
        int pos = alias.lastIndexOf('_');
        if (pos > 0) {
            String tmp = alias.substring(pos + 1);
            if (!tmp.isEmpty()) {
                type = Character.toLowerCase(tmp.charAt(0));
            }
        }
        switch (type) {
            case 'm':
                return ClusterRole.MASTER;
            case 's':
                return ClusterRole.SLAVE;
            case 'b':
                return ClusterRole.BACKUP;
            default:
                return null;
        }
    }

    /**
     * 主从选举候选者
     *
     * @return
     */
    public boolean isCandidate() {
        switch (this) {
            case MASTER:
                return true;
            case SLAVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否可读
     *
     * @return 可读标示
     */
    public boolean readable() {
        return this != NONE;
    }

    /**
     * 是否可写
     *
     * @return 可写标示
     */
    public boolean writable() {
        return this == MASTER;
    }

}