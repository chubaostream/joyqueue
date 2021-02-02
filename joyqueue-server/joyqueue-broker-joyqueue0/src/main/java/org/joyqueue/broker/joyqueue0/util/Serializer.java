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
package org.joyqueue.broker.joyqueue0.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.joyqueue.broker.joyqueue0.command.RetryMessage;
import io.netty.buffer.ByteBuf;
import org.joyqueue.domain.Partition;
import org.joyqueue.message.BrokerMessage;
import org.joyqueue.message.Message;
import org.joyqueue.message.MessageLocation;
import org.joyqueue.toolkit.io.ZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 序列化工具类
 */
public class Serializer {
    protected static Logger logger = LoggerFactory.getLogger(Serializer.class);
    public static final byte BYTE_SIZE = 1;
    public static final byte SHORT_SIZE = 2;
    public static final byte INT_SIZE = 4;
    public static final byte LONG_SIZE = 8;
    public static final byte STRING_SIZE = 9;
    public static final char[] hexDigit =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static final byte BYTE_TYPE = 1;
    public static final byte SHORT_TYPE = 2;
    public static final byte INT_TYPE = 4;
    public static final byte LONG_TYPE = 8;
    public static final byte STRING_TYPE = 9;
    /**
     * 读取字符串，字符长度<=255
     *
     * @param in 输入缓冲区
     * @return 字符串
     * @throws Exception
     */
    public static String readString(final ByteBuf in) throws Exception {
        return readString(in, 1, false);
    }

    /**
     * 读取字符串
     *
     * @param in         输入缓冲区
     * @param lengthSize 长度大小
     * @return 字符串
     * @throws Exception
     */
    public static String readString(final ByteBuf in, final int lengthSize) throws Exception {
        return readString(in, lengthSize, false);
    }

    /**
     * 读取字符串，前面有一个字符串长度字节
     *
     * @param in         输入缓冲区
     * @param lengthSize 长度大小
     * @param compressed 压缩标示
     * @return 字符串
     * @throws Exception
     */
    public static String readString(final ByteBuf in, final int lengthSize, final boolean compressed) throws Exception {
        int length = 0;
        if (lengthSize == 1) {
            length = in.readUnsignedByte();
        } else if (lengthSize == 2) {
            length = in.readUnsignedShort();
        } else {
            length = in.readInt();
        }
        return read(in, length, compressed, "UTF-8");
    }

