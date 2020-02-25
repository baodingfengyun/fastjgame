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
 * 该接口同时支持编码和解码。
 * 它对应{@link Tag}中的POJO类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public interface PojoCodecProvider extends CodecProvider {

    /**
     * 它的意义相当于命名空间
     * Q: 它的意义是什么？
     * A: 避免用户为默认支持的类分配id
     */
    int getProviderId();

    /**
     * 通过classId获取对应的codec
     *
     * @return codec, 如果不存在，则返回null
     */
    @Nullable
    Codec<?> getCodec(int classId);
}

