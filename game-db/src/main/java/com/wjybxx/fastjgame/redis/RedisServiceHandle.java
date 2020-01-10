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

package com.wjybxx.fastjgame.redis;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;

/**
 * redis服务。
 * 使用{@link RedisCommand}有许多好处。
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
public interface RedisServiceHandle {

    /**
     * 异步执行一个命令，并不监听结果
     *
     * @param command 待执行的命令
     */
    void execute(@Nonnull RedisCommand<?> command);

    /**
     * 异步执行一个命令，同时刷新命令队列，并不监听结果。
     *
     * @param command 待执行的命令
     */
    void executeAndFlush(@Nonnull RedisCommand<?> command);

    /**
     * 异步执行一个redis命令，并在完成时通知指定的监听器。
     *
     * @param <V>     the type of result
     * @param command 待执行的命令
     */
    <V> RedisFuture<V> call(@Nonnull RedisCommand<V> command);

    /**
     * 异步执行一个redis命令，同时刷新命令队列，并在完成时通知指定的监听器。
     *
     * @param <V>     the type of result
     * @param command 待执行的命令
     */
    <V> RedisFuture<V> callAndFlush(@Nonnull RedisCommand<V> command);

    /**
     * 执行一个redis命令，同时刷新缓冲区，并阻塞到命令完成。
     *
     * @param command 待执行的命令
     * @param <V>     the type of result
     */
    <V> V syncCall(@Nonnull RedisCommand<V> command) throws ExecutionException;

    /**
     * 返回一个future，该future会在service<b>当前</b>所有命令执行完毕后会得到一个通知。
     * - 当你需要在前面的redis命令执行完毕后需要执行某个动作时可以使用该方法。
     * - 比如：等待redis操作完成后关闭线程。
     */
    RedisFuture<?> newWaitFuture();
}
