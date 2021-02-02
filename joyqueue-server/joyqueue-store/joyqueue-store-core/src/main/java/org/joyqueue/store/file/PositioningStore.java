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
package org.joyqueue.store.file;

import org.joyqueue.store.PositionOverflowException;
import org.joyqueue.store.PositionUnderflowException;
import org.joyqueue.store.WriteException;
import org.joyqueue.store.utils.PreloadBufferPool;
import org.joyqueue.toolkit.format.Format;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 带缓存的、高性能、多文件、基于位置的、Append Only的日志存储存储。
 *
 * @author liyue25
 * Date: 2018/8/14
 */
public class PositioningStore<T /* 保存的数据类型 */> implements Closeable {
    private final Logger logger = LoggerFactory.getLogger(PositioningStore.class);
    private final int fileHeaderSize; // 文件头长度
    private final int fileDataSize; // 文件数据部分最大长度
    // 判断磁盘满的比率，不能等到磁盘真正写满的时候才拒绝写入，否则来不及刷盘的数据将无法写入磁盘，导致丢数据。
    private final int diskFullRatio;
    private final int maxMessageLength; // 最大消息大小
    private final boolean loadOnRead; // 第一次读取文件内容时，是否将整个文件都加载到内存中。
    private final boolean flushForce; // 是否调用fsync刷盘
    private final File base; // 数据存储目录
    private final LogSerializer<T> serializer; // 数据序列化器
    private final PreloadBufferPool bufferPool; // 缓存页管理器
    // 所有数据文件跳表
    private final NavigableMap<Long /* 文件起始位置 */, StoreFile<T>> storeFileMap = new ConcurrentSkipListMap<>();

    private final AtomicLong flushPosition = new AtomicLong(0L); // 刷盘位置
    private final AtomicLong rightPosition = new AtomicLong(0L); // 最大位置
    private final AtomicLong leftPosition = new AtomicLong(0L); // 最小位置

    private final Lock writeLock = new ReentrantLock(); // 写入锁
    private final Lock flushLock = new ReentrantLock(); // 刷盘锁
    private final ReentrantLock deleteLock = new ReentrantLock(); // 删除锁

    // 正在写入的文件
    private StoreFile<T> writeStoreFile = null;

    public PositioningStore(File base, Config config, PreloadBufferPool bufferPool, LogSerializer<T> serializer) {
        this.base = base;
        this.fileHeaderSize = config.fileHeaderSize;
        this.fileDataSize = config.fileDataSize;
        this.maxMessageLength = config.maxMessageLength;
        this.loadOnRead = config.loadOnRead;
        this.flushForce = config.flushForce;
        if(config.diskFullRatio <= 0 || config.diskFullRatio > 100) {
            logger.warn("Invalid config diskFullRatio: {}, using default: {}.", config.diskFullRatio, Config.DEFAULT_DISK_FULL_RATIO);
            diskFullRatio = Config.DEFAULT_DISK_FULL_RATIO;
        } else {
            diskFullRatio = config.diskFullRatio;
        }
        this.bufferPool = bufferPool;
        this.serializer = serializer;
    }

    public long left() {
        return leftPosition.get();
    }

    public long right() {
        return rightPosition.get();
    }

    public long flushPosition() {
        return flushPosition.get();
    }

    public void clear(long position) {
        logger.info("Clear store, new position: {}, store: {}...",
                Format.formatWithComma(position),
                base.getAbsolutePath());

        try {
            // 注意锁的顺序必须一致，避免死锁。
            flushLock.lock();
            writeLock.lock();
            deleteLock.lock();
            clear();
            this.leftPosition.set(position);
            this.rightPosition.set(position);
            this.flushPosition.set(position);
            resetWriteStoreFile();
        } finally {
            deleteLock.unlock();
            writeLock.unlock();
            flushLock.unlock();
        }
    }

