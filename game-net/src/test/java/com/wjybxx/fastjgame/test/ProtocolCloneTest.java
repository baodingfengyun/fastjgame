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

import com.wjybxx.fastjgame.net.example.BinaryProtoCodecTest;
import com.wjybxx.fastjgame.net.example.ExampleConstants;
import com.wjybxx.fastjgame.net.example.ExampleMessages;
import com.wjybxx.fastjgame.net.common.ProtocolCodec;
import com.wjybxx.fastjgame.net.utils.NetUtils;

import java.io.IOException;

/**
 * 对象拷贝测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/11
 * github - https://github.com/hl845740757
 */
public class ProtocolCloneTest {

    public static void main(String[] args) throws IOException {
        // 触发NetUtils类加载，避免输出干扰
        System.out.println(NetUtils.getOuterIp());

        cloneTest(ExampleConstants.binaryCodec);
        cloneTest(ExampleConstants.jsonCodec);
    }

    private static void cloneTest(ProtocolCodec codec) throws IOException {
        System.out.println("\n" + codec.getClass().getName());
        final ExampleMessages.FullMessage fullMessage = BinaryProtoCodecTest.newFullMessage();
        System.out.println("cloneField " + codec.cloneObject(fullMessage).equals(fullMessage));
    }
}
