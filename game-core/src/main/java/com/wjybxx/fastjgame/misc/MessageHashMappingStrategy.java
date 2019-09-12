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

package com.wjybxx.fastjgame.misc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.utils.ClassScanner;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 所有消息类Hash映射策略。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 15:21
 * github - https://github.com/hl845740757
 */
public class MessageHashMappingStrategy implements MessageMappingStrategy {

    private static final String protoBufferPkg = "com.wjybxx.fastjgame.protobuffer";
    private static final String serializablePkg = "com.wjybxx.fastjgame.serializebale";

    /**
     * 同一个进程下使用是的相同的消息类，不必反复扫描
     */
    private static final Object2IntMap<Class<?>> messageClass2IdMap = new Object2IntOpenHashMap<>(512);

    static {
        // 先统计一下所有的吧 所有的带有注解的类，或 protoBuf类
        final Set<Class<?>> allClass = ClassScanner.findClasses("com.wjybxx.fastjgame",
                name -> StringUtils.countMatches(name, "$") <= 1,
                MessageHashMappingStrategy::isSerializable);

        for (Class<?> messageClass : allClass) {
            messageClass2IdMap.put(messageClass, getUniqueId(messageClass));
        }
    }

    private static boolean isSerializable(Class<?> clazz) {
        return clazz.isAnnotationPresent(SerializableClass.class)
                || AbstractMessage.class.isAssignableFrom(clazz)
                || ProtocolMessageEnum.class.isAssignableFrom(clazz);
    }

    private static int getUniqueId(Class<?> rpcCallClass) {
        // 不能直接使用hashCode，直接使用hashCode，在不同的进程的值是不一样的
        return rpcCallClass.getCanonicalName().hashCode();
    }

    @Override
    public Object2IntMap<Class<?>> mapping() throws Exception {
        return messageClass2IdMap;
    }

}
