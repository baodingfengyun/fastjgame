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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认线程工厂。
 * 借鉴于Netty的DefaultThreadFactory 和JDK的Executors中的DefaultThreadFactory实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/23
 * github - https://github.com/hl845740757
 */
public class DefaultThreadFactory implements ThreadFactory {

	private static final Logger logger = LoggerFactory.getLogger(DefaultThreadFactory.class);
	/**
	 * 线程池id，避免name相同时的冲突
	 */
	private static final AtomicInteger poolId = new AtomicInteger();

	private final AtomicInteger nextId = new AtomicInteger();
	/** 线程命名前缀：poolName + poolId */
	private final String prefix;
	/** 是否是守护线程 */
	private final boolean daemon;
	/** 线程优先级 */
	private final int priority;

	public DefaultThreadFactory(String poolName) {
		this(poolName, false);
	}

	public DefaultThreadFactory(String poolName, boolean daemon) {
		this(poolName, daemon, Thread.NORM_PRIORITY);
	}

	public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
		if (poolName == null) {
			throw new NullPointerException("poolName");
		}
		if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException(
					"priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
		}

		prefix = poolName + '-' + poolId.incrementAndGet() + '-';
		this.daemon = daemon;
		this.priority = priority;
	}

	@Override
	public Thread newThread(@Nonnull Runnable r) {
		Thread t = new Thread(r, prefix + nextId.incrementAndGet());
		try {
			if (t.isDaemon() != daemon) {
				t.setDaemon(daemon);
			}

			if (t.getPriority() != priority) {
				t.setPriority(priority);
			}
			UncaughtExceptionHandlers.logIfAbsent(t, logger);
		} catch (Exception ignored) {
			// Doesn't matter even if failed to set.
		}
		return t;
	}
}
