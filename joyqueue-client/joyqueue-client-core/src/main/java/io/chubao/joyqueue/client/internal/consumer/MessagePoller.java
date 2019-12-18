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
package io.chubao.joyqueue.client.internal.consumer;

import io.chubao.joyqueue.client.internal.consumer.domain.ConsumeMessage;
import io.chubao.joyqueue.client.internal.consumer.domain.ConsumeReply;
import io.chubao.joyqueue.client.internal.consumer.domain.FetchIndexData;
import io.chubao.joyqueue.client.internal.metadata.domain.TopicMetadata;
import io.chubao.joyqueue.exception.JoyQueueCode;
import io.chubao.joyqueue.toolkit.lang.LifeCycle;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MessagePoller
 *
 * author: gaohaoxiang
 * date: 2018/12/11
 */
public interface MessagePoller extends LifeCycle {

    // poll
    ConsumeMessage pollOnce(String topic);

    ConsumeMessage pollOnce(String topic, long timeout, TimeUnit timeoutUnit);

    List<ConsumeMessage> poll(String topic);

    List<ConsumeMessage> poll(String topic, long timeout, TimeUnit timeoutUnit);

    CompletableFuture<List<ConsumeMessage>> pollAsync(String topic);

    CompletableFuture<List<ConsumeMessage>> pollAsync(String topic, long timeout, TimeUnit timeoutUnit);

    // poll partition
    ConsumeMessage pollPartitionOnce(String topic, short partition);

    ConsumeMessage pollPartitionOnce(String topic, short partition, long timeout, TimeUnit timeoutUnit);

    ConsumeMessage pollPartitionOnce(String topic, short partition, long index);

    ConsumeMessage pollPartitionOnce(String topic, short partition, long index, long timeout, TimeUnit timeoutUnit);

    List<ConsumeMessage> pollPartition(String topic, short partition);

    List<ConsumeMessage> pollPartition(String topic, short partition, long timeout, TimeUnit timeoutUnit);

    List<ConsumeMessage> pollPartition(String topic, short partition, long index);

    List<ConsumeMessage> pollPartition(String topic, short partition, long index, long timeout, TimeUnit timeoutUnit);

    // async
    CompletableFuture<List<ConsumeMessage>> pollPartitionAsync(String topic, short partition);

    CompletableFuture<List<ConsumeMessage>> pollPartitionAsync(String topic, short partition, long timeout, TimeUnit timeoutUnit);

    CompletableFuture<List<ConsumeMessage>> pollPartitionAsync(String topic, short partition, long index);

    CompletableFuture<List<ConsumeMessage>> pollPartitionAsync(String topic, short partition, long index, long timeout, TimeUnit timeoutUnit);

    // reply
    JoyQueueCode reply(String topic, List<ConsumeReply> replyList);

    JoyQueueCode replyOnce(String topic, ConsumeReply reply);

    // index
    FetchIndexData fetchIndex(String topic, short partition);

    // metadata
    TopicMetadata getTopicMetadata(String topic);
}