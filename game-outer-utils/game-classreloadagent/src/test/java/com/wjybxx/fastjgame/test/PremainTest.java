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

import com.wjybxx.fastjgame.agent.ClassReloadAgent;

/**
 * 测试环境：
 * 1. 把jar包和当前文件放在同一目录
 * 2. 在命令行中运行，启动参数
 * java -javaagent:game-classreloadagent-1.0.jar=test -cp game-classreloadagent-1.0.jar PremainTest.java
 * 测试输出：
 * premain invoked, agentArgs: test
 * true
 * true
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/11
 * github - https://github.com/hl845740757
 */
public class PremainTest {

    public static void main(String[] args) {
        System.out.println(ClassReloadAgent.isRedefineClassesSupported());
        System.out.println(ClassReloadAgent.isModifiableClass(PremainTest.class));
    }
}
