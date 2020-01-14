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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;
import com.wjybxx.fastjgame.eventbus.Subscribe;
import com.wjybxx.fastjgame.misc.RoleType;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/14
 * github - https://github.com/hl845740757
 */
public class CoreAutoTest {

    public static void main(String[] args) {

    }

    @Subscribe
    public void onEvent(String name) {

    }

    @Subscribe
    public void onEvent(RoleType roleType) {

    }

    @SerializableClass
    public static class MessageA {

        @SerializableField(number = 1)
        private int number;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    @SerializableClass
    public static class MessageB {

        @SerializableField(number = 1)
        private final int number;

        private MessageB() {
            number = -1;
        }

        public MessageB(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

    }
}
