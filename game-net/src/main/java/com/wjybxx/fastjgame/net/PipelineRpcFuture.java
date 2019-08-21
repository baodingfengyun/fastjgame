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
 * 通过Pipeline发起Rpc请求时返回的Future，不支持发起Rpc请求的用户在该Future上进行阻塞操作（为了确保用户线程能把消息发送出去）。
 * (未完成的情况下，用户在future上面阻塞会抛出{@link com.wjybxx.fastjgame.concurrent.BlockingOperationException}异常)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 * github - https://github.com/hl845740757
 */
public interface PipelineRpcFuture extends RpcFuture {

}
