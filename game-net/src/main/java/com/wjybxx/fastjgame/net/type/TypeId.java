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

package com.wjybxx.fastjgame.net.type;

import com.wjybxx.fastjgame.utils.MathUtils;

/**
 * 精简版的类型信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/9
 */
public class TypeId {

    /**
     * 1. 当使用算法生成id时可以减少冲突。
     * 2. 可以表示来源。
     */
    private final byte namespace;
    private final int classId;

    public TypeId(byte namespace, int classId) {
        this.namespace = namespace;
        this.classId = classId;
    }

    public byte getNamespace() {
        return namespace;
    }

    public int getClassId() {
        return classId;
    }

    public long toGuid() {
        return MathUtils.composeIntToLong(namespace, classId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TypeId that = (TypeId) o;
        return namespace == that.namespace && classId == that.classId;
    }

    @Override
    public int hashCode() {
        return 31 * (int) namespace + classId;
    }

    @Override
    public String toString() {
        return "TypeId{" +
                "namespace=" + namespace +
                ", classId=" + classId +
                '}';
    }
}
