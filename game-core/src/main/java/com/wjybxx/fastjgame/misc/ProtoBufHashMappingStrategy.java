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
import com.wjybxx.fastjgame.utils.ClassScanner;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * protoBuffer消息hash映射策略。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 15:21
 * github - https://github.com/hl845740757
 */
public class ProtoBufHashMappingStrategy implements MessageMappingStrategy {

    private static final String protoBufferPkg = "com.wjybxx.fastjgame.protobuffer";
    /** 同一个进程下使用是的相同的消息类，不必反复扫描 */
    private static final Object2IntMap<Class<?>> messageClass2IdMap;

    static {
        // 只有一层内部类
        messageClass2IdMap = new Object2IntOpenHashMap<>();
        Set<Class<?>> allClass = ClassScanner.findClasses(protoBufferPkg,
                name -> StringUtils.countMatches(name, "$") == 1,
                AbstractMessage.class::isAssignableFrom);

        for (Class<?> messageClass:allClass) {
            messageClass2IdMap.put(messageClass, messageClass.getCanonicalName().hashCode());
        }
    }

    @Override
    public Object2IntMap<Class<?>> mapping() throws Exception {
        return messageClass2IdMap;
    }
}
