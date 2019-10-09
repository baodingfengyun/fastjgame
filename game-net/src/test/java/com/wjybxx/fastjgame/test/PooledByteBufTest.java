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

package com.wjybxx.fastjgame.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/9
 * github - https://github.com/hl845740757
 */
public class PooledByteBufTest {

    private static final int MAX_SIZE = 64;

    public static void main(String[] args) throws InterruptedException {
        List<ByteBuf> byteBufList = new ArrayList<>(MAX_SIZE);
        for (int index = 0; index < MAX_SIZE; index++) {
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(1024 * 1024);
            byteBufList.add(byteBuf.slice(800, 200).retain());
        }
        Thread.sleep(60 * 60 * 1000);
    }
}
