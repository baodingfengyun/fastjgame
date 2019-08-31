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
 * 网络层已收到，但是还未提交给应用层的消息。
 * 网络层可以批量的将消息网络包提交给应用层，减少 网络层与网络层，网络层与应用层之间的竞争。
 *
 * 2019年8月31日17:22:20
 * Q: 我为什么删除了{@code onReject}方法？<br>
 * A: 网络层操心操太多了，造成了错误的耦合。数据包的形式是不限定的，任何一个消息，网络层都不能代替应用层处理！
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/8
 * github - https://github.com/hl845740757
 */
public interface UncommittedMessage {

	/**
	 * 执行提交操作，此时运行在用户线程下。
	 * @param session 所在的session
	 * @param protocolDispatcher 该会话上的消息处理器
	 */
	void commit(Session session, ProtocolDispatcher protocolDispatcher);

}
