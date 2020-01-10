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

package com.wjybxx.fastjgame.timer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * 定时器(timer)句柄，提供取消等操作。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface TimerHandle {

    /**
     * 设置附加属性(使得task在运行的时候可以获取该值)。
     * 注意：attachment在timer关闭或取消时不会自动删除，当你不需要使用时，可以尽早的释放它(设置为null)。
     *
     * @param newData 新值
     * @return 之前的值，如果不存在，则返回null
     */
    @Nullable
    <T> T attach(@Nullable Object newData);

    /**
     * 返回当前的附加属性（上一次attach的值）。
     *
     * @param <T> 结果类型，方便强制类型转换
     * @return nullable，如果未调用过attach，一定为null
     */
    @Nullable
    <T> T attachment();

    /**
     * 查询距离下次执行的延迟时间
     *
     * @return -1 表示已停止，否则返回大于等于0的值。
     */
    long runDelay();

    /**
     * 尝试关闭该handle关联的TimerTask，如果handle关联的timer早已关闭，则该方法什么也不会做。
     */
    void close();

    /**
     * 该handle关联的TimerTask是否已关闭（手动关闭，正常结束都属于关闭）。
     *
     * @return 如果返回true，表示关联的TimerTask再也不会执行。
     */
    boolean isClosed();

    /**
     * 获取timer关联的异常处理器。
     * 注意：默认的异常处理器为{@link ExceptionHandlers#LOG}，仅仅记录一个日志。
     */
    @Nonnull
    ExceptionHandler getExceptionHandler();

    /**
     * 设置timer关联的异常处理器
     *
     * @param exceptionHandler 异常处理器
     * @return this
     */
    TimerHandle setExceptionHandler(@Nonnull ExceptionHandler exceptionHandler);

}
