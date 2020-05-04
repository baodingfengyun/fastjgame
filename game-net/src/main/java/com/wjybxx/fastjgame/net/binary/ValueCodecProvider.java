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
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 它对应于{@link BinaryTag}中的9种基础数据类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/25
 */
public class ValueCodecProvider implements CodecProvider {

    private final Map<Class<?>, ObjectCodec<?>> codecMap = new IdentityHashMap<>(9);

    ValueCodecProvider() {
        addCodecs();
    }

    private void addCodecs() {
        addCodec(new ByteCodec());
        addCodec(new CharCodec());
        addCodec(new ShortCodec());
        addCodec(new IntegerCodec());
        addCodec(new LongCodec());
        addCodec(new FloatCodec());
        addCodec(new DoubleCodec());
        addCodec(new BooleanCodec());
        addCodec(new StringCodec());
    }

    private void addCodec(ObjectCodec<?> codec) {
        codecMap.put(codec.getEncoderClass(), codec);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> ObjectCodec<T> getCodec(Class<T> clazz) {
        return (ObjectCodec<T>) codecMap.get(clazz);
    }
}