    /**
     * 将位置回滚到position
     * 与如下操作不能并发：
     * flush()
     * append()
     * physicalDeleteTo()
     */
    public void setRight(long position) throws IOException {

        if (position == right()) return;
        logger.info("Rollback to position: {}, left: {}, right: {}, flushPosition: {}, store: {}...",
                Format.formatWithComma(position),
                Format.formatWithComma(left()),
                Format.formatWithComma(right()),
                Format.formatWithComma(flushPosition()),
                base.getAbsolutePath());

        try {
            // 注意锁的顺序必须一致，避免死锁。
            flushLock.lock();
            writeLock.lock();
            deleteLock.lock();
            if (position <= left() || position > right()) {
                clear();
                this.leftPosition.set(position);
                this.rightPosition.set(position);
                this.flushPosition.set(position);
            } else if (position < right()) {
                rollbackFiles(position);
                this.rightPosition.set(position);
                if (this.flushPosition() > position) this.flushPosition.set(position);
            }
            resetWriteStoreFile();
        } finally {
            deleteLock.unlock();
            writeLock.unlock();
            flushLock.unlock();
            logger.info("Rollback finished, left: {}, right: {}, flushPosition: {}, store: {}.",
                    Format.formatWithComma(left()),
                    Format.formatWithComma(right()),
                    Format.formatWithComma(flushPosition()),
                    base.getAbsolutePath());
        }
    }

    private void clear() {
        try {
            while (!storeFileMap.isEmpty()) {
                StoreFile<T> storeFile = storeFileMap.remove(storeFileMap.firstKey());
                forceDeleteStoreFile(storeFile);
                if(storeFile == writeStoreFile) {
                    writeStoreFile = null;
                }
            }
        } catch (IOException e) {
            throw new RollBackException(e);
        }
    }

    /**
     * 删除指定位置position之后的所有数据。
     * position所在的文件，从position开始，截断后面的数据；
     * position之后的文件，直接删除。
     * @param position 指定的删除位置。
     * @throws IOException 发生IO异常时抛出。
     */
    private void rollbackFiles(long position) throws IOException {

        if (!storeFileMap.isEmpty()) {
            // position 所在的Page需要截断至position
            Map.Entry<Long, StoreFile<T>> entry = storeFileMap.floorEntry(position);
            StoreFile<T> storeFile = entry.getValue();
            if (position > storeFile.position()) {
                int relPos = (int) (position - storeFile.position());
                logger.info("Truncate store file {} to relative position {}.", storeFile.file().getAbsolutePath(), relPos);
                storeFile.rollback(relPos);
            }

            SortedMap<Long, StoreFile<T>> toBeRemoved = storeFileMap.tailMap(position);
            while (!toBeRemoved.isEmpty()) {
                StoreFile<T> removedStoreFile = toBeRemoved.remove(toBeRemoved.firstKey());
                forceDeleteStoreFile(removedStoreFile);
                if(writeStoreFile == removedStoreFile) {
                    writeStoreFile = null;
                }
            }
        }


    }


    private void resetWriteStoreFile() {
        if (!storeFileMap.isEmpty()) {
            StoreFile<T> storeFile = storeFileMap.lastEntry().getValue();
            if (storeFile.position() + storeFile.capacity() > right()) {
                writeStoreFile = storeFile;
            } else {
                writeStoreFile = null;
            }
        }
    }

    public void recover() throws IOException {
        logger.info("Recovering positioning store: {}...", base.getAbsolutePath());
        try {
            // 注意锁的顺序必须一致，避免死锁。
            flushLock.lock();
            writeLock.lock();
            deleteLock.lock();

            // 恢复文件列表
            recoverFileMap();

            // 恢复相关位置
            long recoverPosition = this.storeFileMap.isEmpty() ? 0L : this.storeFileMap.lastKey() + this.storeFileMap.lastEntry().getValue().fileDataSize();
            flushPosition.set(recoverPosition);
            rightPosition.set(recoverPosition);
            leftPosition.set(this.storeFileMap.isEmpty() ? 0L : this.storeFileMap.firstKey());

            // 删除末尾可能存在的半条数据
            if (recoverPosition > 0) {
                // 从最大位置 - 1开始，向前找，找到第一条完整的数据，
                // 返回这条数据之后的下一条数据（下一条数据是否存在都没关系）的起始位置。
                long lastLogTail = toLogTail(recoverPosition - 1);
                if (lastLogTail < 0) {
                    throw new CorruptedLogException(String.format("Unable to read any valid log. Corrupted log files: %s.", base.getAbsolutePath()));
                }
                // 如果存在不完整的数据，删除之。
                if (lastLogTail < recoverPosition) {
                    rollbackFiles(lastLogTail);
                    flushPosition.set(lastLogTail);
                    rightPosition.set(lastLogTail);

                }
            }
            // 重置当前写入的文件
            resetWriteStoreFile();
        } finally {
            deleteLock.unlock();
            writeLock.unlock();
            flushLock.unlock();
        }
        logger.info("Store recovered, leftPosition: {}, rightPosition: {}, flushPosition: {},  base: {}.",
                Format.formatWithComma(left()),
                Format.formatWithComma(right()),
                Format.formatWithComma(flushPosition()),
                base.getAbsolutePath());
    }

