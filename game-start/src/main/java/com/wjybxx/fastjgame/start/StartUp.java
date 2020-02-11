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

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.wjybxx.fastjgame.agent.ClassReloadAgent;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.core.LogPublisherFactory;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupBuilder;
import com.wjybxx.fastjgame.imp.DefaultGameLogDirector;
import com.wjybxx.fastjgame.kafka.KafkaLogPublisher;
import com.wjybxx.fastjgame.mgr.GameConfigMgr;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.log.GameLogBuilder;
import com.wjybxx.fastjgame.module.*;
import com.wjybxx.fastjgame.scene.SceneRegion;
import com.wjybxx.fastjgame.utils.DebugUtils;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

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
            "warzoneId=" + 1,
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

    /**
     * 网关服参数
     */
    private static final String[] gateArgs = new String[]{
            "platform=" + PlatformType.TEST.name(),
            "serverId=" + 1
    };

    /**
     * 请在启动参数中添加 -Djdk.attach.allowAttachSelf=true ，
     * 否则抛出 java.io.IOException: Can not attach to current VM 异常。
     */
    public static void main(String[] args) throws Exception {
        DebugUtils.openDebug();

        startClassReloadAgent();

        initSlfLoggerConfig();

        initLogPublisherFactory();

        // NET线程数最少1个
        NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupBuilder()
                .setNetEventLoopNum(2)
                .setBossGroupThreadNum(2)
                .setWorkerGroupThreadNum(4)
                .build();

        final GameEventLoopGroupImp gameEventLoopGroup = GameEventLoopGroupImp.newBuilder()
                .setNetEventLoopGroup(netEventLoopGroup)
                .setRejectedExecutionHandler(RejectedExecutionHandlers.log())
                .addWorld(new GateModule(), gateArgs, 20)
                .addWorld(new LoginModule(), loginArgs, 10)
                .addWorld(new WarzoneModule(), warzoneArgs, 10)
                .addWorld(new CenterModule(), centerArgs, 10)
                .addWorld(new SceneModule(), singleSceneArgs, 20)
                .addWorld(new SceneModule(), singleSceneArgs, 20)
                .addWorld(new SceneModule(), crossSceneArgs, 20)
                .addWorld(new SceneModule(), crossSceneArgs, 20)
                .build();

        // 试一试能否安全关闭
        gameEventLoopGroup.awaitTermination(2, TimeUnit.MINUTES);

        gameEventLoopGroup.shutdown();
        netEventLoopGroup.shutdown();
        System.out.println(" ******* invoked shutdown *******");
    }

    /**
     * 启动热更新代理组件
     */
    private static void startClassReloadAgent() throws IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException {
        String pid = getPid();
        VirtualMachine vm = VirtualMachine.attach(pid);
        // jar包必须放在可加载路径
        vm.loadAgent("./game-libs/game-classreloadagent-1.0.jar");

        System.out.println("isRedefineClassesSupported: " + ClassReloadAgent.isRedefineClassesSupported());
        System.out.println("StartUp isModifiableClass: " + ClassReloadAgent.isModifiableClass(StartUp.class));
    }

    private static String getPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    /**
     * 初始化日志组件
     */
    private static void initSlfLoggerConfig() {
        String logDir = new File("").getAbsolutePath() + File.separator + "log";
        String logPath = logDir + File.separator + "fastjgame";
        System.out.println("logPath " + logPath);
        System.setProperty("logPath", logPath);
    }

    /**
     * 初始化日志发布组件(日志搜集组件)
     */
    private static void initLogPublisherFactory() throws IOException {
        final ConfigWrapper gameConfig = GameConfigMgr.loadGameConfig();
        final String kafkaBrokerList = gameConfig.getAsString("kafkaBrokerList");
        assert null != kafkaBrokerList;

        final LogPublisherFactory<GameLogBuilder> factory = () -> {
            return new KafkaLogPublisher<>(new DefaultThreadFactory("LOG-PUBLISHER"),
                    RejectedExecutionHandlers.log(),
                    kafkaBrokerList, new DefaultGameLogDirector());
        };

        GameConfigMgr.setLogPublisherFactory(factory);
    }
}
