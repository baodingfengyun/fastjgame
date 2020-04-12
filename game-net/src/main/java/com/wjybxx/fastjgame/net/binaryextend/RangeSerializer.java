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

import com.wjybxx.fastjgame.net.binary.EntityInputStream;
import com.wjybxx.fastjgame.net.binary.EntityOutputStream;
import com.wjybxx.fastjgame.net.binary.EntitySerializer;
import com.wjybxx.fastjgame.utils.misc.Range;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/12
 */
@SuppressWarnings("unused")
public class RangeSerializer implements EntitySerializer<Range> {

    @Override
    public Class<Range> getEntityClass() {
        return Range.class;
    }

    @Override
    public Range readObject(EntityInputStream inputStream) throws Exception {
        final int start = inputStream.readInt();
        final int end = inputStream.readInt();
        return new Range(start, end);
    }

    @Override
    public void writeObject(Range instance, EntityOutputStream outputStream) throws Exception {
        outputStream.writeInt(instance.start);
        outputStream.writeInt(instance.end);
    }
}
