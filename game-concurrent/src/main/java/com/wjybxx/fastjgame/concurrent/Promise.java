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

package com.wjybxx.fastjgame.concurrent;

import javax.annotation.Nonnull;

/**
 * 可写结果的FutureListener。
 * 与JDK的{@link java.util.concurrent.FutureTask}不同的是，可以由外部执行计算，Future仅仅作为传递结果的凭证。
 * 当然{@link java.util.concurrent.FutureTask}虽然有些地方不好用，却不容易出现问题，而采用可写模式的promise，会产生一些额外的风险。
 * (支持的越多越复杂，越容易出现错误)
 *
 * 完成的含义：future关联的任务生命周期已结束。不论操作成功，失败，取消（取消是失败的一种），都表示完成状态。
 *
 * @param <V>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface Promise<V> extends ListenableFuture<V> {

	/**
	 * 将future标记为成功完成，并且通知所有的监听器。
	 *
	 * 如果该future对应的操作早已完成(失败或成功)，将抛出一个{@link IllegalStateException}.
	 */
	void setSuccess(V result);

	/**
	 * 尝试将future标记为成功完成，标记成功时通知所有的监听器。
	 *
	 * @return 当且仅当成功将future标记为成功完成时返回true，如果future对应的操作已完成(成功或失败)，则返回false，并什么都不改变。
	 */
	boolean trySuccess(V result);

	/**
	 * 将future标记为失败完成，并且通知所有的监听器。
	 *
	 * 如果future对应的操作早已完成（成功或失败），则抛出一个{@link IllegalStateException}.
	 */
	 void setFailure(@Nonnull Throwable cause);

	/**
	 * 尝试将future标记为失败完成，标记成功时通知所有监听器。
	 *
	 * @return 当前仅当成功将future标记为失败完成时返回true，如果future对应的操作已完成（成功或失败），则返回false，并什么也不改变。
	 */
	boolean tryFailure(@Nonnull Throwable cause);

	/**
	 * 将future标记为不可取消状态，它表示计算已经开始，不可以被取消。
	 *
	 * @return 1. 如果成功设置为不可取消 或 已经是不可取消状态 则返回true.
	 * 		   2. 已经进入完成状态(不是被取消进入的完成状态) 返回true。
	 * 		   否则返回false。
	 */
	boolean setUncancellable();
}
