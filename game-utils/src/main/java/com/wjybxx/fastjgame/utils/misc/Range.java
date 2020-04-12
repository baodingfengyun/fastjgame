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

package com.wjybxx.fastjgame.utils.misc;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/12
 */
public class Range {

    /**
     * inclusive
     */
    public final int start;
    /**
     * inclusive
     */
    public final int end;

    public Range(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean isEmpty() {
        return start > end;
    }

    public int length() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return "Range[" + start + "," + end + "]";
    }
}
