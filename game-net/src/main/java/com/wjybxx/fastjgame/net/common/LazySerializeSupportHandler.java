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

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 延迟序列化和提前反序列化处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/8
 * github - https://github.com/hl845740757
 */
public class LazySerializeSupportHandler extends SessionDuplexHandlerAdapter {

    private ProtocolCodec codec;

    public LazySerializeSupportHandler() {
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        codec = ctx.session().config().codec();
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NetLogicMessage) {
            final NetLogicMessage logicMessage = (NetLogicMessage) msg;
            final Object newBody = checkLazySerialize(logicMessage.getBody());
            logicMessage.setBody(newBody);
        }
        ctx.fireWrite(msg);
    }

    /**
     * 检查延迟初始化参数
     *
     * @return newCall or the same call
     * @throws IOException error
     */
    private Object checkLazySerialize(Object body) throws Exception {
        if (!(body instanceof RpcCall)) {
            return body;
        }

        final RpcCall<?> rpcCall = (RpcCall<?>) body;
        final int lazyIndexes = rpcCall.getLazyIndexes();
        if (lazyIndexes <= 0) {
            return rpcCall;
        }

        // bugs: 如果不创建新的list，则可能出现并发修改的情况，可能导致部分线程看见错误的数据
        // 解决方案有：①copyOnWrite ②对RpcCall对象加锁
        // 选择copyOnWrite的理由：①使用延迟序列化和提前反序列化的比例并不高 ②方法方法参数个数偏小，创建一个小list的成本较低。
        final List<Object> methodParams = rpcCall.getMethodParams();
        final ArrayList<Object> newMethodParams = new ArrayList<>(methodParams.size());

        for (int index = 0, end = methodParams.size(); index < end; index++) {
            final Object parameter = methodParams.get(index);
            final Object newParameter;

            if ((lazyIndexes & (1L << index)) != 0 && !(parameter instanceof byte[])) {
                newParameter = codec.serializeToBytes(parameter);
            } else {
                newParameter = parameter;
            }

            newMethodParams.add(newParameter);
        }
        return new RpcCall<>(rpcCall.getServiceId(), rpcCall.getMethodId(), newMethodParams, 0, rpcCall.getPreIndexes());
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NetLogicMessage) {
            final NetLogicMessage logicMessage = (NetLogicMessage) msg;
            final Object newBody = checkPreDeserialize(logicMessage.getBody());
            logicMessage.setBody(newBody);
        }
        ctx.fireRead(msg);
    }

    /**
     * 检查提前反序列化参数
     *
     * @return newCall or the same call
     * @throws IOException error
     */
    private Object checkPreDeserialize(Object body) throws Exception {
        if (!(body instanceof RpcCall)) {
            return body;
        }

        final RpcCall<?> rpcCall = (RpcCall<?>) body;
        final int preIndexes = rpcCall.getPreIndexes();
        if (preIndexes <= 0) {
            return rpcCall;
        }

        // 线程安全问题同write
        final List<Object> methodParams = rpcCall.getMethodParams();
        final ArrayList<Object> newMethodParams = new ArrayList<>(methodParams.size());

        for (int index = 0, end = methodParams.size(); index < end; index++) {
            final Object parameter = methodParams.get(index);
            final Object newParameter;
            if ((preIndexes & (1L << index)) != 0 && parameter instanceof byte[]) {
                newParameter = codec.deserializeFromBytes((byte[]) parameter);
            } else {
                newParameter = parameter;
            }
            newMethodParams.add(newParameter);
        }
        return new RpcCall<>(rpcCall.getServiceId(), rpcCall.getMethodId(), newMethodParams, rpcCall.getLazyIndexes(), 0);
    }
}
