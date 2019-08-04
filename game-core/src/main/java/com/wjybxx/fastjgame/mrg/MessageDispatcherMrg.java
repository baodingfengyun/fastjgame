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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.OneWayMessageHandler;
import com.wjybxx.fastjgame.misc.RpcRequestHandler1;
import com.wjybxx.fastjgame.misc.RpcRequestHandler2;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 实现TCP/Ws长链接的 [单向消息] 和 [rpc请求] 的分发。
 *
 * 不算标准的RPC，但是其本质是rpc，只不过相当于我们限定了方法参数只能是一个，不同的请求参数对应不同的方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class MessageDispatcherMrg implements MessageHandler {

    private static final Logger logger= LoggerFactory.getLogger(MessageDispatcherMrg.class);

    /**
     * 单向消息处理器
     */
    private final Map<Class<?>, OneWayMessageHandler<?>> messageHandlerMap = new IdentityHashMap<>();

    /**
     * Rpc请求处理器
     */
    private final Map<Class<?>, RpcHandlerWrapper<?>> requestHandlerMap = new IdentityHashMap<>();

    @Inject
    public MessageDispatcherMrg() {

    }

    public <T> void registerMessageHandler(@Nonnull Class<T> messageClazz, @Nonnull OneWayMessageHandler<? super T> messageHandler) {
        CollectionUtils.requireNotContains(messageHandlerMap, messageClazz, "messageClazz");
        messageHandlerMap.put(messageClazz, messageHandler);
    }

    public <T,R> void registerMessageHandler(@Nonnull Class<T> requestClazz, @Nonnull RpcRequestHandler1<? super T, R> rpcRequestHandler) {
        CollectionUtils.requireNotContains(requestHandlerMap, requestClazz, "requestClazz");
        requestHandlerMap.put(requestClazz, new RpcHandlerWrapper<>(rpcRequestHandler, null));
    }

    public <T> void registerMessageHandler(@Nonnull Class<T> requestClazz, @Nonnull RpcRequestHandler2<? super T> rpcRequestHandler) {
        CollectionUtils.requireNotContains(requestHandlerMap, requestClazz, "requestClazz");
        requestHandlerMap.put(requestClazz, new RpcHandlerWrapper<>(null, rpcRequestHandler));
    }

    // ---------------------------------------------- 消息、Rpc请求分发 ----------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(Session session, @Nullable Object message) throws Exception {
        // 未成功解码的消息，做个记录并丢弃(不影响其它请求)
        if (null == message){
            logger.warn("roleType={} sessionId={} send null message", session.remoteRole(), session.remoteGuid());
            return;
        }
        // 未注册的消息，做个记录并丢弃(不影响其它请求)
        OneWayMessageHandler messageHandler = messageHandlerMap.get(message.getClass());
        if (null == messageHandler){
            logger.warn("roleType={} sessionId={} send unregistered message {}",
                    session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName());
            return;
        }
        // 分发请求
        try {
            messageHandler.onMessage(session, message);
        } catch (Exception e) {
            logger.warn("handle message caught exception,sessionId={},roleType={},message clazz={}",
                    session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName(),e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRpcRequest(Session session, @Nullable Object request, RpcResponseChannel responseChannel) throws Exception {
        // 未成功解码的消息，做个记录并丢弃(不影响其它请求)
        if (null == request){
            logger.warn("roleType={} sessionId={} send null message", session.remoteRole(), session.remoteGuid());
            return;
        }
        // 未注册的消息，做个记录并丢弃(不影响其它请求)
        RpcHandlerWrapper handlerWrapper = requestHandlerMap.get(request.getClass());
        if (null == handlerWrapper){
            logger.warn("roleType={} sessionId={} send unregistered message {}",
                    session.remoteRole(), session.remoteGuid(), request.getClass().getSimpleName());
            return;
        }
        // 分发请求
        try {
            if (handlerWrapper.handler1 != null) {
                // 返回成功结果
                Object result = handlerWrapper.handler1.onRequest(session, request);
                responseChannel.writeSuccess(result);
            } else {
                assert handlerWrapper.handler2 != null;
                // 处理器自己决定什么时候返回结果
                handlerWrapper.handler2.onRequest(session, request, responseChannel);
            }
        } catch (Exception e) {
            responseChannel.writeFailure(RpcResultCode.ERROR);
            logger.warn("handle message caught exception,sessionId={},roleType={},message clazz={}",
                    session.remoteRole(), session.remoteGuid(), request.getClass().getSimpleName(),e);
        }
    }

    private static class RpcHandlerWrapper<T> {
        /** 1类型rpc处理器，立即返回结果 */
        private final RpcRequestHandler1<? super T,?> handler1;
        /** 2类型rpc处理器，不一定能立即返回结果 */
        private final RpcRequestHandler2<? super T> handler2;

        private RpcHandlerWrapper(@Nullable RpcRequestHandler1<? super T, ?> handler1, @Nullable RpcRequestHandler2<? super T> handler2) {
            this.handler1 = handler1;
            this.handler2 = handler2;
        }
    }
}
