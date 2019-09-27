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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.annotation.HttpRequestMapping;
import com.wjybxx.fastjgame.misc.DefaultHttpRequestDispatcher;
import com.wjybxx.fastjgame.misc.HttpResponseHelper;
import com.wjybxx.fastjgame.net.http.HttpRequestParam;
import com.wjybxx.fastjgame.net.http.HttpSession;
import com.wjybxx.fastjgame.utils.NetUtils;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
@HttpRequestMapping(path = "/main")
public class HttpServiceExample {

    @HttpRequestMapping(path = NetUtils.FAVICON_PATH, inherit = false)
    public void requestIcon(HttpSession httpSession, String path, HttpRequestParam param) {
        // path is NetUtils.FAVICON_PATH
        httpSession.writeAndFlush(HttpResponseHelper.newNotFoundResponse());
    }

    @HttpRequestMapping(path = "/login")
    public void requestLogin(HttpSession httpSession, String path, HttpRequestParam param) {
        // path is /main/login
    }

    public static void main(String[] args) {
        final DefaultHttpRequestDispatcher dispatcher = new DefaultHttpRequestDispatcher();
        // 使用生成的注册类将自己注册到消息分发器上
        HttpServiceExampleHttpRegister.register(dispatcher, new HttpServiceExample());
        //
    }
}
