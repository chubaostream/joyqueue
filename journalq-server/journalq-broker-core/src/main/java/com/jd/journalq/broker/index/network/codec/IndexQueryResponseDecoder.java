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
package com.jd.journalq.broker.index.network.codec;

import com.jd.journalq.broker.index.command.ConsumeIndexQueryResponse;
import com.jd.journalq.broker.index.model.IndexMetadataAndError;
import com.jd.journalq.network.transport.codec.JMQHeader;
import com.jd.journalq.network.transport.codec.PayloadDecoder;
import com.jd.journalq.network.command.CommandType;
import com.jd.journalq.network.serializer.Serializer;
import com.jd.journalq.network.transport.command.Type;

import io.netty.buffer.ByteBuf;
import org.apache.commons.collections.map.HashedMap;

import java.util.Map;

/**
 * Created by zhuduohui on 2018/9/7.
 */
public class IndexQueryResponseDecoder implements PayloadDecoder<JMQHeader>, Type {

    @Override
    public Object decode(final JMQHeader header, final ByteBuf buffer) throws Exception {
        Map<String, Map<Integer, IndexMetadataAndError>> topicPartitionIndex = new HashedMap();
        int topics = buffer.readInt();
        for (int i = 0; i < topics; i++) {
            String topic = Serializer.readString(buffer, Serializer.SHORT_SIZE);
            int partitions = buffer.readInt();
            Map<Integer, IndexMetadataAndError> partitionIndexs = new HashedMap();
            for (int j = 0; j < partitions; j++) {
                int partition = buffer.readInt();
                long index = buffer.readLong();
                String metadata = Serializer.readString(buffer, Serializer.SHORT_SIZE);
                short error = buffer.readShort();
                partitionIndexs.put(partition, new IndexMetadataAndError(index, metadata, error));
            }
            topicPartitionIndex.put(topic, partitionIndexs);
        }
        return new ConsumeIndexQueryResponse(topicPartitionIndex);
    }

    @Override
    public int type() {
        return CommandType.CONSUME_INDEX_QUERY_RESPONSE;
    }
}
