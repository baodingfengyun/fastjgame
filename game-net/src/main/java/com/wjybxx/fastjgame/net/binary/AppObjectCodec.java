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

/**
 * 应用自定义对象编解码器
 * 它的主要特征包括：
 * 1. 它的{@link #classId}是用户计算的
 * 2. 会有大量生成的{@link EntitySerializer}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public abstract class AppObjectCodec<T> implements Codec<T> {

    private final int classId;

    protected AppObjectCodec(int classId) {
        this.classId = classId;
    }

    @Override
    public int getProviderId() {
        return CodecProviderConst.APP_PROVIDER_ID;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public WireType wireType() {
        return WireType.POJO;
    }
}
