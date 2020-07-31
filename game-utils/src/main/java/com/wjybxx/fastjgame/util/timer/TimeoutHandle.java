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

package com.wjybxx.fastjgame.util.timer;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 只执行一次的Timer的handle。
 * {@link TimerSystem#newTimeout(long, TimerTask)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/7
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public interface TimeoutHandle extends TimerHandle {

    /**
     * 指定的超时时间。
     *
     * @return 毫秒
     */
    long timeout();

    /**
     * 更新超时时间，立即生效。
     *
     * @param timeout 新的超时时间
     * @return 更新成功则返回true
     */
    boolean setTimeout(long timeout);

}
