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

import com.google.common.base.Preconditions;
import com.wjybxx.fastjgame.example.ExampleConstants;
import com.wjybxx.fastjgame.example.ExampleMessages;
import com.wjybxx.fastjgame.example.ReflectBasedProtoCodecExample;
import com.wjybxx.fastjgame.net.ProtocolCodec;

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
        cloneTest(ExampleConstants.reflectBasedCodec);
        System.out.println("------------------------");
        cloneTest(ExampleConstants.jsonBasedCodec);
    }

    private static void cloneTest(ProtocolCodec codec) throws IOException {
        final ExampleMessages.FullMessage fullMessage = ReflectBasedProtoCodecExample.getFullMessage();
        Preconditions.checkArgument(codec.cloneMessage(fullMessage).equals(fullMessage), "cloneMessage");
        Preconditions.checkArgument(codec.cloneRpcRequest(fullMessage).equals(fullMessage), "cloneRpcRequest");
        Preconditions.checkArgument(codec.cloneRpcResponse(fullMessage).equals(fullMessage), "cloneRpcResponse");
    }
}