    /**
     * 从base目录中恢复文件列表。
     *
     * @throws IOException 发生IO异常时抛出
     */
    private void recoverFileMap() throws IOException {
        File[] files = base.listFiles(file -> file.isFile() && file.getName().matches("\\d+"));
        long filePosition;
        if (null != files) {
            for (File file : files) {
                filePosition = Long.parseLong(file.getName());
                storeFileMap.put(filePosition, new StoreFileImpl<>(filePosition, base, fileHeaderSize, serializer, bufferPool, fileDataSize, loadOnRead, flushForce));
            }
        }
        // 当服务器断电时，在存储的末尾，有可能会存在没来得及刷盘的空文件，需要删掉。

        while (!storeFileMap.isEmpty() && storeFileMap.lastEntry().getValue().file().length() <= fileHeaderSize) {
            Map.Entry<Long, StoreFile<T>> lastEntry = storeFileMap.pollLastEntry();
            StoreFile<T> storeFile = lastEntry.getValue();
            File file = storeFile.file();
            if (file.exists()) {
                if (file.delete()) {
                    logger.info("Store file deleted: {}.",
                            file.getAbsolutePath());
                } else {
                    throw new IOException(String.format("Delete file %s failed!", file.getAbsolutePath()));
                }
            }
        }

        // 检查文件是否连续完整
        if (!storeFileMap.isEmpty()) {
            long position = storeFileMap.firstKey();
            for (Map.Entry<Long, StoreFile<T>> fileEntry : storeFileMap.entrySet()) {
                if (position != fileEntry.getKey()) {
                    throw new CorruptedLogException(String.format("Files are not continuous! expect: %d, actual file name: %d, store: %s.", position, fileEntry.getKey(), base.getAbsolutePath()));
                    // TODO: 考虑自动删除store尾部不连续的文件，以解决掉电后需要手动恢复存储的问题。
                }
                position += fileEntry.getValue().file().length() - fileHeaderSize;
            }
        }
    }

    private long toLogTail(long position) {
        T t = null;
        long seekEndPosition = Math.max(position - 2 * maxMessageLength, left());
        while (position >= seekEndPosition) {
            try {
                t = tryRead(position--);
            } catch (Throwable ignored) {
            }
            if (null != t) return position + 1 + serializer.size(t);
        }
        return -1L;
    }

    public long toLogStart(long position) {

        T t = null;
        long seekEndPosition = Math.max(position - 2 * maxMessageLength, left());
        while (position >= seekEndPosition) {
            try {
                t = tryRead(position--);
            } catch (Throwable ignored) {
            }
            if (null != t) return position + 1;
        }
        return -1L;
    }

    public long appendByteBuffer(ByteBuffer byteBuffer) throws IOException {
        try {
            writeLock.lock();
            // 处理更换文件的情况
            if (null == writeStoreFile) writeStoreFile = createStoreFile(right());
            if (writeStoreFile.capacity() - writeStoreFile.writePosition() < byteBuffer.remaining()) {
                writeStoreFile.closeWrite();
                writeStoreFile = createStoreFile(right());
            }
            // 将数据写入文件
            return rightPosition.addAndGet(writeStoreFile.appendByteBuffer(byteBuffer));
        } finally {
            writeLock.unlock();
        }

    }

    public long append(T t) throws IOException {
        try {
            writeLock.lock();
            // 处理更换文件的情况
            if (null == writeStoreFile) writeStoreFile = createStoreFile(right());
            if (writeStoreFile.capacity() - writeStoreFile.writePosition() < serializer.size(t)) {
                writeStoreFile.closeWrite();
                writeStoreFile = createStoreFile(right());
            }
            // 将数据写入文件
            return rightPosition.addAndGet(writeStoreFile.append(t));
        } finally {
            writeLock.unlock();
        }
    }

