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

package com.wjybxx.fastjgame.scene;

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * 场景区域划分。
 * 划重点：
 * 1. 场景区域互斥区域和非互斥区域。
 * 2. 场景区域分单服区域和跨服区域。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 11:33
 * github - https://github.com/hl845740757
 */
@SerializableClass
public enum SceneRegion implements NumberEnum {

    /**
     * 本服普通区域，不互斥，大多数地图都应该属于它。
     */
    LOCAL_NORMAL(1, false, false),
    /**
     * 本服竞技场(DNF玩习惯了，习惯叫PKC)，互斥
     */
    LOCAL_PKC(2, false, true),
    /**
     * 安徒恩，跨服，不互斥。
     */
    WARZONE_ANTON(3, true, false),
    /**
     * 卢克，跨服，不互斥。
     */
    WARZONE_LUKE(4, true, false);

    /**
     * 数字标记，不使用ordinal
     */
    private final int number;
    /**
     * 是否是跨服区域(未来配置到表格)
     */
    private final boolean cross;

    /**
     * 该区域是否互斥，只能存在一个
     */
    private final boolean mutex;

    SceneRegion(int number, boolean cross, boolean mutex) {
        this.number = number;
        this.cross = cross;
        this.mutex = mutex;
    }

    /**
     * 数字id到枚举的映射
     */
    private static final NumberEnumMapper<SceneRegion> mapper = EnumUtils.mapping(values());

    public static SceneRegion forNumber(int number) {
        SceneRegion sceneRegion = mapper.forNumber(number);
        assert null != sceneRegion : "invalid number " + number;
        return sceneRegion;
    }

    @Override
    public int getNumber() {
        return number;
    }

    public boolean isCross() {
        return cross;
    }

    public boolean isMutex() {
        return mutex;
    }
}
