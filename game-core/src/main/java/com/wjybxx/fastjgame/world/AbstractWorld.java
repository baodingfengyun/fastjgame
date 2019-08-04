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
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.misc.OneWayMessageHandler;
import com.wjybxx.fastjgame.misc.RpcRequestHandler1;
import com.wjybxx.fastjgame.misc.RpcRequestHandler2;
import com.wjybxx.fastjgame.mrg.*;
import com.wjybxx.fastjgame.net.HttpRequestHandler;
import com.wjybxx.fastjgame.net.MessageMappingStrategy;
import com.wjybxx.fastjgame.net.MessageSerializer;
import com.wjybxx.fastjgame.net.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 游戏World的模板实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 12:25
 * github - https://github.com/hl845740757
 */
public abstract class AbstractWorld implements World{

    private static final Logger logger = LoggerFactory.getLogger(AbstractWorld.class);

    protected final WorldWrapper worldWrapper;
    protected final GameEventLoopMrg gameEventLoopMrg;
    protected final MessageDispatcherMrg messageDispatcherMrg;
    protected final SystemTimeMrg systemTimeMrg;
    protected final CodecHelperMrg codecHelperMrg;
    protected final TimerMrg timerMrg;
    protected final HttpDispatcherMrg httpDispatcherMrg;
    protected final WorldInfoMrg worldInfoMrg;
    protected final GlobalExecutorMrg globalExecutorMrg;
    protected final CuratorMrg curatorMrg;
    protected final GameConfigMrg gameConfigMrg;
    protected final GuidMrg guidMrg;
    protected final InnerAcceptorMrg innerAcceptorMrg;
    protected final NetContextManager netContextManager;

    @Inject
    public AbstractWorld(WorldWrapper worldWrapper) {
        this.worldWrapper = worldWrapper;
        this.gameEventLoopMrg = worldWrapper.getGameEventLoopMrg();
        messageDispatcherMrg = worldWrapper.getMessageDispatcherMrg();
        systemTimeMrg = worldWrapper.getSystemTimeMrg();
        codecHelperMrg = worldWrapper.getCodecHelperMrg();
        timerMrg = worldWrapper.getTimerMrg();
        httpDispatcherMrg = worldWrapper.getHttpDispatcherMrg();
        worldInfoMrg = worldWrapper.getWorldInfoMrg();
        globalExecutorMrg = worldWrapper.getGlobalExecutorMrg();
        curatorMrg = worldWrapper.getCuratorMrg();
        gameConfigMrg = worldWrapper.getGameConfigMrg();
        guidMrg = worldWrapper.getGuidMrg();
        innerAcceptorMrg = worldWrapper.getInnerAcceptorMrg();
        netContextManager = worldWrapper.getNetContextManager();
    }

    /**
     * 注册需要的编解码辅助类(序列化类，消息映射的初始化)
     * use {@link #registerCodecHelper(String, MessageMappingStrategy, MessageSerializer)} to register.
     */
    protected void registerCodecHelpers() throws Exception {

    }

    /**
     * 注册codec的模板方法
     * @param name codec的名字
     * @param mappingStrategy 消息id到消息映射策略
     * @param messageSerializer 消息序列化反序列化实现类
     */
    protected final void registerCodecHelper(String name, MessageMappingStrategy mappingStrategy, MessageSerializer messageSerializer) throws Exception {
        codecHelperMrg.registerCodecHelper(name,mappingStrategy,messageSerializer);
    }

    // --------------------------------- 消息、请求处理器注册，未来可以考虑使用注解和注解处理器自动注册(生成代码) --------------------------

    /**
     * 注册自己要处理的消息。也可以在自己的类中使用messageDispatcherMrg自己注册，不一定需要在world中注册。
     * use {@link #registerMessageHandler(Class, OneWayMessageHandler)} to register
     */
    protected abstract void registerMessageHandlers();

    /**
     * 注册客户端发来的消息的处理器
     * @param messageClazz 消息类
     * @param messageHandler 消息处理器
     * @param <T> 消息类型
     */
    protected final <T> void registerMessageHandler(Class<T> messageClazz, OneWayMessageHandler<? super T> messageHandler){
        messageDispatcherMrg.registerMessageHandler(messageClazz, messageHandler);
    }

