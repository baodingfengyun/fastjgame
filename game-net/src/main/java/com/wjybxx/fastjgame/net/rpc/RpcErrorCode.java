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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.utils.EnumUtils;
import com.wjybxx.fastjgame.utils.dsl.IndexableEnum;
import com.wjybxx.fastjgame.utils.dsl.IndexableEnumMapper;

import javax.annotation.Nonnull;

/**
 * RPC错误码 - 慢慢扩展
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
public enum RpcErrorCode implements IndexableEnum {

    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * 无法识别的错误码(当找不到错误码时，返回该值)
     */
    UNKNOWN(1),

    /**
     * 本地处理请求异常。
     * 本地处理默认返回码，如果可以更细化，则应该细化。
     */
    LOCAL_EXCEPTION(10),

    /**
     * 找不到对应的session
     */
    LOCAL_SESSION_NOT_FOUND(11),
    /**
     * 会话已关闭
     */
    LOCAL_SESSION_CLOSED(12),

    /**
     * 超时
     */
    LOCAL_TIMEOUT(13),

    /**
     * 服务器处理请求失败。
     * 远程默认返回码，如果可以更细化，则应该细化。
     */
    SERVER_EXCEPTION(20);

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

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * number到枚举的映射
     */
    private static final IndexableEnumMapper<RpcErrorCode> mapper = EnumUtils.mapping(values());

    @Nonnull
    public static RpcErrorCode forNumber(int number) {
        final RpcErrorCode errorCode = mapper.forNumber(number);
        return errorCode == null ? UNKNOWN : errorCode;
    }
}
