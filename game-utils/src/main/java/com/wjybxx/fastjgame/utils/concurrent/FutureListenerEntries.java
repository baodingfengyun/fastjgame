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

package com.wjybxx.fastjgame.utils.concurrent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@link FutureListenerEntry}的容器。
 * Q: 主要解决什么问题？
 * A: 主要解决扩容问题，不使用{@link ArrayList}，是因为@link ArrayList}默认扩容50%，初始容量较小的话，扩容过于频繁 - 2，3，4，6，9。
 * 而这里是扩一倍，对于小容量容器来说，更为友好。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/7
 * github - https://github.com/hl845740757
 */
final class FutureListenerEntries {

    private FutureListenerEntry<?>[] children;
    private int size;

    FutureListenerEntries(FutureListenerEntry<?> first, FutureListenerEntry<?> second) {
        children = new FutureListenerEntry<?>[2];
        children[0] = first;
        children[1] = second;
    }

    void addChild(FutureListenerEntry<?> child) {
        ensureCapacity();
        children[size++] = child;
    }

    private void ensureCapacity() {
        if (size == children.length) {
            this.children = Arrays.copyOf(children, size << 1);
        }
    }

    FutureListenerEntry<?>[] getChildren() {
        return children;
    }

    int getSize() {
        return size;
    }
}
