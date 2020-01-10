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

package com.wjybxx.fastjgame.misc.rank;

import com.wjybxx.fastjgame.enummapper.NumericalEnum;
import com.wjybxx.fastjgame.enummapper.NumericalEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * 排行榜类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/7
 * github - https://github.com/hl845740757
 */
public enum RankType implements NumericalEnum {

    /**
     * 角色等级榜
     */
    PLAYER_LEVEL(1),
    ;
    private final int number;

    RankType(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    private static final NumericalEnumMapper<RankType> mapper = EnumUtils.mapping(values());

    /**
     * 通过排行榜类型数字查找对应的类型
     *
     * @param number 排行榜
     * @return 如果不存在，则返回null
     */
    public static RankType forNumber(int number) {
        return mapper.forNumber(number);
    }
}
