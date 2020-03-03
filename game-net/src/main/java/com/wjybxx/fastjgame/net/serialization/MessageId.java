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

import com.wjybxx.fastjgame.utils.MathUtils;

/**
 * 消息唯一识别码
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/3
 */
public final class MessageId {

    /**
     * 分组id - 类似于命名空间，可减少冲突。
     * [0,127]
     * [0,10]底层备用，应用需要从11开始。
     */
    public final byte providerId;
    public final int classId;

    public MessageId(byte providerId, int classId) {
        this.providerId = providerId;
        this.classId = classId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MessageId other = (MessageId) o;
        return providerId == other.providerId && classId == other.classId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(MathUtils.composeIntToLong(providerId, classId));
    }
}
