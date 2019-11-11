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

package com.wjybxx.fastjgame.misc;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * int值包装。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/6 20:58
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class IntHolder {

    private int value;

    public IntHolder() {
        this(0);
    }

    public IntHolder(int value) {
        this.value = value;
    }

    /**
     * 获取当前值
     *
     * @return
     */
    public int get() {
        return value;
    }

    /**
     * 设置序号
     *
     * @param value
     */
    public void set(int value) {
        this.value = value;
    }

    /**
     * 返回之后+1
     *
     * @return
     */
    public int getAndInc() {
        return value++;
    }

    /**
     * +1之后返回
     *
     * @return
     */
    public int incAndGet() {
        return ++value;
    }

    /**
     * 修改当前值，并返回之前的值
     *
     * @param value
     * @return
     */
    public int getAndSet(int value) {
        int result = this.value;
        this.value = value;
        return result;
    }

    @Override
    public String toString() {
        return "IntHolder{" +
                "value=" + value +
                '}';
    }
}
