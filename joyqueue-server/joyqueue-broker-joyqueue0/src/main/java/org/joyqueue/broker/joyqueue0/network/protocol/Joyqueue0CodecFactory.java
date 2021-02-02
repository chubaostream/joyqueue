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
package org.joyqueue.broker.joyqueue0.network.protocol;

import com.google.common.collect.Lists;
import org.joyqueue.broker.joyqueue0.network.Joyqueue0PayloadCodec;
import org.joyqueue.broker.joyqueue0.network.codec.ResetConsumeOffsetCodec;
import com.jd.laf.extension.ExtensionManager;
import org.joyqueue.network.codec.*;
import org.joyqueue.network.transport.codec.*;
import org.joyqueue.network.transport.codec.support.DefaultCodec;

import java.util.List;

/**
 * jmq编解码器工厂
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/27
 */
public class Joyqueue0CodecFactory implements CodecFactory {

    private PayloadCodecFactory payloadCodecFactory;

    public Joyqueue0CodecFactory() {
        this.payloadCodecFactory = initPayloadCodecFactory();
    }

    protected PayloadCodecFactory initPayloadCodecFactory() {
        List<Joyqueue0PayloadCodec> payloadCodecs = loadPayloadCodecs();
        PayloadCodecFactory payloadCodecFactory = new PayloadCodecFactory();
        for (Joyqueue0PayloadCodec payloadCodec : payloadCodecs) {
            payloadCodecFactory.register(payloadCodec);
        }

        // TODO 临时
        registerMqtt(payloadCodecFactory);
        return payloadCodecFactory;
    }

    protected void registerMqtt(PayloadCodecFactory payloadCodecFactory) {
        payloadCodecFactory.register(new SubscribeCodec());
        payloadCodecFactory.register(new SubscribeAckCodec());
        payloadCodecFactory.register(new UnSubscribeCodec());

        payloadCodecFactory.register(new NullPayloadCodec());

        payloadCodecFactory.register(new GetTopicsCodec());
        payloadCodecFactory.register(new GetTopicsAckCodec());
        payloadCodecFactory.register(new AuthorizationCodec());
        payloadCodecFactory.register(new ResetConsumeOffsetCodec());
    }

    protected List<Joyqueue0PayloadCodec> loadPayloadCodecs() {
        return Lists.newArrayList(ExtensionManager.getOrLoadExtensions(Joyqueue0PayloadCodec.class));
    }

    @Override
    public Codec getCodec() {
        Joyqueue0HeaderCodec headerCodec = new Joyqueue0HeaderCodec();
        return new DefaultCodec(new DefaultDecoder(headerCodec, payloadCodecFactory), new DefaultEncoder(headerCodec, payloadCodecFactory));
    }
}