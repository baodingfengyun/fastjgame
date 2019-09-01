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

import com.wjybxx.fastjgame.eventloop.NetEventLoop;

/**
 * Session消息发送模式
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/31
 * github - https://github.com/hl845740757
 */
public enum SessionSenderMode {

	/**
	 * 直接发送的{@link Sender}，该模式下的{@link Sender}不会对任何消息进行缓存，会立即提交到网络层。
	 * 该模式在数据包量较少的时候拥有较高的吞吐量以及较低的延迟。在数据包较多的情况下， 由于{@link NetEventLoop}竞争较为激烈，
	 * 使得吞吐量会降低，延迟也会增加。
	 * 用户不需要调用{@link Session#flush()}。
	 */
	DIRECT,
	/**
	 * 带缓冲区的消息{@link Sender}，该模式下的{@link Sender}会对Session发送的消息进行缓存。
	 * 该模式在数据量较少的时候，延迟上差于{@link #DIRECT}，且吞吐量上没有明显优势。
	 * 但是在数据量较多的时候，能够有效降低竞争，从而拥有更高的吞吐量，延迟变化较小，较为稳定。
	 * 用户需要调用{@link Session#flush()}以确保没有消息残留在缓冲区中。
	 */
	BUFFERED,
}
