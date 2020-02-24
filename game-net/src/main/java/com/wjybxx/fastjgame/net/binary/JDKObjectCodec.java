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

import com.wjybxx.fastjgame.utils.entity.NumericalEntity;

/**
 * JDK对象的编解码器，它的主要特征是：{@link #classId}是固定值。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/24
 */
public abstract class JDKObjectCodec<T> implements Codec<T>, NumericalEntity {

    /**
     * classId统一分配，每一个Codec得到的值是固定值
     */
    private final int classId;

    protected JDKObjectCodec(int classId) {
        this.classId = classId;
    }


    @Override
    public int getProviderId() {
        return CodecProviderConst.JDK_PROVIDER_ID;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public int getNumber() {
        return classId;
    }

    @Override
    public WireType wireType() {
        return WireType.POJO;
    }
}
