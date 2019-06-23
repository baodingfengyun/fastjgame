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

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.enummapper.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * 枚举辅助类
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/22 17:51
 * github - https://github.com/hl845740757
 */
public class EnumUtils {

    private EnumUtils() {
        // close
    }

    /**
     * 根据枚举的values建立索引；
     * 该方法的开销相对小，代码量也能省下；
     * @param values 枚举数组
     * @param <T> 枚举类型
     * @return unmodifiable
     */
    @SuppressWarnings("unchecked")
    public static <T extends NumberEnum> NumberEnumMapper<T> indexNumberEnum(T[] values){
        if (values.length == 0){
            return (NumberEnumMapper<T>) EmptyMapper.INSTANCE;
        }

        // 存在一定的浪费，判定重复用
        Int2ObjectMap<T> result = FastCollectionsUtils.newEnoughCapacityIntMap(values.length);
        int minNumber = values[0].getNumber();
        int maxNumber = values[0].getNumber();

        for (T t : values){
            FastCollectionsUtils.requireNotContains(result,t.getNumber(),"number");
            result.put(t.getNumber(),t);

            minNumber = Math.min(minNumber,t.getNumber());
            maxNumber = Math.max(maxNumber,t.getNumber());
        }

        if (ArrayBasedMapper.available(minNumber,maxNumber, values.length)){
            return new ArrayBasedMapper<>(values,minNumber,maxNumber);
        }else {
            return new MapBasedMapper<>(result);
        }
    }
}
