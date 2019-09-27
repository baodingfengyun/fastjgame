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

package com.wjybxx.fastjgame.net.injvm;

import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.pipeline.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.pipeline.SessionHandlerContext;

/**
 * 对于在JVM内传输的数据，进行保护性拷贝。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
public class JVMCodecHandler extends SessionDuplexHandlerAdapter {

    private final ProtocolCodec protocolCodec;

    public JVMCodecHandler(ProtocolCodec protocolCodec) {
        this.protocolCodec = protocolCodec;
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcRequestMessage) {
            // rpc请求
            RpcRequestMessage rpcRequestMessage = (RpcRequestMessage) msg;
            rpcRequestMessage.setRequest(protocolCodec.cloneRpcRequest(rpcRequestMessage.getRequest()));
        } else if (msg instanceof OneWayMessage) {
            // 单向消息
            OneWayMessage oneWayMessage = (OneWayMessage) msg;
            oneWayMessage.setMessage(protocolCodec.cloneMessage(oneWayMessage));
        } else if (msg instanceof RpcResponseMessage) {
            // rpc响应
            RpcResponseMessage responseMessage = (RpcResponseMessage) msg;
            final RpcResponse rpcResponse = responseMessage.getRpcResponse();
            final RpcResponse copiedRpcResponse = new RpcResponse(rpcResponse.getResultCode(), protocolCodec.cloneRpcResponse(rpcResponse.getBody()));
            responseMessage.setRpcResponse(copiedRpcResponse);
        }
        // 传递给下一个handler
        ctx.write(msg);
    }
}
