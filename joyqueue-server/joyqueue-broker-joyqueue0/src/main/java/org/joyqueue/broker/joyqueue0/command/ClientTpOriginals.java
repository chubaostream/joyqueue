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

import org.joyqueue.toolkit.stat.TPStat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClientTpAndMachineStat
 *
 * @author luoruiheng
 * @since 11/24/16
 */
public class ClientTpOriginals extends TPStat {

    private static final long serialVersionUID = 7603677380283596564L;
    
    /**
     * tp times map
     * key   - tp time digit  (Short)
     * value - tp time counts (Short)
     */
    private Map<Integer, AtomicInteger> tpTimes = new HashMap<Integer, AtomicInteger>();

    public ClientTpOriginals() {

    }

    public ClientTpOriginals(long count, long success, long error, long size, long time) {
        super(count, success, error, size, time);
    }

    public Map<Integer, AtomicInteger> getTpTimes() {
        return tpTimes;
    }

    public void setTpTimes(Map<Integer, AtomicInteger> tpTimes) {
        this.tpTimes = tpTimes;
    }

    @Override
    public String toString() {
        return "ClientTpOriginals{" +
                "tpTimes=" + tpTimes +
                '}';
    }
}
