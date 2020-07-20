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

package com.wjybxx.fastjgame.net.binaryextend;

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;
import com.wjybxx.fastjgame.utils.misc.Tuple3;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/20
 */
@SuppressWarnings({"rawtypes"})
public class Tuple3Codec implements PojoCodecImpl<Tuple3> {

    @Override
    public Class<Tuple3> getEncoderClass() {
        return Tuple3.class;
    }

    @Override
    public Tuple3 readObject(ObjectReader reader) throws Exception {
        return new Tuple3<>(reader.readObject(), reader.readObject(), reader.readObject());
    }

    @Override
    public void writeObject(Tuple3 instance, ObjectWriter writer) throws Exception {
        writer.writeObject(instance.first);
        writer.writeObject(instance.second);
        writer.writeObject(instance.third);
    }
}
