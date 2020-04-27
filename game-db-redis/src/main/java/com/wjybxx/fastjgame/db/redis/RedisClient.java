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

package com.wjybxx.fastjgame.db.redis;

import com.wjybxx.fastjgame.utils.concurrent.FluentFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * redis客户端。
 * 使用{@code Command}有许多好处。
 * 1. 扩展更为方便。
 * 2. api更加简洁。
 * 3. service的核心工作清清楚楚。
 * 4. 可以简化代理类的工作。
 * 5. 如果改成远程服务，那么改动量将会很小。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/9
 * github - https://github.com/hl845740757
 */
public interface RedisClient {

    /**
     * 异步执行一个命令，并不监听结果
     *
     * @param command 待执行的命令
     */
    void execute(@Nonnull PipelineCommand<?> command);

    /**
     * 异步执行一个命令，同时刷新命令队列，并不监听结果。
     *
     * @param command 待执行的命令
     */
    void executeAndFlush(@Nonnull PipelineCommand<?> command);

    /**
     * 异步执行一个redis命令，并在完成时通知指定的监听器。
     *
     * @param command 待执行的命令
     * @param decoder 解码器，不建议直接使用redis的存储结构作为返回值。如果需要，请使用{@link Function#identity()}函数。
     * @return future
     */
    <T, U> FluentFuture<U> call(@Nonnull PipelineCommand<T> command, Function<T, U> decoder);

    /**
     * 异步执行一个redis命令，同时刷新命令队列，并在完成时通知指定的监听器。
     *
     * @param command 待执行的命令
     * @param decoder 解码器
     * @return future
     */
    <T, U> FluentFuture<U> callAndFlush(@Nonnull PipelineCommand<T> command, Function<T, U> decoder);

    /**
     * 执行一个redis命令，并阻塞到命令完成。
     *
     * @param command 待执行的命令
     * @param decoder 解码器
     * @return 解码后的结果
     */
    <T, U> U syncCall(@Nonnull RedisCommand<T> command, Function<T, U> decoder) throws CompletionException;

}
