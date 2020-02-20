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

package com.wjybxx.fastjgame.net.serializer;

import com.wjybxx.fastjgame.net.binary.EntityInputStream;
import com.wjybxx.fastjgame.net.binary.EntityOutputStream;
import com.wjybxx.fastjgame.net.binary.EntitySerializer;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 它负责Object数组的解析 - 它会被扫描到
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
@SuppressWarnings("unused")
public class ObjectArraySerializer implements EntitySerializer<Object[]> {

    @Override
    public Class<Object[]> getEntityClass() {
        return Object[].class;
    }

    @Override
    public Object[] readObject(EntityInputStream inputStream) throws Exception {
        final int length = inputStream.readInt();
        if (length == 0) {
            return ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        final Object[] result = new Object[length];
        for (int index = 0; index < length; index++) {
            result[index] = inputStream.readRuntime();
        }
        return result;
    }

    @Override
    public void writeObject(Object[] instance, EntityOutputStream outputStream) throws Exception {
        outputStream.writeInt(instance.length);
        for (Object object : instance) {
            outputStream.writeRuntime(object);
        }
    }
}
