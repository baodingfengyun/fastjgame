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

package com.wjybxx.fastjgame.net.binary;

import com.wjybxx.fastjgame.db.core.TypeId;
import com.wjybxx.fastjgame.db.core.TypeModelMapper;

import javax.annotation.Nullable;

/**
 * {@link PojoCodecImpl}全局注册表，可以获取所有注册的{@link PojoCodecImpl}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface CodecRegistry {

    /**
     * 获取关联的{@link TypeModelMapper}
     */
    TypeModelMapper typeModelMapper();

    /**
     * 获取指定类class对应的编解码器
     */
    @Nullable
    <T> PojoCodecImpl<T> get(Class<T> clazz);

    /**
     * 通过类型的id获取对应的编解码器。
     * {@link TypeModelMapper#ofId(TypeId)}和{@link #get(Class)}的快捷调用
     */
    @Nullable
    <T> PojoCodecImpl<T> get(TypeId typeId);

}
