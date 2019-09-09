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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * RPC结果码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
public enum RpcResultCode implements NumberEnum {

    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * Session不存在
     */
    SESSION_NULL(10),
    /**
     * 会话已关闭
     */
    SESSION_CLOSED(11),

    /**
     * 用户取消了rpc调用
     */
    CANCELLED(21),
    /**
     * 请求超时
     */
    TIMEOUT(22),
    /**
     * 出现异常(本地异常)，有body，body在本地赋值，不序列化。
     * 如果需要查看异常，可以获取body。
     */
    LOCAL_EXCEPTION(23),

    /**
     * 请求被禁止
     */
    FORBID(41),
    /**
     * 请求错误
     */
    BAD_REQUEST(42),

    /**
     * 错误(对方执行请求时发生错误)，现阶段不返回额外信息。
     * (注解处理器使用了该对象，不要轻易重命名)
     */
    ERROR(51),

    ;

    /**
     * 唯一标识，不可随意修改
     */
    private final int number;

    RpcResultCode(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    /**
     * number到枚举的映射
     */
    private static final NumberEnumMapper<RpcResultCode> mapper = EnumUtils.indexNumberEnum(values());

    public static RpcResultCode forNumber(int number) {
        return mapper.forNumber(number);
    }
}
