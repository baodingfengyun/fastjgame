/*
 * Copyright 2019 wjybxx
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

package com.wjybxx.fastjgame.net.misc;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 临时实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/15
 * github - https://github.com/hl845740757
 */
public class BufferPool {

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 512 * 1024;
    /**
     * 缓存数量
     */
    private static final int POOL_SIZE = 16;

    /**
     * 使用队列主要是为了解决用户在序列化的过程中，递归调用序列化方法问题。
     */
    private static final ThreadLocal<Queue<byte[]>> LOCAL_BUFFER_QUEUE = ThreadLocal.withInitial(() -> new ArrayDeque<>(POOL_SIZE));

    public static byte[] allocateBuffer() {
        final byte[] buffer = LOCAL_BUFFER_QUEUE.get().poll();
        if (buffer == null) {
            return new byte[BUFFER_SIZE];
        } else {
            return buffer;
        }
    }

    public static void releaseBuffer(byte[] buffer) {
        final Queue<byte[]> queue = LOCAL_BUFFER_QUEUE.get();
        if (queue.size() >= POOL_SIZE) {
            return;
        }
        queue.offer(buffer);
    }
}
