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

package com.wjybxx.fastjgame.utils.concurrent.simple;

import com.wjybxx.fastjgame.utils.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.utils.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.utils.concurrent.RejectedExecutionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * {@link SimpleEventLoopGroup}的默认实现，使用{@link SimpleEventLoopFactory}实现与{@link SimpleEventLoop}的解耦。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/3
 * github - https://github.com/hl845740757
 */
public class DefaultSimpleEventLoopGroup extends MultiThreadEventLoopGroup implements SimpleEventLoopGroup {

    public DefaultSimpleEventLoopGroup(int nThreads,
                                       @Nonnull ThreadFactory threadFactory,
                                       @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                       @Nonnull SimpleEventLoopFactory simpleEventLoopFactory) {
        super(nThreads, threadFactory, rejectedExecutionHandler, simpleEventLoopFactory);
    }

    public DefaultSimpleEventLoopGroup(int nThreads,
                                       @Nonnull ThreadFactory threadFactory,
                                       @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                       @Nullable EventLoopChooserFactory chooserFactory,
                                       @Nonnull SimpleEventLoopFactory simpleEventLoopFactory) {
        super(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, simpleEventLoopFactory);
    }

    @Nonnull
    @Override
    public SimpleEventLoop next() {
        return (SimpleEventLoop) super.next();
    }

    @Nonnull
    @Override
    public SimpleEventLoop select(int key) {
        return (SimpleEventLoop) super.select(key);
    }

    @Nonnull
    @Override
    protected SimpleEventLoop newChild(int childIndex, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        final SimpleEventLoopFactory simpleEventLoopFactory = (SimpleEventLoopFactory) context;
        return simpleEventLoopFactory.newInstance(this, threadFactory, rejectedExecutionHandler);
    }

}