    /**
     * 读取字符串
     *
     * @param in         输入缓冲区
     * @param length     长度
     * @param compressed 压缩
     * @param charset    字符集
     * @return 字符串
     * @throws Exception
     */
    public static String read(final ByteBuf in, final int length, final boolean compressed, String charset) throws
            Exception {
        if (length <= 0) {
            return null;
        }

        byte[] bytes = readBytes(in, length);
        try {
            if (compressed) {
                bytes = ZipUtil.decompressByZlib(bytes, 0, bytes.length);
            }

            if (charset == null || charset.isEmpty()) {
                charset = "UTF-8";
            }
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    /**
     * 读取字节数
     *
     * @param in     输入缓冲区
     * @param length 长度
     * @throws Exception
     */
    public static byte[] readBytes(final ByteBuf in, final int length) throws Exception {
        if (in == null || length <= 0) {
            return new byte[0];
        }
        int len = in.readableBytes();
        if (len == 0) {
            return new byte[0];
        }
        if (length < len) {
            len = length;
        }

        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        return bytes;
    }

    /**
     * 读取存储的消息
     *
     * @param in 输入缓冲区
     * @throws Exception
     */
    public static BrokerMessage readBrokerMessage(final ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }
        BrokerMessage message = new BrokerMessage();

        // 4个字节的消息长度
        message.setSize(in.readInt());
        // 2个字节的魔法标识
        in.readUnsignedShort();
        // 1个字节的系统字段 1-1：压缩标识 2-2：顺序消息 3-8：其他
        byte system = in.readByte();
        message.setCompressed((system & 0x1) == 1);
        // 消息顺序
        message.setOrdered((system >>> 1 & 0x1) == 1);
        // 其它 暂时保留
        // 2字节业务标签
        message.setFlag(in.readShort());
        // 1字节优先级
        message.setPriority(in.readByte());
        // TODO 8字节日志偏移
        in.skipBytes(8);
//        message.setJournalOffset(in.readLong());
        // 1字节队列
        message.setPartition(in.readUnsignedByte());
        // 8字节队列偏移
        message.setMsgIndexNo(in.readLong());
        // 6字节的客户端地址
        message.setClientIp(readBytes(in, 6));
        // TODO 6字节的服务端地址
        in.skipBytes(6);
//        message.setServerAddress(readBytes(in, 6));
        // 8字节发送时间
        long sendTime = in.readLong();
        message.setStartTime(sendTime);
        // 4字节接受时间（相对发送时间的偏移）
        message.setStartTime(sendTime + in.readInt());
        // 4字节存储时间（相对发送时间偏移）
        message.setStoreTime(in.readInt());
        // 8字节消息体CRC
        message.setBodyCRC(in.readLong());

        // 4字节消息体大小
        int length = in.readInt();
        if (length > 0) {
            // 消息体（字节数组）
            message.setBody(readByteBuffer(in, length));
        }
        // 1字节主题长度
        // 主题（字节数组）
        message.setTopic(readString(in));
        // 1字节应用长度
        // 应用（字节数组）
        message.setApp(readString(in));
        // 1字节业务ID长度
        // 业务ID（字节数组）
        message.setBusinessId(readString(in));
        // 2字节属性长度
        // 属性（字节数组）
        message.setAttributes(toMap(readString(in, 2)));

        return message;
    }

    /**
     * 读取存储消息
     *
     * @param in 输入缓冲区
     * @return 存储消息
     * @throws Exception
     */
    public static BrokerMessage readMessage(final ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }

        BrokerMessage message = new BrokerMessage();
        // 1字节系统字段 1-1:压缩标识 2-2:顺序消息 3-8:其它
        short sysCode = in.readUnsignedByte();
        message.setCompressed((sysCode & 0x1) == 1);
        message.setOrdered(((sysCode >> 1) & 0x1) == 1);
        message.setFlag(in.readShort());
        // 1字节优先级
        message.setPriority(in.readByte());
        // 4字节消息体CRC
        message.setBodyCRC(in.readLong());
        // 发送时间。
        message.setStartTime(in.readLong());

        // 4字节消息体大小
        // 消息体
        int length = in.readInt();
        if (length > 0) {
            // 消息体（字节数组）
            message.setBody(readByteBuffer(in, length));
        }

        // 1字节主题长度
        // 主题
        message.setTopic(readString(in, 1));
        // 1字节应用长度
        // 应用
        message.setApp(readString(in, 1));
        // 1字节业务ID长度
        // 业务ID
        message.setBusinessId(readString(in, 1));
        // 2字节属性长度
        // 属性 （以属性文件格式存储）
        message.setAttributes(toMap(readString(in, 2)));

        if (message.isCompressed()) {
            message.setCompressionType(Message.CompressionType.ZLIB);
        }
        return message;
    }

    /**
     * 读取存储消息数组
     *
     * @param in 输入缓冲区
     * @return 存储消息数组
     * @throws Exception
     */
    public static List<BrokerMessage> readMessages(final ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }
        // 2字节消息条数
        int count = in.readShort();
        if (count < 1) { //没有消息
            return null;
        }

