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

package com.wjybxx.fastjgame.configwrapper;


import com.wjybxx.fastjgame.constants.UtilConstants;
import com.wjybxx.fastjgame.utils.CollectionUtils;

import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 基于数组的键值对配置的包装器。
 * 数组的每一个元素都是一个键值对 key=value，如：[a=1,b=2]
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 13:36
 * github - https://github.com/hl845740757
 */
@Immutable
public class ArrayConfigWrapper extends ConfigWrapper {

    /**
     * 索引好的数据
     */
    private Map<String,String> indexedMap;

    public ArrayConfigWrapper(String[] pairsArray) {
        this(pairsArray, UtilConstants.DEFAULT_KEY_VALUE_DELIMITER);
    }

    /**
     * @param pairsArray 键值对数组
     * @param kvDelimiter 键值对分隔符
     */
    public ArrayConfigWrapper(String[] pairsArray, String kvDelimiter) {
        this.indexedMap = index(pairsArray, kvDelimiter);
    }

    @Override
    public Set<String> keys() {
        return indexedMap.keySet();
    }

    @Override
    public String getAsString(String key) {
        return indexedMap.get(key);
    }

    @Override
    public MapConfigWrapper convert2MapWrapper() {
        return new MapConfigWrapper(indexedMap);
    }

    @Override
    public String toString() {
        return "ArrayConfigWrapper{" +
                "indexedMap=" + indexedMap +
                '}';
    }

    private static Map<String, String> index(String[] pairsArray, String kvDelimiter) {
        HashMap<String,String> map = CollectionUtils.newEnoughCapacityHashMap(pairsArray.length);
        for (String pair:pairsArray){
            String[] keyValuePair = pair.split(kvDelimiter, 2);
            if (keyValuePair.length==2){
                map.put(keyValuePair[0],keyValuePair[1]);
            }else {
                map.put(keyValuePair[0],null);
            }
        }
        return map;
    }
}
