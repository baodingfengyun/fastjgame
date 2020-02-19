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

package com.wjybxx.fastjgame.net.serializer;

import com.wjybxx.fastjgame.net.binary.EntityInputStream;
import com.wjybxx.fastjgame.net.binary.EntityOutputStream;
import com.wjybxx.fastjgame.net.binary.EntitySerializer;
import com.wjybxx.fastjgame.utils.misc.Chunk;

/**
 * 负责{@link Chunk}类的序列化和反序列化 - 会被扫描到
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 * github - https://github.com/hl845740757
 */
@SuppressWarnings("unused")
public class ChunkSerializer implements EntitySerializer<Chunk> {

    @Override
    public Class<Chunk> getEntityClass() {
        return Chunk.class;
    }

    @Override
    public Chunk readObject(EntityInputStream inputStream) throws Exception {
        return Chunk.newInstance(inputStream.readBytes());
    }

    @Override
    public void writeObject(Chunk instance, EntityOutputStream outputStream) throws Exception {
        outputStream.writeBytes(instance.getBuffer(), instance.getOffset(), instance.getLength());
    }
}
