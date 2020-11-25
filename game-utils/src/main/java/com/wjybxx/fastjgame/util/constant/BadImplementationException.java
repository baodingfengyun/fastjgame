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

package com.wjybxx.fastjgame.util.constant;

/**
 * 该异常表示实现类是错误的糟糕的。
 *
 * @author wjybxx
 * date - 2020/11/25
 * github - https://github.com/hl845740757
 */
public class BadImplementationException extends RuntimeException {

    public BadImplementationException() {
    }

    public BadImplementationException(String message) {
        super(message);
    }

    public BadImplementationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadImplementationException(Throwable cause) {
        super(cause);
    }

    public BadImplementationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
