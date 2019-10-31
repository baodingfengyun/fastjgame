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

package com.wjybxx.fastjgame.start;

import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupBuilder;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.module.CenterModule;
import com.wjybxx.fastjgame.module.LoginModule;
import com.wjybxx.fastjgame.module.SceneModule;
import com.wjybxx.fastjgame.module.WarzoneModule;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp;

import java.io.File;

/**
 * 启动器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/5
 * github - https://github.com/hl845740757
 */
public class StartUp {

    /**
     * 战区服参数
     */
    private static final String[] warzoneArgs = new String[]{
            "warzoneId=" + 1
    };

    /**
     * 中心服参数
     */
    private static final String[] centerArgs = new String[]{
            "platform=" + PlatformType.TEST.name(),
            "serverId=" + 1
    };

    /**
     * 单服scene参数
     */
    private static final String[] singleSceneArgs = new String[]{
            "platform=" + PlatformType.TEST.name(),
            "serverId=" + 1,
            "configuredRegions=" + SceneRegion.LOCAL_PKC.name() + "|" + SceneRegion.LOCAL_NORMAL.name()
    };

    /**
     * 跨服scene参数
     */
    private static final String[] crossSceneArgs = new String[]{
            "warzoneId=" + 1,
            "configuredRegions=" + SceneRegion.WARZONE_ANTON.name() + "|" + SceneRegion.WARZONE_LUKE.name()
    };

    /**
     * 登录服参数
     */
    private static final String[] loginArgs = new String[]{
            "port=" + 12345
    };


    public static void main(String[] args) throws Exception {
        // 指定一下日志文件
        String logDir = new File("").getAbsolutePath() + File.separator + "log";
        String logPath = logDir + File.separator + "fastjgame.log";
        System.out.println("logPath " + logPath);
        System.setProperty("logPath", logPath);

        // 试一试ALL IN ONE
        // NET线程数最少1个
        NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupBuilder()
                .setNetEventLoopNum(2)
                .setBossGroupThreadNum(2)
                .setWorkerGroupThreadNum(4)
                .build();

        final GameEventLoopGroupImp gameEventLoopGroup = GameEventLoopGroupImp.newBuilder()
                .setNetEventLoopGroup(netEventLoopGroup)
                .setRejectedExecutionHandler(RejectedExecutionHandlers.log())
                .addWorld(new LoginModule(), loginArgs, 10)
                .addWorld(new WarzoneModule(), warzoneArgs, 10)
                .addWorld(new CenterModule(), centerArgs, 10)
                .addWorld(new SceneModule(), singleSceneArgs, 20)
                .addWorld(new SceneModule(), singleSceneArgs, 20)
                .addWorld(new SceneModule(), crossSceneArgs, 20)
                .addWorld(new SceneModule(), crossSceneArgs, 20)
                .build();
        try {
            Thread.sleep(2 * TimeUtils.MIN);
        } catch (InterruptedException ignore) {
        }
        // 试一试能否安全关闭
        gameEventLoopGroup.shutdown();
        netEventLoopGroup.shutdown();
        System.out.println(" ******* invoked shutdown *******");
    }
}
