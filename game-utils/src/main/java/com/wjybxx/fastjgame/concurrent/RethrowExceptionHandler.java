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

package com.wjybxx.fastjgame.concurrent;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;

/**
 * 重新抛出异常的异常处理器你
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/23
 * github - https://github.com/hl845740757
 */
public class RethrowExceptionHandler implements ExceptionHandler{

    public static final RethrowExceptionHandler INSTANCE = new RethrowExceptionHandler();

    private RethrowExceptionHandler() {
    }

    @Override
    public void handleException(Exception e) {
        ConcurrentUtils.rethrow(e);
    }
}
