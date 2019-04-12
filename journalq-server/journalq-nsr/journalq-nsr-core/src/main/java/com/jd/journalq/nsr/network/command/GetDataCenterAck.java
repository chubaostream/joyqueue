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
package com.jd.journalq.nsr.network.command;

import com.jd.journalq.domain.DataCenter;
import com.jd.journalq.network.transport.command.JMQPayload;

/**
 * @author wylixiaobin
 * Date: 2019/1/29
 */
public class GetDataCenterAck extends JMQPayload {
    private DataCenter dataCenter;

    public GetDataCenterAck dataCenter(DataCenter dataCenter){
        this.dataCenter = dataCenter;
        return this;
    }

    public DataCenter getDataCenter() {
        return dataCenter;
    }

    @Override
    public int type() {
        return NsrCommandType.GET_DATACENTER_ACK;
    }
}
