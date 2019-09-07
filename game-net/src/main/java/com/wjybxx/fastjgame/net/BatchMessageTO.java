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

import java.util.List;

/**
 * 一个纯粹的传输对象,用于一次将一批对象发送到channel。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/9
 * github - https://github.com/hl845740757
 */
@TransferObject
public class BatchMessageTO {

	private long ack;

	private List<NetMessage> netMessages;

	public BatchMessageTO(long ack, List<NetMessage> netMessages) {
		this.ack = ack;
		this.netMessages = netMessages;
	}

	public long getAck() {
		return ack;
	}

	public List<NetMessage> getNetMessages() {
		return netMessages;
	}
}
