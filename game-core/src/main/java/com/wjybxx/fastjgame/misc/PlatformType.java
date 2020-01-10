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

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.enummapper.NumericalEnum;
import com.wjybxx.fastjgame.enummapper.NumericalEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;


/**
 * 平台类型，平台问题最终还是会遇见，这里先处理。
 * 可能是：
 * 1. android、ios - ios 和 android进度不一样
 * 2. 运营平台类型，给多个平台运营的时候
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/17 22:02
 * github - https://github.com/hl845740757
 */
@SerializableClass
public enum PlatformType implements NumericalEnum {

    /**
     * 测试用的平台
     */
    TEST(0),
    ;

    /**
     * 平台数字标记，不可以修改。
     * 枚举的名字可以修改。
     */
    private final int number;

    PlatformType(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    private static final NumericalEnumMapper<PlatformType> mapper = EnumUtils.mapping(values());

    public static PlatformType forNumber(int number) {
        PlatformType platformType = mapper.forNumber(number);
        assert null != platformType : "invalid number " + number;
        return platformType;
    }
}
