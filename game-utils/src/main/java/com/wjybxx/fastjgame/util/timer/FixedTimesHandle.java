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

package com.wjybxx.fastjgame.util.timer;

/**
 * 执行固定次数的timer的句柄
 *
 * @author wjybxx
 * date - 2020/11/6
 * github - https://github.com/hl845740757
 */
public interface FixedTimesHandle extends FixedDelayHandle {

    /**
     * 注意：同{@link #executedTimes()}一样，该值在每次执行回调之后才会更新。
     *
     * @return 剩余可执行次数，大于等于0。等于0表示已结束，大于0也可能已关闭，请注意{@link #isClosed()}方法。
     */
    int remainTimes();

    /**
     * @param remainTimes 期望的剩余执行次数，必须大于0。如果期望关闭timer，请调用{@link #close()}方法。
     * @return 如果设置成功，则返回true。
     */
    boolean setRemainTimes(int remainTimes);

}
