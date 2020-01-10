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

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.enummapper.NumericalEnum;
import com.wjybxx.fastjgame.enummapper.NumericalEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * RPC错误码 - 慢慢扩展
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@SerializableClass
public enum RpcErrorCode implements NumericalEnum {

    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * 本地处理请求异常。
     * 本地处理默认返回码，如果可以更细化，则应该细化。
     */
    LOCAL_EXCEPTION(10),

    /**
     * 会话已关闭
     */
    LOCAL_SESSION_CLOSED(11),

    /**
     * 超时
     */
    LOCAL_TIMEOUT(12),

    /**
     * 服务器处理请求失败。
     * 远程默认返回码，如果可以更细化，则应该细化。
     */
    SERVER_EXCEPTION(20),

    /**
     * 路由转发时找不到session
     */
    SERVER_ROUTER_SESSION_NULL(21),
    ;

    /**
     * 唯一标识，不可随意修改
     */
    private final int number;

    RpcErrorCode(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    /**
     * number到枚举的映射
     */
    private static final NumericalEnumMapper<RpcErrorCode> mapper = EnumUtils.mapping(values());

    public static RpcErrorCode forNumber(int number) {
        return mapper.forNumber(number);
    }
}
