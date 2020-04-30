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

package com.wjybxx.fastjgame.utils.concurrent;

import javax.annotation.Nonnull;

/**
 * 建议使用{@link FutureUtils#newPromise()}工厂方法代替构造方法，减少实现类的依赖
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/26
 * github - https://github.com/hl845740757
 */
public class DefaultPromise<V> extends AbstractFluentPromise<V> {

    protected DefaultPromise() {
    }

    protected DefaultPromise(V result) {
        super(result);
    }

    protected DefaultPromise(@Nonnull Throwable cause) {
        super(cause);
    }

    @Override
    protected <U> AbstractPromise<U> newIncompletePromise() {
        return new DefaultPromise<>();
    }
}
