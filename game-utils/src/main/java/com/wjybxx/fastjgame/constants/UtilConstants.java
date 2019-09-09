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

package com.wjybxx.fastjgame.constants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 工具包的常量
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 20:10
 * github - https://github.com/hl845740757
 */
public class UtilConstants {

    private UtilConstants() {
    }

    /**
     * 默认数组分隔符 '|'
     * 逗号在某些场合下效果不好，逗号使用面太广。
     */
    public static final String DEFAULT_ARRAY_DELIMITER = "\\|";

    /**
     * 默认键值对分隔符, '=' 与 ':' 都是不错的选择， ':'更贴近于json
     */
    public static final String DEFAULT_KEY_VALUE_DELIMITER = "=";

    /**
     * 游戏默认的字符编码集
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
}
