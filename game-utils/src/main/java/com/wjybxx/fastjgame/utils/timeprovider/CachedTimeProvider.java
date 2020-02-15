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

package com.wjybxx.fastjgame.utils.timeprovider;

/**
 * 可缓存的系统时间提供器，主要目的：减少{@link System#currentTimeMillis()}调用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/29
 * github - https://github.com/hl845740757
 */
public interface CachedTimeProvider extends TimeProvider {

    @Override
    long curTimeMillis();

    @Override
    int curTimeSeconds();

    /**
     * 尝试更新系统时间
     *
     * @param curTimeMillis 指定的系统毫秒时间
     * @return 更新成功则返回true
     */
    boolean update(long curTimeMillis);
}
