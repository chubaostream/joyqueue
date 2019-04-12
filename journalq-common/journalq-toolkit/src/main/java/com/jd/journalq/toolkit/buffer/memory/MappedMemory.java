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
package com.jd.journalq.toolkit.buffer.memory;

import com.jd.journalq.toolkit.lang.Preconditions;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Mapped memory.
 * <p>
 * This is a special memory descriptor that handles management of {@link java.nio.MappedByteBuffer} based memory. The
 * mapped memory descriptor simply points to the memory address of the underlying byte buffer. When memory is
 * reallocated,
 * the parent {@link MappedMemoryAllocator} is used to create a new {@link java.nio.MappedByteBuffer}
 * and free the existing buffer.
 */
public class MappedMemory extends NativeMemory<MappedMemory, MappedMemoryAllocator> {

    /**
     * Allocates memory mapped to a file on disk.
     *
     * @param file The file to which to map memory.
     * @param size The count of the memory to map.
     * @return The mapped memory.
     * @throws IllegalArgumentException If {@code count} is greater than {@link Integer#MAX_VALUE}
     */
    public static MappedMemory allocate(final File file, final long size) {
        return new MappedMemoryAllocator(file).allocate(size);
    }

    /**
     * Allocates memory mapped to a file on disk.
     *
     * @param file The file to which to map memory.
     * @param mode The mode with which to map memory.
     * @param size The count of the memory to map.
     * @return The mapped memory.
     * @throws IllegalArgumentException If {@code count} is greater than {@link Integer#MAX_VALUE}
     */
    public static MappedMemory allocate(final File file, final FileChannel.MapMode mode, final long size) {
        Preconditions.checkNotNull(file, "file cannot be null");
        Preconditions.checkArgument(size >= 0 && size <= SIZE_MAX, SIZE_ERROR);
        return new MappedMemoryAllocator(file, mode).allocate(size);
    }

    protected MappedByteBuffer memory;

    public MappedMemory(final MappedByteBuffer memory, final MappedMemoryAllocator allocator) {
        super(((DirectBuffer) memory).address(), memory.capacity(), allocator);
        this.memory = memory;
    }

    public MappedByteBuffer memory() {
        return memory;
    }

    /**
     * Flushes the mapped buffer to disk.
     */
    public void flush() {
        memory.force();
    }

    @Override
    public void free() {
        Cleaner cleaner = ((DirectBuffer) memory).cleaner();
        if (cleaner != null) {
            cleaner.clean();
        }
        allocator.release();
    }

}
