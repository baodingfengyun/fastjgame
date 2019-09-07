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

package com.wjybxx.fastjgame.net;

import io.netty.channel.Channel;

/**
 * 单向消息事件参数。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@TransferObject
public class OneWayMessageEventParam extends MessageEventParam{

	/** 消息内容 */
	private Object message;

	public OneWayMessageEventParam(Channel channel, long localGuid, long remoteGuid, long ack, long sequence, Object message) {
		super(channel, localGuid, remoteGuid, ack, sequence);
		this.message = message;
	}

	public Object getMessage() {
		return message;
	}
}
