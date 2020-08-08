/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net.serialization;

import com.wjybxx.fastjgame.util.MathUtils;

/**
 * 精简版的类型信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/5/9
 */
public class TypeId {

    /**
     * 数组命名空间 - 在为其它数组分配{@link TypeId}时，需要保证{@link #classId}大于0，或使用其它命名空间
     */
    public static final byte NAMESPACE_ARRAY = 127;
    public static final TypeId DEFAULT_ARRAY = new TypeId(NAMESPACE_ARRAY, 0);

    /**
     * 集合命名空间 - 在为其它集合分配{@link TypeId}时，需要保证{@link #classId}大于0，或使用其它命名空间
     */
    public static final byte NAMESPACE_COLLECTION = 126;
    public static final TypeId DEFAULT_LIST = new TypeId(NAMESPACE_COLLECTION, 0);
    public static final TypeId DEFAULT_SET = new TypeId(NAMESPACE_COLLECTION, -1);

    /**
     * Map命名空间 - 在为其它Map分配{@link TypeId}时，需要保证{@link #classId}大于0，或使用其它命名空间
     */
    public static final byte NAMESPACE_MAP = 125;
    public static final TypeId DEFAULT_MAP = new TypeId(NAMESPACE_MAP, 0);

    /**
     * 1. 当使用算法生成id时可以减少冲突。
     * 2. 可以表示来源。
     */
    private final byte namespace;
    /**
     * class尽量保持稳定。
     * 最简单的方式是计算类的简单名的hash。 {@link Class#getSimpleName()}
     */
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
