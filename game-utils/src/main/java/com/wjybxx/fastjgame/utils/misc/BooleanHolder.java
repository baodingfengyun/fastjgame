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

package com.wjybxx.fastjgame.utils.misc;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Bool值封装。
 * 主要在lambda表达式中使用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class BooleanHolder {

    private boolean value;

    public BooleanHolder() {
        this.value = false;
    }

    public BooleanHolder(boolean value) {
        this.value = value;
    }

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getAndSet(boolean value) {
        boolean result = this.value;
        this.value = value;
        return result;
    }

    @Override
    public String toString() {
        return "BooleanHolder{" +
                "value=" + value +
                '}';
    }
}

