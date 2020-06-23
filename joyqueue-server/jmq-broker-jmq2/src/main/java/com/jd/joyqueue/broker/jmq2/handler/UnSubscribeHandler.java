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
package com.jd.joyqueue.broker.jmq2.handler;


import com.jd.joyqueue.broker.jmq2.JMQ2CommandHandler;
import com.jd.joyqueue.broker.jmq2.JMQ2CommandType;
import com.jd.joyqueue.broker.jmq2.command.BooleanAck;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.network.command.UnSubscribe;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Type;
import org.joyqueue.nsr.NameService;

/**
 * @author wylixiaobin
 * Date: 2018/10/10
 */
@Deprecated
public class UnSubscribeHandler implements JMQ2CommandHandler, Type, BrokerContextAware {
    private NameService nameService;

    @Override
    public int type() {
        return JMQ2CommandType.UNSUBSCRIBE.getCode();
    }

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.nameService = brokerContext.getNameService();
    }

    @Override
    public Command handle(Transport transport, Command command) {
        UnSubscribe unSubscribe = (UnSubscribe) command.getPayload();
        nameService.unSubscribe(unSubscribe.getSubscriptions());
        return BooleanAck.build();
    }
}
