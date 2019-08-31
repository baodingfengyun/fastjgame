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

/**
 * {@link CommitTask}的抽象实现，确保所有操作只在{@link Session#isActive() true}的情况下执行。
 * 在用户已关闭session的情况下，如果提交消息，可能导致应用层逻辑错误。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/31
 * github - https://github.com/hl845740757
 */
public abstract class AbstractCommitTask implements CommitTask {

	protected final Session session;

	public AbstractCommitTask(Session session) {
		this.session = session;
	}

	@Override
	public final void run() {
		if (session.isActive()) {
			doCommit();
		}
	}

	protected abstract void doCommit();
}
