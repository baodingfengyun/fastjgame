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

package com.wjybxx.fastjgame.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 常用的timer异常处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/28
 * github - https://github.com/hl845740757
 */
public class ExceptionHandlers {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlers.class);

    /**
     * 记录异常信息，并自动关闭timer
     */
    public static final ExceptionHandler CLOSE = (handle, cause) -> {
        handle.close();
        logger.warn("timer callback caught exception", cause);
    };

    /**
     * 仅仅记录一个日常，不影响接下来的执行
     */
    public static final ExceptionHandler LOG = (handle, cause) -> {
        logger.warn("timer callback caught exception", cause);
    };

}
