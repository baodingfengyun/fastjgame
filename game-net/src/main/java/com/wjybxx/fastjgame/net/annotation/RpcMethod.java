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

package com.wjybxx.fastjgame.net.annotation;

import com.wjybxx.fastjgame.net.rpc.RpcResponseChannel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解表示该方法是一个Rpc调用。
 * 该注解分三种情况：
 * 1. 当方法返回值类型不为void时，表明可以立即返回结果，代码生成工具会捕获返回值类型。
 * 2. 当返回值为void时，如果参数中有 {@link RpcResponseChannel}，表明需要异步返回结果，
 * 代码生成工具会那么会捕获其泛型参数作为Rpc调用结果。
 * 3. 如果返回值为void，且参数中没有{@link RpcResponseChannel}，那么表示没有返回值。
 * 即：
 * <pre>{@code
 * 	1. String rpcMethod(long id) -> RpcBuilder<String>
 * 	2. void rpcMethod(RpcResponseChannel<String> channel, ling id) -> RpcBuilder<String>
 * 	3. void oneWayMethod(long id) -> RpcBuilder<?>
 * }</pre>
 * 注意：
 * 1. RpcResponseChannel不参与生成的代理方法的参数列表，因此上面 1，2生成的代理方法签名是一致的！你必须避免这种情况。
 * 2. 方法不能是private - 至少是包级访问权限。
 * 3. methodId必须在[0,9999]区间段。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RpcMethod {

    /**
     * 该方法在该类中的唯一id。
     * 注意：取值范围为闭区间[0, 9999]。
     *
     * @return 由该id和serviceId构成唯一索引。
     */
    short methodId();
}
