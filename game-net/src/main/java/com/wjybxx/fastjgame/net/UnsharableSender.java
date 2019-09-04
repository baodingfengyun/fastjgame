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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;

/**
 * 不可共享的带有缓冲区的sender。
 *
 * 该类是线程安全的 --- 因为其它线程调用直接抛错......
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/4
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class UnsharableSender extends AbstractBufferedSender{

	/** 用户缓存的消息 */
	private LinkedList<BufferTask> buffer = new LinkedList<>();

	public UnsharableSender(AbstractSession session) {
		super(session);
	}

	@Override
	protected void addTask(BufferTask task) {
		if (userEventLoop().inEventLoop()) {
			buffer.add(task);
			// 检查是否需要清空缓冲区
			if (buffer.size() >= session.getNetConfigManager().flushThreshold()) {
				flushBuffer();
			}
		} else {
			throw new IllegalStateException("unsharable");
		}
	}

	@Override
	public void flush() {
		if (userEventLoop().inEventLoop()) {
			if (buffer.size() == 0) {
				return;
			}
			if (session.isActive()) {
				flushBuffer();
			}
			// else 等待关闭
		} else {
			throw new IllegalStateException("unsharable");
		}
	}

	/**
	 * 清空缓冲区(用户线程下) - 批量提交，减少竞争
	 */
	private void flushBuffer() {
		final LinkedList<BufferTask> oldBuffer = exchangeBuffer();
		netEventLoop().execute(()-> {
			for (BufferTask bufferTask :oldBuffer) {
				bufferTask.run();
			}
		});
	}

	/**
	 * 交换缓冲区(用户线程下)
	 * @return oldBuffer
	 */
	private LinkedList<BufferTask> exchangeBuffer() {
		LinkedList<BufferTask> result = this.buffer;
		this.buffer = new LinkedList<>();
		return result;
	}

	/**
	 * 网络层请求清空缓冲器 - 因为数据只能用户线程访问，因此需要提交执行。
	 */
	@Override
	public void clearBuffer() {
		ConcurrentUtils.tryCommit(userEventLoop(), this::cancelAll);
	}

	private void cancelAll() {
		BufferTask bufferTask;
		while ((bufferTask = buffer.pollFirst()) != null) {
			ConcurrentUtils.safeExecute((Runnable) bufferTask::cancel);
		}
	}

}
