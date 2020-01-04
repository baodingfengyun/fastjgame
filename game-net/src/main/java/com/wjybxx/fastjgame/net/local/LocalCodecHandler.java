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

package com.wjybxx.fastjgame.net.local;

import com.wjybxx.fastjgame.misc.RpcCall;
import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.session.SessionOutboundHandlerAdapter;
import com.wjybxx.fastjgame.utils.NetUtils;

import java.io.IOException;

/**
 * 对于在JVM内传输的数据，进行保护性拷贝。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public class LocalCodecHandler extends SessionOutboundHandlerAdapter {

    private ProtocolCodec codec;

    public LocalCodecHandler() {

    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        codec = ctx.session().config().codec();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        // msg 是根据writeTask创建的对象，不是共享的
        if (msg instanceof RpcRequestMessage) {
            // rpc请求
            RpcRequestMessage rpcRequestMessage = (RpcRequestMessage) msg;
            // 拷贝body
            rpcRequestMessage.setRequest(cloneBody(rpcRequestMessage.getRequest()));
        } else if (msg instanceof RpcResponseMessage) {
            // rpc响应
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcResponse rpcResponse = responseMessage.getRpcResponse();
            final RpcResponse copiedRpcResponse = new RpcResponse(rpcResponse.getResultCode(), codec.cloneObject(rpcResponse.getBody()));
            responseMessage.setRpcResponse(copiedRpcResponse);
        } else if (msg instanceof OneWayMessage) {
            // 单向消息
            OneWayMessage oneWayMessage = (OneWayMessage) msg;
            // 拷贝body
            oneWayMessage.setMessage(cloneBody(oneWayMessage.getMessage()));
        }
        // 传递给下一个handler
        ctx.fireWrite(msg);
    }

    /**
     * 拷贝消息内容
     */
    private Object cloneBody(Object body) throws IOException {
        if (body instanceof RpcCall) {
            // 检查延迟序列化和预反序列化
            final RpcCall<?> rpcCall0 = NetUtils.checkLazySerialize((RpcCall<?>) body, codec);
            final RpcCall<?> rpcCall1 = NetUtils.checkPreDeserialize(rpcCall0, codec);
            return codec.cloneObject(rpcCall1);
        } else {
            return codec.cloneObject(body);
        }
    }
}
