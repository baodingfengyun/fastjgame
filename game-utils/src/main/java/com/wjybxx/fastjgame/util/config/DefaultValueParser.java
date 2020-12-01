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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author wjybxx
 * date - 2020/12/1
 * github - https://github.com/hl845740757
 */
public class DefaultValueParser implements ValueParser {

    public static final DefaultValueParser INSTANCE = new DefaultValueParser();

    private DefaultValueParser() {
    }

    @Override
    public boolean parseBool(@Nonnull final String value) {
        return value.equals("1")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("y");
    }

    /**
     * a1,a2,a3
     */
    @Override
    public List<String> parseList(@Nonnull final String value) {
        final String[] array = value.split(",");
        final List<String> result = new ArrayList<>(array.length);
        Collections.addAll(result, array);
        return result;
    }

    /**
     * k1:v1, k2:v2, k3:v3
     */
    @Override
    public Map<String, String> parseMap(@Nonnull final String value) {
        final String[] array = value.split(",");
        final Map<String, String> result = Maps.newLinkedHashMapWithExpectedSize(array.length);
        for (String pair : array) {
            final String[] kvArray = pair.split(":", 2);
            if (kvArray.length != 2) {
                throw new IllegalArgumentException("pair mismatch, pair: " + pair);
            }
            final String k = kvArray[0];
            final String v = kvArray[1];
            if (result.put(k, v) != null) {
                final String msg = String.format("k duplicate, value: %s, k: %s", value, k);
                throw new IllegalArgumentException(msg);
            }
        }
        return result;
    }
}
