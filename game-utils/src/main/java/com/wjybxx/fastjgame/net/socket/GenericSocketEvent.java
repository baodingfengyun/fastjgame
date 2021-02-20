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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.util.eventbus.GenericEvent;

import javax.annotation.Nonnull;

/**
 * 泛型socket事件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/19
 * github - https://github.com/hl845740757
 */
public class GenericSocketEvent<T extends SocketEvent> implements GenericEvent<T> {

    private final T socketEvent;
    private final boolean forAcceptor;

    public GenericSocketEvent(T socketEvent, boolean forAcceptor) {
        this.socketEvent = socketEvent;
        this.forAcceptor = forAcceptor;
    }

    @Nonnull
    public T child() {
        return socketEvent;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public Class<T> childKey() {
        return (Class<T>) socketEvent.getClass();
    }

    public boolean isForAcceptor() {
        return forAcceptor;
    }

    @Override
    public String toString() {
        return "GenericSocketEvent{" +
                "child=" + socketEvent +
                ", forAcceptor=" + forAcceptor +
                '}';
    }
}
