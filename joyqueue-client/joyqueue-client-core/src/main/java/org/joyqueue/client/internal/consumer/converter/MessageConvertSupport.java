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
package org.joyqueue.client.internal.consumer.converter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joyqueue.client.internal.Plugins;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MessageConvertSupport
 *
 * author: gaohaoxiang
 * date: 2019/4/3
 */
public class MessageConvertSupport {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Byte /** type **/, MessageConverter> converterMap;

    public MessageConvertSupport() {
        this.converterMap = loadConverters();
    }

    protected Map<Byte, MessageConverter> loadConverters() {
        Map<Byte, MessageConverter> result = Maps.newHashMap();
        Iterable<MessageConverter> iterable = Plugins.MESSAGE_CONVERTER.extensions();
        for (MessageConverter messageConverter : iterable) {
            result.put(messageConverter.type(), messageConverter);
        }
        return result;
    }

    public List<BrokerMessage> convert(List<BrokerMessage> messages) {
        List<BrokerMessage> result = Lists.newLinkedList();
        for (BrokerMessage message : messages) {
            if (message.isBatch()) {
                List<BrokerMessage> convertedMessages = convertBatch(message);
                if (convertedMessages != null) {
                    result.addAll(convertedMessages);
                } else {
                    result.add(message);
                }
            } else {
                BrokerMessage convertedMessage = convert(message);
                if (convertedMessage != null) {
                    result.add(convertedMessage);
                } else {
                    result.add(message);
                }
            }
        }
        return result;
    }

    public BrokerMessage convert(BrokerMessage message) {
        MessageConverter messageConverter = converterMap.get(message.getSource());
        if (messageConverter == null) {
            if (message.getSource() != SourceType.JOYQUEUE.getValue() && message.getSource() != SourceType.JOYQUEUE0.getValue()) {
                logger.warn("message converter not found, source: {}, current: {}", message.getSource(), converterMap);
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("convert message, converter: {}", message.getSource(), messageConverter);
        }
        return messageConverter.convert(message);
    }

    public List<BrokerMessage> convertBatch(BrokerMessage message) {
        MessageConverter messageConverter = converterMap.get(message.getSource());
        if (messageConverter == null) {
            if (message.getSource() != SourceType.JOYQUEUE.getValue() && message.getSource() != SourceType.JOYQUEUE0.getValue()) {
                logger.warn("message converter not found, source: {}, current: {}", message.getSource(), converterMap);
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("convert message, converter: {}", message.getSource(), messageConverter);
        }
        return messageConverter.convertBatch(message);
    }
}