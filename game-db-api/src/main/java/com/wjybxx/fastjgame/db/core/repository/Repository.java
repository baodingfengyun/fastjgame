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
 * 资源库
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/8
 */
@UnstableApi
public interface Repository<T extends PersistableObject> {

    /**
     * 适用于首次
     */
    void store(T instance);

    /**
     * 适用于更新
     */
    void update(T instance);

    /**
     * 从持久化数据中删除给定对象
     */
    void remove(long id);

    /**
     * 通过id获取指定对象
     */
    T ofId(long id);

}
