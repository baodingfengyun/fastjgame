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

package com.wjybxx.fastjgame.net.http;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * http响应处理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/28 19:53
 * github - https://github.com/hl845740757
 */
public interface HttpCallback {

    /**
     * 当http请求失败时该方法将被调用
     *
     * @param cause 造成失败的原因
     */
    void onFailure(@Nonnull IOException cause);

    /**
     * 当http请求成功时该方法将被调用
     *
     * @param response 响应内容
     */
    void onResponse(@Nonnull byte[] response);
}
