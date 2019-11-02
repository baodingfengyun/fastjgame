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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.utils.MathUtils;

import javax.annotation.Nonnull;

/**
 * 唯一服务器编号
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/2
 * github - https://github.com/hl845740757
 */
public class UniqueServerID {

    private final PlatformType platformType;
    private final int serverId;

    public UniqueServerID(@Nonnull PlatformType platformType, int serverId) {
        this.platformType = platformType;
        this.serverId = serverId;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UniqueServerID that = (UniqueServerID) o;
        return platformType == that.platformType && serverId == that.serverId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(MathUtils.composeToLong(platformType.getNumber(), serverId));
    }
}
