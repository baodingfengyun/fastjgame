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
import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * RPC错误码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@SerializableClass
public enum RpcErrorCode implements NumberEnum {

    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * Session不存在
     */
    SESSION_NULL(1),
    /**
     * 会话已关闭
     */
    SESSION_CLOSED(2),

    /**
     * 出现异常(本地异常)。
     * 如果需要查看异常，可以获取body。
     */
    LOCAL_EXCEPTION(3),
    /**
     * 服务器处理请求失败时失败。
     * (注解处理器使用了该对象，不要轻易重命名)
     */
    SERVER_EXCEPTION(4),

    /**
     * 路由转发时找不到session
     */
    ROUTER_SESSION_NULL(5),
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
    private static final NumberEnumMapper<RpcErrorCode> mapper = EnumUtils.mapping(values());

    public static RpcErrorCode forNumber(int number) {
        return mapper.forNumber(number);
    }
}
