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

package com.wjybxx.fastjgame.utils;

/**
 * 用于控制debug相关参数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/29
 * github - https://github.com/hl845740757
 */
public class DebugUtils {

    /**
     * 是否开启了debug - 存为volatile而非final是为了方便运行期间开启和关闭
     */
    private static volatile boolean debug = SystemUtils.getProperties().getAsBool("fastjgame.debug", false);

    /**
     * 是否开启了debug模式
     */
    public static boolean isDebugOpen() {
        return debug;
    }

    /**
     * 开启debug
     */
    public static void openDebug() {
        debug = true;
    }

    /**
     * 关闭debug
     */
    public static void closeDebug() {
        debug = false;
    }
}
