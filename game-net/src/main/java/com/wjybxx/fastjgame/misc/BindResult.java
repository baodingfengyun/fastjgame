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
package com.wjybxx.fastjgame.misc;

import io.netty.channel.Channel;

/**
 * 绑定端口结果
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/5
 * github - https://github.com/hl845740757
 */
public class BindResult {

    /**
     * 绑定到的channel
     */
    private final Channel channel;
    /**
     * 成功绑定的端口
     */
    private final HostAndPort hostAndPort;

    public BindResult(Channel channel, HostAndPort hostAndPort) {
        this.channel = channel;
        this.hostAndPort = hostAndPort;
    }

    public Channel getChannel() {
        return channel;
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }
}
