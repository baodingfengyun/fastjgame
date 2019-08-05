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
import com.wjybxx.fastjgame.module.CenterModule;
import com.wjybxx.fastjgame.module.GlobalModule;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp;

import java.io.File;

/**
 * 启动参数：
 platform=TEST serverId=1
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 21:29
 * github - https://github.com/hl845740757
 */
public class CenterWorldTest {

    public static void main(String[] args) throws Exception {
        Injector globalModule = Guice.createInjector(new GlobalModule());
        NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("NET"));
        GameEventLoopGroup gameEventLoopGroup = new GameEventLoopGroupImp(1,
                new DefaultThreadFactory("LOGIC-WORLD"), netEventLoopGroup);

        String logDir=new File("").getAbsolutePath() + File.separator + "log";
        String logFilePath = logDir + File.separator + "center.log";
        System.setProperty("logFilePath",logFilePath);

        new Bootstrap<>(globalModule, gameEventLoopGroup)
                .setArgs(args)
                .setFramesPerSecond(5)
                .addModule(new CenterModule())
                .start();
    }
}
