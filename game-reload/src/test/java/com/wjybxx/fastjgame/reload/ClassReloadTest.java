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

package com.wjybxx.fastjgame.reload;

import com.sun.tools.attach.VirtualMachine;
import com.wjybxx.fastjgame.reload.mgr.ClassReloadMgr;

import java.lang.management.ManagementFactory;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class ClassReloadTest {

    /**
     * 请在启动参数中添加 -Djdk.attach.allowAttachSelf=true
     * 否则抛出 java.io.IOException: Can not attach to current VM 异常。
     */
    public static void main(String[] args) throws Exception {
        startClassReloadAgent();
        final ClassReloadMgr classReloadMgr = new ClassReloadMgr("game-reload/target", "classes");
        classReloadMgr.reloadAll();
    }

    /**
     * 启动热更新代理组件
     */
    private static void startClassReloadAgent() throws Exception {
        final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        final VirtualMachine vm = VirtualMachine.attach(pid);
        // jar包必须放在可加载路径
        vm.loadAgent("./game-libs/game-classreloadagent-1.0.jar");
    }

}
