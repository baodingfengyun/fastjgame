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

import javax.annotation.Nullable;
import java.io.Closeable;

/**
 * 可关闭资源工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/16
 * github - https://github.com/hl845740757
 */
public class CloseableUtils {


    private CloseableUtils() {
    }

    /**
     * 安静地关闭一个资源
     */
    public static void closeQuietly(@Nullable Closeable resource) {
        if (null != resource) {
            try {
                resource.close();
            } catch (Throwable ignore) {

            }
        }
    }
}
