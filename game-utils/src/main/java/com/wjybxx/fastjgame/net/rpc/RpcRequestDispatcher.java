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

import com.wjybxx.fastjgame.net.misc.NetContext;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * rpc请求分发器。
 * 注意：该接口实现不必是线程安全的，网络层保证所有的逻辑执行都在用户线程 - 即 {@link NetContext#appEventLoop()}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:05
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface RpcRequestDispatcher {

    /**
     * 推送一个rpc请求
     *
     * @param context rpc执行的上下文，用于获取方法参数之外的一些信息
     * @param request rpc请求，如果编解码异常，则可能为null。
     * @return 方法执行结果，可能情况：1.null 2.FluentFuture 3.其它结果
     * @throws Exception 异常情况请抛出异常，暂不支持使用特殊的返回值表达失败。
     */
    Object post(RpcProcessContext context, @Nullable RpcMethodSpec<?> request) throws Exception;

}