        List<BrokerMessage> messages = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            messages.add(readMessage(in));
        }

        return messages;
    }

    /**
     * 写入存储消息
     *
     * @param messages 存储消息
     * @param out      输出缓冲区
     * @throws Exception
     */
    public static void writeMessage(List<BrokerMessage> messages, final ByteBuf out) throws Exception {
        if (out == null) {
            return;
        }

        int count = messages == null ? 0 : messages.size();
        out.writeShort(count);

        for (int i = 0; i < count; i++) {
            writeMessage(messages.get(i), out);
        }
    }

    /**
     * 写消息
     *
     * @param message 消息
     * @param out     输出缓冲区
     * @throws Exception
     */
    public static void writeMessage(final BrokerMessage message, final ByteBuf out) throws Exception {
        int size;
        if (out == null || message == null) {
            return;
        }
        // 记录写入的起始位置
        int begin = out.writerIndex();
        // 4个字节的消息长度需要计算出来
        out.writeInt(0);
        // 2个字节的魔法标识
        out.writeShort(BrokerMessage.MAGIC_CODE);
        // 1个字节的系统字段 1-1：压缩标识 2-2：顺序消息 3-8：其他,预留未用
        byte sysCode = (byte) (message.isCompressed() ? 1 : 0);
        sysCode |= ((message.isOrdered() ? 1 : 0) << 1) & 0x3;
        out.writeByte(sysCode);
        // 2字节业务标签
        out.writeShort(message.getFlag());
        // 1字节优先级
        out.writeByte(message.getPriority());
        // 8字节日志偏移
        out.writeLong(message.getMsgIndexNo());
        // 1字节队列
        //TODO 特殊处理一下
        if (message.getPartition() == Partition.RETRY_PARTITION_ID) {
            out.writeByte(RetryMessage.RETRY_PARTITION_ID);
        } else {
            out.writeByte(message.getPartition());
        }
        // TODO 8字节队列偏移
        out.writeLong(message.getMsgIndexNo());
        // TODO 6字节的客户端地址
//        out.writeBytes(message.getClientIp() == null ? new byte[6] : message.getClientIp());
        out.writeBytes(new byte[6]);
        // 6字节的服务端地址
        // out.writeBytes(new byte[6]);
        // 占用6字节的服务端地址存partition值（jmq2协议queueId字段一个字节不够存short类型的partition）
        out.writeBytes(new byte[4]);
        out.writeShort(message.getPartition());
        // 8字节发送时间
        out.writeLong(message.getStartTime());
        // TODO 4字节接受时间（相对发送时间的偏移）
        out.writeInt(message.getStoreTime());
        // 4字节存储时间（相对发送时间偏移）
        out.writeInt(message.getStoreTime());
        // 8字节消息体CRC
        out.writeLong(message.getBodyCRC());

        // 4字节消息体大小
        // 消息体（字节数组）
        write(message.getBody(), out);

        // 1字节主题长度
        // 主题（字节数组）
        write(message.getTopic(), out);
        // 1字节应用长度
        // 应用（字节数组）
        write(message.getApp(), out);
        // 1字节业务ID长度
        // 业务ID（字节数组）
        write(message.getBusinessId(), out);
        // 2字节属性长度
        // 属性（字节数组）
        write(toProperties(message.getAttributes()), out, 2);

        // 重写总长度
        int end = out.writerIndex();
        size = end - begin;
        message.setSize(size);
        out.writerIndex(begin);
        out.writeInt(size);
        out.writerIndex(end);
    }

    /**
     * 写入存储消息
     *
     * @param message 存储消息
     * @param out     输出缓冲区
     * @throws Exception
     */
    public static void write(final BrokerMessage message, final ByteBuf out) throws Exception {
        int size;
        if (out == null || message == null) {
            return;
        }
        // 记录写入的起始位置
        int begin = out.writerIndex();
        // 4个字节的消息长度需要计算出来
        out.writeInt(0);
        // 2个字节的魔法标识
        out.writeShort(BrokerMessage.MAGIC_CODE);
        // 1个字节的系统字段 1-1：压缩标识 2-2：顺序消息 3-8：其他,预留未用
        byte sysCode = (byte) (message.isCompressed() ? 1 : 0);
        sysCode |= ((message.isOrdered() ? 1 : 0) << 1) & 0x3;
        out.writeByte(sysCode);
        // 2字节业务标签
        out.writeShort(message.getFlag());
        // 1字节优先级
        out.writeByte(message.getPriority());
        // TODO 8字节日志偏移
//        out.writeLong(message.getJournalOffset());
        // 1字节队列
        out.writeByte(message.getPartition());
        // 8字节队列偏移
        out.writeLong(message.getMsgIndexNo());
        // 6字节的客户端地址
        out.writeBytes(message.getClientIp() == null ? new byte[6] : message.getClientIp());
        // 6字节的服务端地址
        // TODO 地址
//        out.writeBytes(message.getServerAddress() == null ? new byte[6] : message.getServerAddress());
        // 8字节发送时间
        out.writeLong(message.getStartTime());
        // 4字节接受时间（相对发送时间的偏移）
        out.writeInt((int) (message.
                getStartTime() - message.getStartTime()));
        // 4字节存储时间（相对发送时间偏移）
        out.writeInt((int) (message.
                getStoreTime() - message.getStartTime()));
        // 8字节消息体CRC
        out.writeLong(message.getBodyCRC());

        // 4字节消息体大小
        // 消息体（字节数组）
        write(message.getBody(), out);

        // 1字节主题长度
        // 主题（字节数组）
        write(message.getTopic(), out);
        // 1字节应用长度
        // 应用（字节数组）
        write(message.getApp(), out);
        // 1字节业务ID长度
        // 业务ID（字节数组）
        write(message.getBusinessId(), out);
        // 2字节属性长度
        // 属性（字节数组）
        write(toProperties(message.getAttributes()), out, 2);

        // 重写总长度
        int end = out.writerIndex();
        size = end - begin;
        message.setSize(size);
        out.writerIndex(begin);
        out.writeInt(size);
        out.writerIndex(end);
    }

    /**
     * 写入存储消息
     *
     * @param messages 存储消息
     * @param out      输出缓冲区
     * @throws Exception
     */
    public static void write(final BrokerMessage[] messages, final ByteBuf out) throws Exception {
        if (out == null) {
            return;
        }

        int count = messages == null ? 0 : messages.length;
        out.writeShort(count);

        for (int i = 0; i < count; i++) {
            write(messages[i], out);
        }
    }

    /**
     * 写整数
     *
     * @param value      整数
     * @param out        输出
     * @param lengthSize 长度字节数
     * @throws Exception
     */
    public static void write(final int value, final ByteBuf out, final int lengthSize) throws Exception {
        if (out == null) {
            return;
        }
        switch (lengthSize) {
            case BYTE_SIZE:
                out.writeByte(value);
                break;
            case SHORT_SIZE:
                out.writeShort(value);
                break;
            case INT_SIZE:
                out.writeInt(value);
                break;
        }
    }

    /**
     * 写长整数
     *
     * @param value      长整数
     * @param out        输出
     * @throws Exception
     */
    public static void write(final long value, final ByteBuf out) throws Exception {
        if (out == null) {
            return;
        }
        out.writeLong(value);
    }

    /**
     * 写字符串(长度<=255)
     *
     * @param value 字符串
     * @param out   输出缓冲区
     * @throws Exception
     */
    public static void write(final String value, final ByteBuf out) throws Exception {
        write(value, out, 1, false);
    }

    /**
     * 获取字节数组
     *
     * @param value   字符串
     * @param charset 字符集
     * @return 字节数组
     */
    protected static byte[] getBytes(final String value, final Charset charset) {
        if (value == null) {
            return new byte[0];
        }

        byte[] bytes;
        if (charset == null) {
            bytes = value.getBytes(Charsets.UTF_8);
        } else {
            bytes = value.getBytes(charset);
        }
        return bytes;
    }

    /**
     * 预测字符串占用的字节数
     *
     * @param value      值
     * @param lengthSize 长度字节数
     * @return 字节数
     */
    public static int getPredictionSize(final String value, final int lengthSize) {
        int size = lengthSize;
        if (value != null) {
            // 按照UNICODE计算字节数
            size += value.length() * 2;
        }
        return size;
    }

    /**
     * 预测字符串占用的字节数，字符串长度占用1位
     *
     * @param values 值
     * @return 字节数
     */
    public static int getPredictionSize(final String... values) {
        int size = 0;
        if (values != null) {
            for (String value : values) {
                size++;
                if (value != null) {
                    // 按照UNICODE计算字节数
                    size += value.length() * 2;
                }
            }
        }
        return size;
    }

    /**
     * 预测消息占用的字节数
     *
     * @param messages 消息数组
     * @return 字节数
     */
    public static int getPredictionSize(final BrokerMessage... messages) {
        int size = 2;
        if (messages != null) {
            for (BrokerMessage message : messages) {
                size += 4 + 2 + 1 + 2 + 1 + 8 + 1 + 8 + 6 + 6 + 8 + 4 + 4 + 8;
                size += getPredictionSize(message.getBody(), true);
                size += getPredictionSize(message.getTopic(), message.getApp(), message.getBusinessId());
                size += 2;
                if (message.getAttributes() != null && !message.getAttributes().isEmpty()) {
                    size += 100;
                }
            }
        }

        return size;
    }

    /**
     * 预测消息占用的字节数
     *
     * @param messages 消息数组
     * @return 字节数
     */
    public static int getPredictionSize(final Message... messages) {
        int size = 2;
        if (messages != null) {
            for (Message message : messages) {
                size += 1 + 2 + 1 + 8 + 8;
                size += getPredictionSize(message.getBody(), true);
                size += Serializer.getPredictionSize(message.getTopic(), message.getApp(), message.getBusinessId());
                size += 2;
                if (message.getAttributes() != null && !message.getAttributes().isEmpty()) {
                    size += 100;
                }
            }
        }

        return size;
    }

    /**
     * 预测缓冲区占用的字节数
     *
     * @param buffer      缓冲区
     * @param writeLength 写长度标示
     * @return 字节数
     */
    public static int getPredictionSize(final ByteBuffer buffer, final boolean writeLength) {
        int size = 0;
        if (writeLength) {
            size += 4;
        }
        if (buffer != null) {
            size += buffer.remaining();
        }
        return size;
    }

    /**
     * 预测消息位置占用的字节数
     *
     * @param locations 消息位置数组
     * @return 字节数
     */
    public static int getPredictionSize(final MessageLocation... locations) {
        String topic = locations[0].getTopic();
        return 6 + getPredictionSize(topic) + 2 + locations.length * 9;
    }

    /**
     * 预测map对象占用的字节数
     *
     * @param hashMap
     * @return
     */
    public static int getPredictionSize(final Map<Object, Object> hashMap) {
        int count = 0;
        int size;
        if (hashMap == null) {
            return count;
        }
        size = hashMap.size();
        if (0 == size) {
            count = 4;
            return count;
        }
        count += 4;
        Iterator iterator = hashMap.entrySet().iterator();
        Map.Entry entry;
        while (iterator.hasNext()){
            entry = (Map.Entry)iterator.next();
            try {
                count += getMapObjectSize(entry.getKey());
                count += getMapObjectSize(entry.getValue());
            }catch (Exception e){
                logger.error("getPredictionSize exception, may type error", e);
            }
        }
        return count;
    }

    /**
     * 预测Map对应的Object占用的字节数
     *
     * @param object
     * @return
     * @throws Exception
     */
    public static int getMapObjectSize(Object object) throws Exception{
        if (object instanceof Byte) {
            return 2;
        } else if (object instanceof Short) {
            return 3;
        } else if (object instanceof Integer) {
            return 5;
        } else if (object instanceof Long) {
            return 9;
        } else if (object instanceof String) {
            return 1 + getPredictionSize((String)object);
        } else {
            throw new Exception();
        }
    }

    /**
     * 写字符串
     *
     * @param value      字符串
     * @param out        输出缓冲区
     * @param lengthSize 长度字节数
     * @throws Exception
     */
    public static void write(final String value, final ByteBuf out, final int lengthSize) throws Exception {
        write(value, out, lengthSize, false);
    }

    /**
     * 写字符串
     *
     * @param value      字符串
     * @param out        输出缓冲区
     * @param lengthSize 长度字节数
     * @param compressed 是否进行压缩
     * @throws Exception
     */
    public static void write(final String value, final ByteBuf out, final int lengthSize,
                             final boolean compressed) throws Exception {
        if (out == null) {
            return;
        }
        if (value != null && !value.isEmpty()) {
            byte[] bytes = getBytes(value, Charsets.UTF_8);
            if (compressed) {
                bytes = ZipUtil.compressByZlib(bytes, 0, bytes.length);
            }
            write(bytes.length, out, lengthSize);
            out.writeBytes(bytes);
        } else {
            write(0, out, lengthSize);
        }
    }

    /**
     * 写入字符串
     *
     * @param value   字符串
     * @param out     输出缓冲区
     * @param charset 字符集
     * @throws Exception
     */
    public static void write(final String value, final ByteBuf out, final Charset charset) throws Exception {
        if (out == null || value == null) {
            return;
        }
        out.writeBytes(getBytes(value, charset));
    }


    /**
     * 写数据
     *
     * @param value 数据源
     * @param out   输出缓冲区
     * @throws Exception
     */
    public static void write(final ByteBuffer value, final ByteBuf out) throws Exception {
        write(value, out, true);
    }

    /**
     * 写数据
     *
     * @param value       数据源
     * @param out         输出缓冲区
     * @param writeLength 是否写长度
     * @throws Exception
     */
    public static void write(final ByteBuffer value, final ByteBuf out, final boolean writeLength) throws Exception {
        int length = value == null ? 0 : value.remaining();
        if (writeLength) {
            out.writeInt(length);
        }
        if (length > 0) {
            if (value.hasArray()) {
                out.writeBytes(value.array(), value.arrayOffset() + value.position(), value.remaining());
            } else {
                out.writeBytes(value.slice());
            }
        }
    }

    /**
     * 读数据
     *
     * @param in 输入缓冲区
     * @return 缓冲区
     * @throws Exception
     */
    public static ByteBuffer readByteBuffer(final ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }

        int length = in.readInt();
        if (length <= 0) {
            return null;
        }

        return readByteBuffer(in, length);
    }

    /**
     * 读数据
     *
     * @param in     输入缓冲区
     * @param length 读取长度
     * @return 缓冲区
     * @throws Exception
     */
    public static ByteBuffer readByteBuffer(final ByteBuf in, int length) throws Exception {
        if (in == null || length <= 0) {
            return null;
        }

        ByteBuffer buffer;

        if (in.isDirect()) {
            buffer = ByteBuffer.allocate(length);
            in.readBytes(buffer);
            buffer.flip();
        } else {
            int index = in.readerIndex();
            buffer = in.slice(index, length).nioBuffer();
            in.readerIndex(index + length);
        }

        return buffer;
    }

    /**
     * 把Properties字符串转换成Map
     *
     * @param text 字符串
     * @return 散列对象
     * @throws IOException
     */
    protected static Map<String, String> toMap(final String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Properties properties = new Properties();
        properties.load(new StringReader(text));

        return (Map<String, String>) new HashMap(properties);
    }

    /**
     * 把Map转换成Properties字符串
     *
     * @param attributes 散列
     * @return 字符串
     */
    protected static String toProperties(final Map<String, String> attributes) {
        if (attributes == null) {
            return "";
        }
        int count = 0;
        StringBuilder builder = new StringBuilder(100);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (count > 0) {
                builder.append('\n');
            }
            append(builder, entry.getKey(), true, true);
            builder.append('=');
            append(builder, entry.getValue(), false, true);
            count++;
        }
        return builder.toString();
    }

    /**
     * 添加字符串
     *
     * @param builder       缓冲区
     * @param value         字符串
     * @param escapeSpace   转移空格标示
     * @param escapeUnicode 转移Unicode标示
     */
    private static void append(final StringBuilder builder, final String value, final boolean escapeSpace,
                               final boolean escapeUnicode) {
        int len = value.length();
        for (int x = 0; x < len; x++) {
            char aChar = value.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    builder.append('\\');
                    builder.append('\\');
                    continue;
                }
                builder.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace) {
                        builder.append('\\');
                    }
                    builder.append(' ');
                    break;
                case '\t':
                    builder.append('\\');
                    builder.append('t');
                    break;
                case '\n':
                    builder.append('\\');
                    builder.append('n');
                    break;
                case '\r':
                    builder.append('\\');
                    builder.append('r');
                    break;
                case '\f':
                    builder.append('\\');
                    builder.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    builder.append('\\');
                    builder.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                        builder.append('\\');
                        builder.append('u');
                        builder.append(hexDigit[((aChar >> 12) & 0xF)]);
                        builder.append(hexDigit[((aChar >> 8) & 0xF)]);
                        builder.append(hexDigit[((aChar >> 4) & 0xF)]);
                        builder.append(hexDigit[(aChar & 0xF)]);
                    } else {
                        builder.append(aChar);
                    }
            }
        }
    }

    /**
     * 读取Map数据
     *
     * @param in
     * @return
     * @throws Exception
     */
    public static Map<Object, Object> readMap(final ByteBuf in) {
        int size = in.readInt();
        byte type;
        Object key;
        Object value;
        Map<Object, Object> hashMap = new HashMap<Object, Object>();
        if (0 == size) {
            return hashMap;
        }else {
            for (int i = 0; i < size; i++) {
                type = in.readByte();
                key = readObject(type, in);
                type = in.readByte();
                value = readObject(type, in);
                if (key != null && value != null) {
                    hashMap.put(key, value);
                }
            }

        }
        return hashMap;
    }

    /**
     * 根据类型读取数据
     *
     * @param type
     * @param in
     * @return
     */
    public static Object readObject(byte type, ByteBuf in) {
        Object value = null;
        Object mRetObject;
        try {
            switch (type){
                case BYTE_TYPE:
                    value = in.readByte();
                    break;
                case SHORT_TYPE:
                    value = in.readShort();
                    break;
                case INT_TYPE:
                    value = in.readInt();
                    break;
                case LONG_TYPE:
                    value = in.readLong();
                    break;
                case STRING_TYPE:
                    value = readString(in);
            }
        }catch (Exception e) {
            logger.error("read exception", e);
        }
        mRetObject = value;
        return mRetObject;
    }
}