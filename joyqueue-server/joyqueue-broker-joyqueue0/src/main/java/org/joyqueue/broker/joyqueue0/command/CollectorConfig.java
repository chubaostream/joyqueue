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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuduohui on 2018/1/31.
 */
public class CollectorConfig implements Serializable{
    private List<Collector> collectors = new ArrayList<Collector>();

    private int profileInterval = 30000;
    private int machineMetricInterval = 20000;
    private boolean enableProfileMetric = true;
    private boolean enableMachineMetric = true;

    public CollectorConfig() {}

    public List<Collector> getCollectors() {
        return collectors;
    }

    public void setCollectors(List<Collector> collectors) {
        this.collectors = collectors;
    }

    public int getProfileInterval() {
        return profileInterval;
    }

    public void setProfileInterval(int profileInterval) {
        this.profileInterval = profileInterval;
    }

    public int getMachineMetricInterval() {
        return machineMetricInterval;
    }

    public void setMachineMetricInterval(int machineMetricInterval) {
        this.machineMetricInterval = machineMetricInterval;
    }

    public boolean isEnableProfileMetric() {
        return enableProfileMetric;
    }

    public void setEnableProfileMetric(boolean enableProfileMetric) {
        this.enableProfileMetric = enableProfileMetric;
    }

    public boolean isEnableMachineMetric() {
        return enableMachineMetric;
    }

    public void setEnableMachineMetric(boolean enableMachineMetric) {
        this.enableMachineMetric = enableMachineMetric;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"collectors\":").append(collectors)
                .append(",\"profileInterval\":\"").append(profileInterval)
                .append(",\"machineMetricInterval\":\"").append(machineMetricInterval)
                .append(",\"enableProfileMetric\":\"").append(enableProfileMetric)
                .append(",\"enableMachineMetric\":\"").append(enableMachineMetric)
                .append("}");
        return sb.toString();
    }
}
