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
package org.joyqueue.broker.joyqueue0.converter;

import org.joyqueue.broker.consumer.MessageConverter;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.SourceType;

import java.util.Arrays;
import java.util.List;

/**
 * Jmq2ToKafkaMessageConverter
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/7/30
 */
public class Joyqueue0ToKafkaMessageConverter implements MessageConverter {

    public BrokerMessage convert(BrokerMessage message) {
        message.setBody(message.getDecompressedBody());
        return message;
    }

    public List<BrokerMessage> convertBatch(BrokerMessage message) {
        return Arrays.asList(message);
    }

    @Override
    public Byte type() {
        return SourceType.JOYQUEUE0.getValue();
    }

    @Override
    public byte target() {
        return SourceType.KAFKA.getValue();
    }
}