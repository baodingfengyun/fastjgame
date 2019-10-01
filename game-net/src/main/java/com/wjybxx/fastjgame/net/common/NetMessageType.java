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

import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * 网络包类型
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public enum NetMessageType implements NumberEnum {

    /**
     * 客户端请求建立链接
     */
    CONNECT_REQUEST(1),
    /**
     * 服务器通知建立连接结果(验证结果)
     */
    CONNECT_RESPONSE(2),

    /**
     * Rpc请求包，必须有一个响应。 -- Rpc消息使用protoBuf编解码，内部使用。
     */

    RPC_REQUEST(3),
    /**
     * Rpc响应包。
     */
    RPC_RESPONSE(4),

    /**
     * 单向消息包。
     */
    ONE_WAY_MESSAGE(5),

    /**
     * 心跳包
     */
    PING_PONG(6),

    /**
     * 主动断开连接
     */
    DISCONNECT(8),

    /**
     * 重定向 - 让网关服连接另一个服务器
     */
    RELOCATION(9);

    public final byte pkgType;

    NetMessageType(int pkgType) {
        this.pkgType = (byte) pkgType;
    }

    /**
     * 仅仅用于初始化映射
     *
     * @return 枚举对应的编号
     */
    @Override
    public int getNumber() {
        return pkgType;
    }

    /**
     * 排序号的枚举数组，方便查找
     */
    private static final NumberEnumMapper<NetMessageType> mapper = EnumUtils.indexNumberEnum(values());

    /**
     * 通过网络包中的pkgType找到对应的枚举。
     *
     * @param pkgType 包类型
     * @return 包类型对应的枚举
     */
    public static NetMessageType forNumber(byte pkgType) {
        return mapper.forNumber(pkgType);
    }
}
