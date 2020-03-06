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
 * promise用于为关联的{@link ListenableFuture}赋值结果。
 * --
 * 新版本的promise与future的关系改为组合关系，旧版本中的继承关系实在是太多坑，过多参考了netty的设计，把坑的也继承过来了。
 *
 * @param <V>
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14
 * github - https://github.com/hl845740757
 */
public interface Promise<V> extends IPromise<V> {

    @Nonnull
    @Override
    ListenableFuture<V> getFuture();

}
