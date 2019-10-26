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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Rpc响应结果。
 * 注意：这是RPC调用的结果，一定不能使用 == 判断相等！！！
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public final class RpcResponse {

    // 这些常量仅仅是为了减少对象创建，但是你需要谨记：这是RPC调用的结果，一定不能使用 == 判断相等！！！
    /**
     * 执行成功但是没有返回值的body
     */
    public static final RpcResponse SUCCESS = newSucceedResponse(null);

    public static final RpcResponse SESSION_NULL = newFailResponse(RpcResultCode.SESSION_NULL);
    public static final RpcResponse SESSION_CLOSED = newFailResponse(RpcResultCode.SESSION_CLOSED);

    public static final RpcResponse CANCELLED = newFailResponse(RpcResultCode.CANCELLED);
    public static final RpcResponse TIMEOUT = newFailResponse(RpcResultCode.TIMEOUT);

    public static final RpcResponse FORBID = newFailResponse(RpcResultCode.FORBID);
    public static final RpcResponse BAD_REQUEST = newFailResponse(RpcResultCode.BAD_REQUEST);

    public static final RpcResponse ERROR = newFailResponse(RpcResultCode.ERROR);

    /**
     * 结果标识
     */
    private final RpcResultCode resultCode;
    /**
     * rpc响应结果，可能为null
     */
    private final Object body;

    public RpcResponse(@Nonnull RpcResultCode resultCode, @Nullable Object body) {
        this.resultCode = resultCode;
        this.body = body;
    }

    public RpcResultCode getResultCode() {
        return resultCode;
    }

    @Nullable
    public Object getBody() {
        return body;
    }

    public boolean isSuccess() {
        return resultCode == RpcResultCode.SUCCESS;
    }

    public boolean isFailure() {
        return resultCode != RpcResultCode.SUCCESS;
    }

    public static RpcResponse newFailResponse(@Nonnull RpcResultCode resultCode) {
        return new RpcResponse(resultCode, null);
    }

    public static RpcResponse newSucceedResponse(@Nullable Object body) {
        return new RpcResponse(RpcResultCode.SUCCESS, body);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        RpcResponse that = (RpcResponse) object;

        return new EqualsBuilder()
                .append(resultCode, that.resultCode)
                .append(body, that.body)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(resultCode)
                .append(body)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "resultCode=" + resultCode +
                ", body=" + body +
                '}';
    }
}
