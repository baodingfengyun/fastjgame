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

import com.wjybxx.fastjgame.net.misc.*;
import com.wjybxx.fastjgame.net.serializer.BeanInputStream;
import com.wjybxx.fastjgame.net.serializer.BeanOutputStream;
import com.wjybxx.fastjgame.net.serializer.BeanSerializer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Rpc响应结果。
 * 注意：这是RPC调用的结果，一定不能使用 == 判断相等！！！
 * {@link RpcResponseSerializer}负责解析该类
 *
 * @author wjybxx
 * @version 1.1
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public final class RpcResponse {

    // 这些常量仅仅是为了减少对象创建，但是你需要谨记：这是RPC调用的结果，一定不能使用 == 判断相等！！！
    /**
     * 执行成功但是没有返回值的body
     */
    public static final RpcResponse SUCCESS = newSucceedResponse(null);

    /**
     * 结果标识 - 错误码
     */
    private final RpcErrorCode errorCode;
    /**
     * rpc响应结果。
     * 如果{@link #errorCode}为{@link RpcErrorCode#SUCCESS}，则body为对应的结果(null可能是个正常的结果)。
     * 否则body为对应的错误信息(String)(应该非null)。
     */
    private final Object body;

    public RpcResponse(@Nonnull RpcErrorCode errorCode, @Nullable Object body) {
        this.errorCode = errorCode;
        this.body = body;
    }

    public RpcErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getBody() {
        return body;
    }

    public boolean isSuccess() {
        return errorCode == RpcErrorCode.SUCCESS;
    }

    public boolean isFailure() {
        return errorCode != RpcErrorCode.SUCCESS;
    }

    public static RpcResponse newFailResponse(@Nonnull RpcErrorCode errorCode, @Nonnull String message) {
        return new RpcResponse(errorCode, message);
    }

    public static RpcResponse newSucceedResponse(@Nullable Object body) {
        return new RpcResponse(RpcErrorCode.SUCCESS, body);
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
                .append(errorCode, that.errorCode)
                .append(body, that.body)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(errorCode)
                .append(body)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "errorCode=" + errorCode +
                ", body=" + body +
                '}';
    }

    /**
     * 负责编解码{@link RpcResponse}
     */
    private static class RpcResponseSerializer implements BeanSerializer<RpcResponse> {

        @Override
        public void writeFields(RpcResponse instance, BeanOutputStream outputStream) throws IOException {
            outputStream.writeField(WireType.CUSTOM_ENTITY, instance.errorCode);
            outputStream.writeField(WireType.RUN_TIME, instance.body);
        }

        @Override
        public RpcResponse read(BeanInputStream inputStream) throws IOException {
            final RpcErrorCode errorCode = inputStream.readField(WireType.CUSTOM_ENTITY);
            final Object body = inputStream.readField(WireType.RUN_TIME);
            return new RpcResponse(errorCode, body);
        }

        @Override
        public RpcResponse clone(RpcResponse instance, BeanCloneUtil util) throws IOException {
            return new RpcResponse(instance.errorCode, util.cloneField(WireType.RUN_TIME, instance.body));
        }
    }
}
