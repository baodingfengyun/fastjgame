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

import javax.annotation.Nullable;

/**
 * 一个局部的{@link Codec}注册表，它管理着一组{@link Codec}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface CodecProvider {

    /**
     * 获取指定类class对应的编解码器
     */
    @Nullable
    <T> Codec<T> getCodec(Class<T> clazz);

}
