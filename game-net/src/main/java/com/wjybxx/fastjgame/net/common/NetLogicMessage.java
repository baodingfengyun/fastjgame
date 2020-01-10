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

package com.wjybxx.fastjgame.net.common;

/**
 * 网络层逻辑消息包。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/7
 * github - https://github.com/hl845740757
 */
public abstract class NetLogicMessage implements NetMessage {

    /**
     * 消息的内容，限定为Object类型，分离协议规范和实现。
     * 子类只保留控制信息(各自的协议头信息)。
     */
    private Object body;

    protected NetLogicMessage(Object body) {
        this.body = body;
    }

    public final Object getBody() {
        return body;
    }

    public final void setBody(Object body) {
        this.body = body;
    }
}
