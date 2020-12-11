/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.util.pool;

/**
 * 当实现此接口的对象被传递给{@link Pool#free(Object)}时，它的{@link #reset()}方法将会被调用。
 */
public interface Poolable {

    /**
     * 重置对象状态以便重用。
     * 对象引用应为空，字段可以设置为默认值。
     */
    void reset();

}
