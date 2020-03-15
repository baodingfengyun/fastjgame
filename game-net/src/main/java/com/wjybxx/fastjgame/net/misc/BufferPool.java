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
     * 最大缓冲区大小
     * 64K应该足够游戏内的任何内容了。
     */
    private static final int MAX_BUFFER_SIZE = 64 * 1024;

    /**
     * 使用队列主要是为了解决用户在序列化的过程中，递归调用序列化方法问题。
     */
    private static final ThreadLocal<Queue<byte[]>> LOCAL_BUFFER_QUEUE = ThreadLocal.withInitial(ArrayDeque::new);

    public static byte[] allocateBuffer() {
        final byte[] buffer = LOCAL_BUFFER_QUEUE.get().poll();
        if (buffer == null) {
            return new byte[MAX_BUFFER_SIZE];
        } else {
            return buffer;
        }
    }

    public static void releaseBuffer(byte[] buffer) {
        LOCAL_BUFFER_QUEUE.get().offer(buffer);
    }
}
