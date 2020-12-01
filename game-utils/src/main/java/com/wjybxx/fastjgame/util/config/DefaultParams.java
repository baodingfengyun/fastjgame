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

package com.wjybxx.fastjgame.util.config;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * 约定：该实现不引用传入的对象，构建的均为快照。
 *
 * @author wjybxx
 * date - 2020/12/1
 * github - https://github.com/hl845740757
 */
@Immutable
public class DefaultParams extends Params {

    private final Map<String, String> params;
    private final ValueParser valueParser;

    private DefaultParams(Map<String, String> params, ValueParser valueParser) {
        this.params = params;
        this.valueParser = valueParser;
    }

    /**
     * 注意：该方法并不会保存{@link Map}的引用，因此只是个快照。
     */
    public static DefaultParams ofMap(Map<String, String> params, ValueParser valueParser) {
        return new DefaultParams(new LinkedHashMap<>(params), valueParser);
    }

    /**
     * 注意：该方法并不会保存{@link Properties}的引用，因此只是个快照。
     */
    public static DefaultParams ofProperties(Properties properties, ValueParser valueParser) {
        final Set<String> names = properties.stringPropertyNames();
        final Map<String, String> params = Maps.newLinkedHashMapWithExpectedSize(names.size());
        for (String name : names) {
            params.put(name, properties.getProperty(name));
        }
        return new DefaultParams(params, valueParser);
    }

    public static DefaultParams ofArray(String[] pairArray, String kvDelimiter, ValueParser valueParser) {
        final Map<String, String> map = Maps.newLinkedHashMapWithExpectedSize(pairArray.length);
        for (String pair : pairArray) {
            String[] keyValuePair = pair.split(kvDelimiter, 2);
            if (keyValuePair.length == 2) {
                map.put(keyValuePair[0], keyValuePair[1]);
            } else {
                map.put(keyValuePair[0], null);
            }
        }
        return new DefaultParams(map, valueParser);
    }

    /**
     * 合并两个{@link Params}，如果两个{@link Params}中存在相同的key，则第一个{@link Params}的值会被覆盖。
     */
    public static DefaultParams merge(Params first, Params second, ValueParser valueParser) {
        final Map<String, String> firstMap = first.toMap();
        final Map<String, String> secondMap = second.toMap();
        firstMap.putAll(secondMap);
        return new DefaultParams(firstMap, valueParser);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(params.keySet());
    }

    @Nullable
    @Override
    public String getAsString(String key) {
        return params.get(key);
    }

    @Override
    public Map<String, String> toMap() {
        return new LinkedHashMap<>(params);
    }

    @Override
    protected boolean parseBool(@Nonnull String value) {
        return valueParser.parseBool(value);
    }

    @Nonnull
    @Override
    protected List<String> parseList(@Nonnull String value) {
        return valueParser.parseList(value);
    }

    @Nonnull
    @Override
    protected Map<String, String> parseMap(@Nonnull String value) {
        return valueParser.parseMap(value);
    }
}