    /**
     * 注册rpc请求处理器。
     * use {@link #registerRpcRequestHandler(Class, RpcRequestHandler1)}and
     * {@link #registerRpcRequestHandler(Class, RpcRequestHandler2)} to register.
     */
    protected abstract void registerRpcRequestHandlers();

    /**
     * @param requestClazz rpc请求内容
     * @param rpcRequestHandler 请求处理器
     * @param <T> rpc请求类型
     * @param <R> 结果类型
     */
    protected final <T,R> void registerRpcRequestHandler(@Nonnull Class<T> requestClazz, @Nonnull RpcRequestHandler1<? super T, R> rpcRequestHandler) {
        messageDispatcherMrg.registerMessageHandler(requestClazz, rpcRequestHandler);
    }

    /**
     * @param requestClazz rpc请求内容
     * @param rpcRequestHandler 请求处理器
     * @param <T> rpc请求类型
     */
    protected final <T> void registerRpcRequestHandler(@Nonnull Class<T> requestClazz, @Nonnull RpcRequestHandler2<? super T> rpcRequestHandler) {
        messageDispatcherMrg.registerMessageHandler(requestClazz, rpcRequestHandler);
    }

    /**
     * 注册自己要处理的http请求
     * use {@link #registerHttpRequestHandler(String, HttpRequestHandler)} to register
     */
    protected abstract void registerHttpRequestHandlers();

    /**
     * 注册http请求处理器
     * @param path http请求路径
     * @param httpRequestHandler 对应的处理器
     */
    protected final void registerHttpRequestHandler(String path, HttpRequestHandler httpRequestHandler){
        httpDispatcherMrg.registerHandler(path,httpRequestHandler);
    }

    // ----------------------------------------- 接口模板实现 ------------------------------------


    @Override
    public final long worldGuid() {
        return worldInfoMrg.getWorldGuid();
    }

    @Nonnull
    @Override
    public final RoleType worldRole() {
        return worldInfoMrg.getWorldType();
    }

    public final void startUp(GameEventLoop eventLoop, NetEventLoopGroup netEventLoopGroup) throws Exception{
        gameEventLoopMrg.setEventLoop(eventLoop);
        netContextManager.setNetEventLoopGroup(netEventLoopGroup);

        // 初始化网络层需要的组件(codec帮助类)
        registerCodecHelpers();

        // 注册要处理的异步普通消息和http请求和同步rpc请求
        registerMessageHandlers();
        registerRpcRequestHandlers();
        registerHttpRequestHandlers();

        // 子类自己的其它启动逻辑
        startHook();

        // 启动成功，时间切换到缓存策略
        systemTimeMrg.changeToCacheStrategy();
    }

    /**
     * 启动游戏服务器
     */
    protected abstract void startHook() throws Exception;

    /**
     * 游戏世界帧
     * @param curMillTime 当前系统时间
     */
    public final void tick(long curMillTime){
        tickCore(curMillTime);

        tickHook();
    }

    /**
     * 超类tick逻辑
     */
    private void tickCore(long curMillTime){
        // 优先更新系统时间缓存
        systemTimeMrg.update(curMillTime);
        timerMrg.tickTrigger();
    }

    /**
     * 子类tick钩子
     */
    protected abstract void tickHook();

    @Override
    public final void shutdown() throws Exception {
        // 关闭期间可能较为耗时，切换到实时策略
        systemTimeMrg.changeToRealTimeStrategy();

        try {
            shutdownHook();
        } catch (Exception e){
            // 关闭操作和启动操作都是重要操作尽量不要产生异常
            logger.error("shutdown caught exception",e);
        }finally {
            shutdownCore();
        }
    }

    /**
     * 关闭公共服务
     */
    private void shutdownCore(){
        curatorMrg.shutdown();
        globalExecutorMrg.shutdown();
    }

    /**
     * 子类自己的关闭动作
     */
    protected abstract void shutdownHook();

    @Nonnull
    @Override
    public GameEventLoop gameEventLoop() {
        return gameEventLoopMrg.getEventLoop();
    }

    @Override
    public ListenableFuture<?> deregister() {
        return gameEventLoopMrg.getEventLoop().deregisterWorld(worldGuid());
    }

}
