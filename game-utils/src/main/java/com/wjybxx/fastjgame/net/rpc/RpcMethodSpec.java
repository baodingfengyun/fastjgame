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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.util.misc.MethodSpec;

/**
 * Rpc方法描述信息
 * <p>
 * Q: 网关服与玩家之间也使用该对象吗？如果使用的话，是为什么呢？
 * A: 也使用该对象。理由如下：
 * 1. 部分消息可能需要在网关服处理，这些协议需要解码；而某些协议可能只是直接转发到另一个服务器，这部分协议不需要解码或半解码。
 * 2. 除了protoBuf消息以外，可能还有一些其它信息。
 * 鉴于以上原因，不直接使用protoBuf对象作为派发依据。
 * PS: 某些游戏服务器可能没有网关服，玩家直接与场景服连接，也适用这里所说的情况。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/8
 * github - https://github.com/hl845740757
 */
public interface RpcMethodSpec<V> extends MethodSpec<V> {

}
