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

package com.wjybxx.fastjgame.util.excel;

import com.google.common.collect.Maps;
import com.wjybxx.util.common.constant.AbstractConstant;
import com.wjybxx.util.common.constant.ConstantPool;

import java.util.List;
import java.util.Map;

/**
 * 默认的单元格解析器，为避免引入不必要的依赖，识别为json时只是当作普通字符串，不解析json。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public class DefaultCellValueParser {

    /**
     * @author wjybxx
     * date - 2020/11/20
     * github - https://github.com/hl845740757
     */
    public static final class CellType extends AbstractConstant<CellType> {

        private static final ConstantPool<CellType> POOL = new ConstantPool<>(CellType::new);

        private CellType(String name) {
            super(name);
        }

        public static final CellType STRING = POOL.newInstance("string");
        public static final CellType NUMBER = POOL.newInstance("number");
        public static final CellType FLOAT = POOL.newInstance("float");
        public static final CellType BOOL = POOL.newInstance("bool");

        public static final CellType JSON = POOL.newInstance("json");

        private static final List<CellType> BASIC_CELL_TYPE_LIST;
        private static final Map<String, CellType> ARRAY_CELL_TYPE_MAP;

        static {
            BASIC_CELL_TYPE_LIST = List.of(STRING, NUMBER, FLOAT, BOOL);
            ARRAY_CELL_TYPE_MAP = Maps.newHashMapWithExpectedSize(BASIC_CELL_TYPE_LIST.size() * 2);

            for (CellType cellType : BASIC_CELL_TYPE_LIST) {
                String a1 = cellType.name() + "[]";
                String a2 = cellType.name() + "[][]";

                ARRAY_CELL_TYPE_MAP.put(a1, POOL.newInstance(a1));
                ARRAY_CELL_TYPE_MAP.put(a2, POOL.newInstance(a2));
            }

        }

    }
}
