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
package com.jd.journalq.client.internal.consumer.converter;

import com.google.common.collect.Lists;
import com.jd.journalq.client.internal.consumer.domain.ConsumeMessage;
import com.jd.journalq.client.internal.consumer.domain.ConsumeReply;
import com.jd.journalq.network.command.RetryType;

import java.util.List;

/**
 * ConsumeMessageConverter
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/1/11
 */
public class ConsumeMessageConverter {

    public static List<ConsumeReply> convertToReply(List<ConsumeMessage> messages, RetryType retryType) {
        List<ConsumeReply> result = Lists.newArrayListWithCapacity(messages.size());
        for (ConsumeMessage message : messages) {
            result.add(new ConsumeReply(message.getPartition(), message.getIndex(), retryType));
        }
        return result;
    }
}