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

package com.wjybxx.fastjgame.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.Bootstrap;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.module.GlobalModule;
import com.wjybxx.fastjgame.module.LoginModule;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp;

import java.io.File;

/**
 * loginserver测试用例。
 *
 * 启动参数：
 port=8989
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/18 15:46
 * github - https://github.com/hl845740757
 */
public class LoginWorldTest {

    public static void main(String[] args) throws Exception {
        String logDir=new File("").getAbsolutePath() + File.separator + "log";
        String logFilePath = logDir + File.separator + "login.log";
        System.setProperty("logFilePath",logFilePath);

        NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET"));
        GameEventLoopGroup gameEventLoopGroup = new GameEventLoopGroupImp(1, new DefaultThreadFactory("LOGIC-WORLD"), netEventLoopGroup);

        new Bootstrap<>(gameEventLoopGroup)
                .setArgs(args)
                .setFramesPerSecond(5)
                .addModule(new LoginModule())
                .start();
    }
}
