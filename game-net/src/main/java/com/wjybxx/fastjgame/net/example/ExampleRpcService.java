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

package com.wjybxx.fastjgame.net.example;

import com.wjybxx.fastjgame.net.rpc.*;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * 示例rpcService
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/26
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = 32700)
public class ExampleRpcService {

    @RpcMethod(methodId = 0)
    public void sync() {

    }

    @RpcMethod(methodId = 1)
    public void hello(String name) {
        System.out.println(name);
    }

    @RpcMethod(methodId = 2)
    public int queryId(String name) {
        return name.hashCode();
    }

    @RpcMethod(methodId = 3)
    public int inc(final int number) {
        return number + 1;
    }

    /**
     * @param number  待加的数
     * @param session 会话信息。
     *                该参数不会出现在客户端的代理中，Session参数可以出现在任意位置，注解处理器会处理，不要求在特定位置
     * @return 增加后的值
     */
    @RpcMethod(methodId = 4)
    public int incWithSession(final int number, Session session) {
        return number + 2;
    }

    /**
     * @param number          待加的数
     * @param responseChannel 返回结果的通道，表示该方法可能不能立即返回结果，需要持有channel以便在未来返回结果。
     *                        该参数不会出现在客户端的代理中，Channel参数可以出现在任意位置，注解处理器会处理，不要求在特定位置
     */
    @RpcMethod(methodId = 5)
    public void incWithChannel(final int number, RpcResponseChannel<Integer> responseChannel) {
        responseChannel.writeSuccess(number + 3);
    }

    @RpcMethod(methodId = 6)
    public void incWithSessionAndChannel(Session session, final int number, RpcResponseChannel<Integer> responseChannel) {
        responseChannel.writeSuccess(number + 4);
    }

    @RpcMethod(methodId = 7)
    public void notifySuccess(long id) {
        System.out.println(id);
    }

    @RpcMethod(methodId = 8)
    public String combine(String prefix, String content) {
        return prefix + "-" + content;
    }

    @RpcMethod(methodId = 9)
    public ExampleMessages.FullMessage echo(ExampleMessages.FullMessage message) {
        return message;
    }

    /**
     * 模拟场景服务器将消息通过网关发送给玩家
     *
     * @param playerGuid 玩家标识
     * @param proto      生成的代理方法类型为Object
     */
    @RpcMethod(methodId = 10)
    public void sendToPlayer(long playerGuid, @LazySerializable byte[] proto) throws Exception {
        System.out.println("playerGuid " + playerGuid + ", " + ExampleConstants.binaryCodec.fromBytes(proto));
    }

    /**
     * 模拟玩家将消息通过网关发送到场景服务器
     *
     * @param playerGuid 玩家标识
     * @param msg        玩家发来的消息
     */
    @RpcMethod(methodId = 11)
    public void sendToScene(long playerGuid, @PreDeserializable String msg) {
        System.out.println("playerGuid " + playerGuid + ", " + msg);
    }

    /**
     * 合并字符串，测试变长参数
     */
    @RpcMethod(methodId = 12)
    public String join(String... params) {
        return String.join(",", params);
    }
}
