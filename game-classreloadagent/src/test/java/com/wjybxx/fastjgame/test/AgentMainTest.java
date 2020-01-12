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

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.wjybxx.fastjgame.agent.ClassReloadAgent;

import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * 测试环境：
 * 1. 将jar包安装到本地maven仓库
 * 2. 在idedebug配置vm options中添加 -Djdk.attach.allowAttachSelf=true
 * 3. 直接在ide中debug
 * 测试输出：
 * Connected to the target VM, address: '127.0.0.1:10876', transport: 'socket'
 * agentmain invoked, agentArgs: null
 * true
 * true
 * <p>
 * 问题：java.lang.RuntimeException: java.io.IOException: Can not attach to current VM
 * 解决方法：run configurations中的vm options中加上 -Djdk.attach.allowAttachSelf=true
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/12
 * github - https://github.com/hl845740757
 */
public class AgentMainTest {

    public static void main(String[] args) throws IOException, AttachNotSupportedException,
            AgentLoadException, AgentInitializationException {
        String pid = getPid();
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent("game-classreloadagent-1.0.jar");

        System.out.println(ClassReloadAgent.isRedefineClassesSupported());
        System.out.println(ClassReloadAgent.isModifiableClass(AgentMainTest.class));
    }

    private static String getPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }
}
