/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.annotation.Delegated;
import com.wjybxx.fastjgame.annotation.UnstableApi;
import com.wjybxx.fastjgame.concurrent.BlockingOperationException;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetConfigManager;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Session对应的Pipeline组件。
 * Q: 我为什么要设计这个？
 * A: 为了支持用户批量发送异步消息，可大幅度减少竞争。避免每发送一个消息就提交一个任务到{@link NetEventLoop}。
 *
 * @apiNote
 * 注意：如果要使用该特性，请注意以下几点。
 *
 * 1. 必须开启发送缓冲区，也就是{@link NetConfigManager#flushThreshold()}必须大于0。
 *
 * 2. Pipeline只保证通过Pipeline发送的异步消息之间的顺序。使用session中的方法发送的异步消息与通过pipeline发送的异步消息之间没有顺序保证。
 *
 * 3. 同步消息不论从哪里发送，都具备顺序保证。
 *
 * 4. 为确保消息发送出去，用户必须在合适的时候调用{@link #flush()}，否则Pipeline中的消息会有残留。
 *
 * 5. 代理了Session中的方法，使得你可以一直使用Pipeline进行通信，而不必使用Session，两者混合使用容易出现问题。
 *   如果你决定使用{@link SessionPipeline}，那么建议你只使用{@link SessionPipeline}通信。
 *
 * 6. 该类不是线程安全的，非代理的所有接口只有用户线程可以访问。{@link RpcResponseChannel}是线程安全的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/11
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
@UnstableApi
public interface SessionPipeline {

	/**
	 * @return 该pipeline绑定的session。
	 */
	Session session();

	/**
	 * 压入一个单向消息到管道中。
	 * 注意：在压入完成后，如果管道中的异步消息数到达**阈值**，则会立即进行提交，而不会等待 {@link #flush()}
	 *
	 * @param message 单向消息的内容
	 */
	void enqueueMessage(@Nonnull Object message);

	/**
	 * 压入一个异步rpc请求，使用系统默认的异步rpc请求超时时间。
	 * 注意：在压入完成后，如果管道中的异步消息数到达**阈值**，则会立即进行提交，而不会等待 {@link #flush()}
	 *
	 * @param request rpc请求内容
	 */
	void enqueueRpc(@Nonnull Object request, RpcCallback rpcCallback);

	/**
	 * 压入一个异步rpc请求，并指定该异步rpc请求的超时时间。
	 * 注意：在压入完成后，如果管道中的异步消息数到达**阈值**，则会立即进行提交，而不会等待 {@link #flush()}
	 *
	 * @param request rpc请求对象
	 * @param timeoutMs 超时时间，毫秒，必须大于0。
	 */
	void enqueueRpc(@Nonnull Object request, RpcCallback rpcCallback, long timeoutMs);

	/**
	 * 压入一个异步rpc请求，使用系统默认的异步rpc请求超时时间。
	 * 注意：在压入完成后，如果管道中的异步消息数到达**阈值**，则会立即进行提交，而不会等待 {@link #flush()}
	 *
	 * @param request rpc请求内容
	 * @return future，注意：用户在该Future上阻塞一定会抛出{@link BlockingOperationException}
	 */
	@Nonnull
	PipelineRpcFuture enqueueRpc(@Nonnull Object request);

	/**
	 * 压入一个异步rpc请求，并指定该异步rpc请求的超时时间。
	 * 注意：在压入完成后，如果管道中的异步消息数到达**阈值**，则会立即进行提交，而不会等待 {@link #flush()}
	 *
	 * @param request rpc请求对象
	 * @param timeoutMs 超时时间，毫秒，必须大于0。
	 * @return future，注意：用户在该Future上阻塞一定会抛出{@link BlockingOperationException}
	 */
	@Nonnull
	PipelineRpcFuture enqueueRpc(@Nonnull Object request, long timeoutMs);

	/**
	 * 创建一个特定rpc请求对应的结果通道，该通道写入的结果将会由{@link SessionPipeline}发送。
	 *
	 * @param context rpc请求对应的上下文，注意必须是{@link ProtocolDispatcher#onRpcRequest(Session, Object, RpcRequestContext)}中的context。
	 *                一个请求的context，不可以用在其它请求上。
	 * @return 用于返回结果的通道
	 */
	@Nonnull
	<T> RpcResponseChannel<T> newResponseChannel(@Nonnull RpcRequestContext context);

	/**
	 * 返回当前缓存的异步消息数量，不建议查看size后再调用{@link #flush()}，{@link #flush()}一定会处理。
	 * @return int
	 */
	int size();

	/**
	 * 如果pipeline中有缓存的异步消息{@code size() > 0}，则会将pipeline中的消息全部提交到网络层（{@link NetEventLoop}）。
	 * 否则什么也不做。
	 */
	void flush();

	/**
	 * 关闭pipeline，也会关闭session。
	 *
	 * @param flush 是否pipelin中的缓冲消息发送出去
	 * @return future
	 */
	ListenableFuture<?> close(boolean flush);

	// ---------------------------------------- 代理Session的这些方法 ------------------------------------

	@Delegated
	NetContext netContext();

	@Delegated
	long localGuid();

	@Delegated
	RoleType localRole();

	@Delegated
	long remoteGuid();

	@Delegated
	RoleType remoteRole();

	@Nonnull
	@Delegated
	RpcResponse syncRpc(@Nonnull Object request) throws InterruptedException;

	@Nonnull
	@Delegated
	RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) throws InterruptedException;

	@Nonnull
	@Delegated
	RpcResponse syncRpcUninterruptibly(@Nonnull Object request);

	@Nonnull
	@Delegated
	RpcResponse syncRpcUninterruptibly(@Nonnull Object request, long timeoutMs);

	@Delegated
	boolean isActive();
}
