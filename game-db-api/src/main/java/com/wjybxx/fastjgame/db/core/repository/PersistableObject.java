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

package com.wjybxx.fastjgame.db.core.repository;

import com.wjybxx.fastjgame.utils.annotation.UnstableApi;

/**
 * 可持久化的对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/8
 */
@UnstableApi
public interface PersistableObject {

    /**
     * 我们在持久化时，统一的要求每个对象使用{@code long}关联数据库的一个存储结构(一个数据库实体)。
     * 如果是实体对象，建议就是实体对象的{@code id}；
     * 如果是值对象，该值并不会改变值对象的含义，值对象也可以有唯一标识，但含义并不一样。比如：有许多事件是值对象，但同时也是唯一的不可重复的。
     */
    long persistentId();

    /**
     * 将对象存入数据库中。
     * 新增对象必须调用该方法才会真正入库
     */
    void persist();

    /**
     * 从数据库中删除
     */
    void remove();

    /**
     * 刷新缓冲区。
     * 底层可能会缓存增量，调用该方法立即刷新缓冲区。
     */
    void update();

}