    public long append(final List<T> ts) throws IOException {
        try {
            writeLock.lock();
            if (null == ts || ts.isEmpty()) throw new WriteException("Parameter list is empty!");
            long position = 0;
            for (T t : ts) {
                position = append(t);
            }
            return position;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 将内存中的数据写入到磁盘上，每次最多刷盘一个文件。
     * @return 是否有数据被写入磁盘中
     * @throws IOException 发生IO异常时抛出
     */
    public boolean flush() throws IOException {
        if (flushPosition() < right()) {
            try {
                flushLock.lock();
                if (flushPosition() < left()) {
                    flushPosition.set(left());
                }
                Map.Entry<Long, StoreFile<T>> entry = storeFileMap.floorEntry(flushPosition());
                if (null == entry) {
                    if (storeFileMap.isEmpty()) {
                        flushPosition.set(right());
                    } else {
                        flushPosition.set(storeFileMap.firstKey());
                    }
                    return true;
                }
                StoreFile<T> storeFile = entry.getValue();
                if (!storeFile.isClean()) {
                    // 在文件第一次刷盘之前，需要把上一个文件fsync到磁盘上，避免服务器宕机导致文件不连续
                    if (flushForce && storeFile.flushPosition() == 0) {
                        Map.Entry<Long, StoreFile<T>> prevEntry = storeFileMap.floorEntry(entry.getKey() - 1);
                        if(null != prevEntry) {
                            prevEntry.getValue().force();
                        }
                    }
                    storeFile.flush();
                }
                if (flushPosition() < storeFile.position() + storeFile.flushPosition()) {
                    flushPosition.set(storeFile.position() + storeFile.flushPosition());
                }
                return true;
            } finally {
                flushLock.unlock();
            }
        }
        return false;
    }

    private StoreFile<T> createStoreFile(long position) {
        StoreFile<T> storeFile = new StoreFileImpl<>(position, base, fileHeaderSize, serializer, bufferPool, fileDataSize, loadOnRead, flushForce);
        StoreFile<T> present;
        if ((present = storeFileMap.putIfAbsent(position, storeFile)) != null) {
            storeFile = present;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Store file created, leftPosition: {}, rightPosition: {}, flushPosition: {}, base: {}.",
                    Format.formatWithComma(left()),
                    Format.formatWithComma(right()),
                    Format.formatWithComma(flushPosition()),
                    base.getAbsolutePath()
            );
        }
        return storeFile;
    }

    public boolean isDiskFull() {
//        return (base.getTotalSpace() - base.getFreeSpace()) * 100 >  base.getTotalSpace() * diskFullRatio;
        return false;
    }

    public T read(long position) throws IOException {
        checkReadPosition(position);
        try {
            return tryRead(position);
        } catch (Throwable t) {
            logger.warn("Exception on read position {} of store {}.", position, base.getAbsolutePath(), t);
            throw t;
        }
    }

    public T read(long position, int length) throws IOException {
        checkReadPosition(position);
        try {
            StoreFile<T> storeFile = storeFileMap.floorEntry(position).getValue();
            int relPosition = (int) (position - storeFile.position());
            return storeFile.read(relPosition, length);
        } catch (Throwable t) {
            logger.warn("Exception on readByteBuffer position {} of store {}, " +
                            "leftPosition: {}, rightPosition: {}, flushPosition: {}.",
                    position, base.getAbsolutePath(),
                    Format.formatWithComma(left()),
                    Format.formatWithComma(right()),
                    Format.formatWithComma(flushPosition()),
                    t);
            throw t;
        }
    }

    private T tryRead(long position) throws IOException {

        checkReadPosition(position);
        StoreFile<T> storeFile = storeFileMap.floorEntry(position).getValue();
        int relPosition = (int) (position - storeFile.position());
        return storeFile.read(relPosition, -1);
    }

    public List<T> batchRead(long position, int count) throws IOException {
        checkReadPosition(position);
        List<T> list = new ArrayList<>(count);
        long pointer = position;

        StoreFile<T> storeFile = null;
        try {
            while (list.size() < count && pointer < right()) {

                if (null == storeFile || storeFile.writePosition() + storeFile.position() <= pointer) {
                    storeFile = storeFileMap.floorEntry(pointer).getValue();
                }

                int relPosition = (int) (pointer - storeFile.position());
                T t = storeFile.read(relPosition, -1);
                list.add(t);
                pointer += serializer.size(t);

            }

            return list;
        } catch (Throwable t) {
            logger.warn("Exception on batchRead position {} of store {}, " +
                    "leftPosition: {}, rightPosition: {}, flushPosition: {}.",
                    pointer, base.getAbsolutePath(),
                    Format.formatWithComma(left()),
                    Format.formatWithComma(right()),
                    Format.formatWithComma(flushPosition()),
                    t);
            throw t;
        }

    }

    public ByteBuffer readByteBuffer(long position, int length) throws IOException {
        checkReadPosition(position);
        try {
            StoreFile<T> storeFile = storeFileMap.floorEntry(position).getValue();
            int relPosition = (int) (position - storeFile.position());
            ByteBuffer byteBuffer = storeFile.readByteBuffer(relPosition, length);
            byteBuffer.limit(byteBuffer.position() + serializer.trim(byteBuffer, length));
            return byteBuffer;
        } catch (Throwable t) {
            logger.warn("Exception on readByteBuffer position {} of store {}, " +
                            "leftPosition: {}, rightPosition: {}, flushPosition: {}.",
                    position, base.getAbsolutePath(),
                    Format.formatWithComma(left()),
                    Format.formatWithComma(right()),
                    Format.formatWithComma(flushPosition()),
                    t);
            throw t;
        }
    }


    private void checkReadPosition(long position) {
        long p;
        if ((p = left()) > position) {
            throw new PositionUnderflowException(position, p);
        } else if (position >= (p = right())) {
            throw new PositionOverflowException(position, p);
        }

    }

    public long physicalSize() {
        return right() - left();
    }

    /**
     * 删除 position之前的文件
     */
    public long physicalDeleteTo(long position) throws IOException {


        long deleteSize = 0L;
        Map.Entry<Long, StoreFile<T>> entry;
        long fileDataSize;
        StoreFile<T> storeFile;
        try {
            deleteLock.lock();
            while (storeFileMap.size() > 0 &&
                    (entry = storeFileMap.firstEntry()) != null &&
                    entry.getKey() +
                            (fileDataSize =
                                    (storeFile = entry.getValue()).hasPage() ?
                                            storeFile.writePosition() :
                                            storeFile.fileDataSize())
                            <= position) {

                if (storeFile == writeStoreFile) {
                    deleteLock.unlock();
                    try {
                        // 注意锁的顺序必须一致，避免死锁。
                        flushLock.lock();
                        writeLock.lock();
                        deleteLock.lock();
                        if (storeFileMap.remove(entry.getKey(), storeFile)) {
                            this.writeStoreFile = null;
                            forceDeleteStoreFile(storeFile);
                            deleteSize += fileDataSize;
                            leftPosition.addAndGet(fileDataSize);
                            if(flushPosition.get() < leftPosition.get()) {
                                flushPosition.set(leftPosition.get());
                            }
                        } else {
                            break;
                        }

                    } finally {
                        deleteLock.unlock();
                        writeLock.unlock();
                        flushLock.unlock();
                    }

                } else {
                    if (storeFileMap.remove(entry.getKey(), storeFile)) {
                        leftPosition.addAndGet(fileDataSize);
                        forceDeleteStoreFile(storeFile);
                        deleteSize += fileDataSize;
                    } else {
                        break;
                    }
                }
            }
        } finally {
            if(deleteLock.isHeldByCurrentThread()) {
                deleteLock.unlock();
            }
        }
        return deleteSize;
    }

    public boolean isClean() {
        return flushPosition() == right();
    }

    public long physicalDeleteLeftFile() throws IOException {
        if (storeFileMap.isEmpty()) return 0;
        StoreFile<T> storeFile = storeFileMap.firstEntry().getValue();
        return physicalDeleteTo(storeFile.position() + (storeFile.hasPage() ? storeFile.writePosition() : storeFile.fileDataSize()));
    }

    /**
     * 删除文件，丢弃未刷盘的数据，用于rollback
     */
    private void forceDeleteStoreFile(StoreFile<T> storeFile) throws IOException {
        storeFile.forceUnload();
        File file = storeFile.file();
        if (file.exists()) {
            if (file.delete()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Store file deleted, leftPosition: {}, rightPosition: {}, flushPosition: {}, store: {}.",
                            Format.formatWithComma(left()),
                            Format.formatWithComma(right()),
                            Format.formatWithComma(flushPosition()),
                            file.getAbsolutePath());
                }
            } else {
                throw new IOException(String.format("Delete file %s failed!", file.getAbsolutePath()));
            }
        }
    }

    /**
     * 获得日志的相对给定位置的位置（怎么这么绕？）
     * 先找到position所在的日志的起始位置，然后：
     * 向前(offsetCount为负值时)/向后(offsetCount为正值时
     * 偏移offsetCount条日志，返回日志的位置
     * 如果到了左右边界，直接返回左右边界位置。
     *
     * @param position    给定的绝对位置
     * @param offsetCount 偏移日志条数，可以为负值
     */
    public long position(long position, int offsetCount) {

        long t0 = SystemClock.now();
        int offset = 0;
        long pos = position;
        if (pos < left()) {
            pos = left();
        } else if (pos > right()) {
            pos = right();
        } else if (left() < pos && pos < right()) {
            pos = toLogStart(position);
        }

        if (offsetCount > 0) {
            while (offset < offsetCount && pos < right()) {
                pos = toLogTail(pos);
                offset++;
            }
        } else if (offsetCount < 0) {
            while (offset > offsetCount && pos > left()) {
                pos = toLogStart(pos - 1);
                offset--;
            }

        }
        long t1 = SystemClock.now();

        if(t1 - t0 > 300) {
            logger.info("Find log position takes: {}ms, store: {}", t1 - t0, base.getAbsolutePath());
        }

        return pos;
    }


    public File base() {
        return base;
    }

    @Override
    public void close() {
        for (StoreFile<T> storeFile : storeFileMap.values()) {
            storeFile.unload();
        }
    }

    public byte[] readBytes(long position, int length) throws IOException {
        checkReadPosition(position);
        StoreFile<T> storeFile = storeFileMap.floorEntry(position).getValue();
        int relPosition = (int) (position - storeFile.position());
        return storeFile.readByteBuffer(relPosition, length).array();
    }

    public int fileCount() {
        return storeFileMap.size();
    }

    public int meetMinStoreFile(long minIndexedPhysicalPosition) {
        return storeFileMap.headMap(minIndexedPhysicalPosition).size();
    }

    public static class Config {
        public static final int DEFAULT_FILE_HEADER_SIZE = 128;
        public static final int DEFAULT_FILE_DATA_SIZE = 128 * 1024 * 1024;
        public static final int DEFAULT_DISK_FULL_RATIO = 90;
        public static final int DEFAULT_MAX_MESSAGE_LENGTH = 4 * 1024 * 1024;
        public static final boolean DEFAULT_LOAD_ON_READ = false;
        public static final boolean DEFAULT_FLUSH_FORCE = true;

        /**
         * 文件头长度
         */
        private final int fileHeaderSize;
        /**
         * 文件内数据最大长度
         */
        private final int fileDataSize;

        private final int diskFullRatio;

        private final int maxMessageLength;

        /**
         * 读取文件的时候，是否预加载整个文件到内存中。
         */
        private final boolean loadOnRead;
        private final boolean flushForce;

        public Config() {
            this(DEFAULT_FILE_DATA_SIZE,
                    DEFAULT_FILE_HEADER_SIZE);
        }

        public Config(int fileDataSize) {
            this(fileDataSize,
                    DEFAULT_FILE_HEADER_SIZE);
        }

        public Config(int fileDataSize, int fileHeaderSize) {
            this(fileDataSize, fileHeaderSize, DEFAULT_DISK_FULL_RATIO);
        }
        public Config(int fileDataSize, int fileHeaderSize, int diskFullRatio) {
            this(fileDataSize, fileHeaderSize, diskFullRatio, DEFAULT_MAX_MESSAGE_LENGTH, DEFAULT_LOAD_ON_READ, DEFAULT_FLUSH_FORCE);
        }
        public Config(int fileDataSize, int fileHeaderSize, int diskFullRatio, int maxMessageLength, boolean loadOnRead, boolean flushForce) {
            this.fileDataSize = fileDataSize;
            this.fileHeaderSize = fileHeaderSize;
            this.diskFullRatio = diskFullRatio;
            this.maxMessageLength = maxMessageLength;
            this.loadOnRead = loadOnRead;
            this.flushForce = flushForce;
        }

        public Config(int fileDataSize, boolean loadOnRead, boolean flushForce) {
            this(fileDataSize, DEFAULT_FILE_HEADER_SIZE, DEFAULT_DISK_FULL_RATIO, DEFAULT_MAX_MESSAGE_LENGTH, loadOnRead, flushForce);
        }

        public Config(int messageFileSize, int fileHeaderSize, int diskFullRatio, int maxMessageLength) {
            this(messageFileSize,fileHeaderSize,diskFullRatio, maxMessageLength, DEFAULT_LOAD_ON_READ, DEFAULT_FLUSH_FORCE);
        }
    }


}
