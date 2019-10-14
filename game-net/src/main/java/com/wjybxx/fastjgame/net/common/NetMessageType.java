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

package com.wjybxx.fastjgame.net.common;

/**
 * 网络包类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public enum NetMessageType {

    /**
     * 客户端请求建立链接
     */
    CONNECT_REQUEST(1),
    /**
     * 服务器通知建立连接结果(验证结果)
     */
    CONNECT_RESPONSE(2),

    /**
     * 心跳包
     */
    PING_PONG(3),

    /**
     * Rpc请求包，必须有一个响应。 -- Rpc消息使用protoBuf编解码，内部使用。
     */
    RPC_REQUEST(4),
    /**
     * Rpc响应包。
     */
    RPC_RESPONSE(5),

    /**
     * 单向消息包。
     */
    ONE_WAY_MESSAGE(6);

    public final byte pkgType;

    NetMessageType(int pkgType) {
        this.pkgType = (byte) pkgType;
    }

    /**
     * 通过网络包中的pkgType找到对应的枚举。
     *
     * @param pkgType 包类型
     * @return 包类型对应的枚举
     */
    public static NetMessageType forNumber(byte pkgType) {
        return values()[pkgType - 1];
    }
}
