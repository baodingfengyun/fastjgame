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

package com.wjybxx.fastjgame.utils.time;

/**
 * 系统时间提供者
 * 线程安全性取决于实现，该接口并不要求所有子类都是线程安全的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/9
 * github - https://github.com/hl845740757
 */
public interface TimeProvider {

    /**
     * 获取系统毫秒时间戳
     *
     * @return 毫秒
     */
    long curTimeMillis();

    /**
     * 获取系统秒数时间戳
     *
     * @return 秒
     */
    int curTimeSeconds();

}
