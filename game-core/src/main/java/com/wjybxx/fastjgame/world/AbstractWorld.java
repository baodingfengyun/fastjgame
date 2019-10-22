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

package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.misc.MessageMappingStrategy;
import com.wjybxx.fastjgame.misc.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 游戏World的模板实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 12:25
 * github - https://github.com/hl845740757
 */
public abstract class AbstractWorld implements World {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWorld.class);

    protected final WorldWrapper worldWrapper;
    protected final GameEventLoopMgr gameEventLoopMgr;
    protected final ProtocolDispatcherMgr protocolDispatcherMgr;
    protected final WorldTimeMgr worldTimeMgr;
    protected final ProtocolCodecMgr protocolCodecMgr;
    protected final WorldTimerMgr worldTimerMgr;
    protected final HttpDispatcherMgr httpDispatcherMgr;
    protected final WorldInfoMgr worldInfoMgr;
    protected final GlobalExecutorMgr globalExecutorMgr;
    protected final CuratorMgr curatorMgr;
    protected final GameConfigMgr gameConfigMgr;
    protected final GuidMgr guidMgr;
    protected final GameAcceptorMgr gameAcceptorMgr;
    protected final NetContextMgr netContextMgr;

    @Inject
    public AbstractWorld(WorldWrapper worldWrapper) {
        this.worldWrapper = worldWrapper;
        this.gameEventLoopMgr = worldWrapper.getGameEventLoopMgr();
        protocolDispatcherMgr = worldWrapper.getProtocolDispatcherMgr();
        worldTimeMgr = worldWrapper.getWorldTimeMgr();
        protocolCodecMgr = worldWrapper.getProtocolCodecMgr();
        worldTimerMgr = worldWrapper.getWorldTimerMgr();
        httpDispatcherMgr = worldWrapper.getHttpDispatcherMgr();
        worldInfoMgr = worldWrapper.getWorldInfoMgr();
        globalExecutorMgr = worldWrapper.getGlobalExecutorMgr();
        curatorMgr = worldWrapper.getCuratorMgr();
        gameConfigMgr = worldWrapper.getGameConfigMgr();
        guidMgr = worldWrapper.getGuidMgr();
        gameAcceptorMgr = worldWrapper.getGameAcceptorMgr();
        netContextMgr = worldWrapper.getNetContextMgr();
    }

    /**
     * 注册需要的编解码辅助类(序列化类，消息映射的初始化)
     * use {@link #registerProtocolCodec(String, MessageMappingStrategy)} to register.
     */
    protected void registerProtocolCodecs() throws Exception {

    }

    /**
     * 注册codec的模板方法
     *
     * @param name            codec的名字
     * @param mappingStrategy 消息id到消息映射策略
     */
    protected final void registerProtocolCodec(String name, MessageMappingStrategy mappingStrategy) throws Exception {
        protocolCodecMgr.registerProtocolCodec(name, mappingStrategy);
    }

    // --------------------------------- rpc请求、玩家消息、http请求处理器注册--------------------------

    /**
     * 注册玩家消息处理器，主要是scene服注册
     * 使用注解处理器生成的{@code xxxMsgFunRegister}进行注册
     * 也可以在自己的类中使用messageDispatcherMgr自己注册，不一定需要在world中注册。
     */
    protected void registerMessageHandlers() {

    }

    /**
     * 注册rpc请求处理器，服务器之间使用rpc进行通信。
     * 使用注解处理生成的{@code xxxRpcRegister}进行注册。
     */
    protected abstract void registerRpcService();

    /**
     * 注册自己要处理的http请求。后台管理和登录服使用http进行服务。
     * 使用注解处理器生成的{@code xxxHttpRegister}进行注册
     */
    protected abstract void registerHttpRequestHandlers();

    // ----------------------------------------- 接口模板实现 ------------------------------------

    @Override
    public final long worldGuid() {
        return worldInfoMgr.getWorldGuid();
    }

    @Nonnull
    @Override
    public final RoleType worldRole() {
        return worldInfoMgr.getWorldType();
    }

    public final void startUp(ConfigWrapper startArgs) throws Exception {
        // 必须先初始world信息
        worldInfoMgr.init(startArgs);
        // 初始化网络上下文
        netContextMgr.start();

        // 初始化网络层需要的组件(codec帮助类)
        registerProtocolCodecs();

        // 注册要处理的异步普通消息和http请求和同步rpc请求
        registerMessageHandlers();
        registerRpcService();
        registerHttpRequestHandlers();

        // 子类自己的其它启动逻辑
        startHook();

        // 启动成功，时间切换到缓存策略
        worldTimeMgr.changeToCacheStrategy();
    }

    /**
     * 启动游戏服务器
     */
    protected abstract void startHook() throws Exception;

    /**
     * 游戏世界帧
     *
     * @param curMillTime 当前系统时间
     */
    public final void tick(long curMillTime) {
        tickCore(curMillTime);
        tickHook();
    }

    /**
     * 超类tick逻辑
     */
    private void tickCore(long curMillTime) {
        // 优先更新系统时间缓存
        worldTimeMgr.update(curMillTime);
        worldTimerMgr.tick();
    }

    /**
     * 子类tick钩子
     */
    protected abstract void tickHook();

    @Override
    public final void shutdown() {
        // 该方法由GameEventLoop线程在关闭时来调用
        if (!gameEventLoop().isShuttingDown()) {
            gameEventLoop().shutdown();
            return;
        }
        // 关闭期间可能较为耗时，切换到实时策略
        worldTimeMgr.changeToRealTimeStrategy();

        try {
            shutdownHook();
        } catch (Exception e) {
            // 关闭操作和启动操作都是重要操作尽量不要产生异常
            logger.error("shutdown caught exception", e);
        } finally {
            shutdownCore();
        }
    }

    /**
     * 关闭公共服务
     */
    private void shutdownCore() {
        netContextMgr.shutdown();
        curatorMgr.shutdown();
        protocolDispatcherMgr.release();
        httpDispatcherMgr.release();
    }

    /**
     * 子类自己的关闭动作
     */
    protected abstract void shutdownHook() throws Exception;

    /**
     * 获取world绑定到的{@link GameEventLoop}
     *
     * @return EventLoop
     */
    @Nonnull
    protected final GameEventLoop gameEventLoop() {
        return gameEventLoopMgr.getEventLoop();
    }

}
