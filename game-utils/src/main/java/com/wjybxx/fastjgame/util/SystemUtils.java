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

package com.wjybxx.fastjgame.util;

import com.wjybxx.fastjgame.util.config.ConfigWrapper;
import com.wjybxx.fastjgame.util.config.PropertiesConfigWrapper;

/**
 * 系统属性工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/20 14:53
 * github - https://github.com/hl845740757
 */
public class SystemUtils {

    /**
     * 系统属性 - 既可以是真实的系统属性，也可以把配置文件中的部分属性放在这里
     */
    private static final ConfigWrapper properties = new PropertiesConfigWrapper(System.getProperties());

    private SystemUtils() {

    }

    public static ConfigWrapper getProperties() {
        return properties;
    }

    public static boolean isWindows() {
        return org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
    }

    public static boolean isLinux() {
        return org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
    }
}
