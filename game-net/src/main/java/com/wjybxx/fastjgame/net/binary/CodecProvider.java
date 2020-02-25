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
 * 一组codec的注册器，该接口只服务编码需求
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public interface CodecProvider {

    /**
     * 获取指定类class对应的编解码器
     *
     * @param clazz 注意：某些作为键的类可能不是实际被编解码的类，如：{@link java.util.Map}
     * @return codec，如果不存在，则返回null (类似于之前设计的isSupport(Class))
     */
    @Nullable
    <T> Codec<T> getCodec(Class<T> clazz);
}
