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

/**
 * 接收缓冲区中未提交的单向消息
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public class UncommittedOneWayMessage implements UncommittedMessage{

	/** 单向消息的内容 */
	private final Object message;

	public UncommittedOneWayMessage(Object message) {
		this.message = message;
	}

	@Override
	public void commit(Session session, ProtocolDispatcher protocolDispatcher) throws Exception {
		protocolDispatcher.postOneWayMessage(session, message);
	}

	@Override
	public void onRejected(Session session) {
		// do noting
	}
}
