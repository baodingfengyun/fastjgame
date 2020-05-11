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

package com.wjybxx.fastjgame.net.serialization;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.db.core.TypeId;
import com.wjybxx.fastjgame.db.core.TypeMappingStrategy;
import com.wjybxx.fastjgame.db.core.TypeModel;

/**
 * 基于hash的消息映射方法，由类的简单名计算hash值。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 15:21
 * github - https://github.com/hl845740757
 */
public class HashTypeMappingStrategy implements TypeMappingStrategy {

    public HashTypeMappingStrategy() {

    }

    @Override
    public TypeModel mapping(Class<?> messageClass) {
        return hash(messageClass);
    }

    /**
     * 计算一个消息类的唯一hash值
     */
    private static TypeModel hash(Class<?> type) {
        final byte namespace = calNamespace(type);
        final int classId = calClassId(type);
        final TypeId typeId = new TypeId(namespace, classId);
        return new TypeModel(type, type.getSimpleName(), typeId);
    }

    private static int calClassId(Class<?> type) {
        // 不能直接使用hashCode，直接使用hashCode，在不同的进程的值是不一样的
        // 为什么要simple Name? protoBuf的消息的名字就是java的类名，也方便前端计算该值 - 相同的hash算法即可
        return type.getSimpleName().hashCode();
    }

    private static byte calNamespace(Class<?> messageClass) {
        if (Message.class.isAssignableFrom(messageClass)) {
            return 1;
        }
        if (ProtocolMessageEnum.class.isAssignableFrom(messageClass)) {
            return 2;
        }
        return 3;
    }
}
