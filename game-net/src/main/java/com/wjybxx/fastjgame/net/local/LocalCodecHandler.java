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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public void init(SessionHandlerContext ctx) throws Exception {
        codec = ctx.session().config().codec();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
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
     * 这里是为了解决一件事情：一个非字节数组对象，序列化为字节数组后，再进行拷贝是冗余的，序列化已经实现了拷贝（安全性）。
     * 不过增加了复杂度，降低了可维护性。
     */
    @SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
    private Object cloneBody(Object body) throws IOException {
        // 检查延迟序列化的属性
        if (body instanceof RpcCall) {
            final RpcCall rpcCall = (RpcCall) body;
            final int lazyIndexes = rpcCall.getLazyIndexes();
            if (lazyIndexes <= 0) {
                // 拷贝参数列表即可
                return new RpcCall(rpcCall.getMethodKey(), (List<Object>) codec.cloneObject(rpcCall.getMethodParams()), lazyIndexes);
            }
            // 逐个检查
            final RpcCall newCall = new RpcCall(rpcCall.getMethodKey(), new ArrayList<>(rpcCall.getMethodParams().size()), lazyIndexes);
            for (int index = 0, end = rpcCall.getMethodParams().size(); index < end; index++) {
                final Object parameter = rpcCall.getMethodParams().get(index);
                final Object newParameter;
                if ((lazyIndexes & (1L << index)) != 0) {
                    // 需要延迟初始化的属性
                    if (parameter instanceof byte[]) {
                        // 已经是字节数组了，拷贝一下即可
                        byte[] bytesParameter = (byte[]) parameter;
                        newParameter = new byte[bytesParameter.length];
                        System.arraycopy(bytesParameter, 0, newParameter, 0, bytesParameter.length);
                    } else {
                        // 还不是字节数组，执行序列化，减少不必要的拷贝
                        newParameter = codec.serializeToBytes(parameter);
                    }
                } else {
                    // 不需要延迟初始化的属性，进行显式拷贝
                    newParameter = codec.cloneObject(parameter);
                }
                newCall.getMethodParams().add(newParameter);
            }
            return newCall;
        } else {
            return codec.cloneObject(body);
        }
    }
}
