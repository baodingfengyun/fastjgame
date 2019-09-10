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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.net.injvm.JVMPort;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 管理同一个{@link GameEventLoopGroup}中的内部连接端口
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class JVMPortMrg {

    /**
     * 写到这里很有感慨啊...
     * 当初我去某龙面试的时候。
     * Q1: 用过{@link ConcurrentHashMap}没有。
     * A: 没有。
     * <p>
     * Q2: 那平时都是写的单线程代码是吗？
     * A: 是的。
     * <p>
     * 然后就没问过我多线程的东西了...
     *
     * <p>
     * 这确实是我第一次用{@link ConcurrentHashMap}，我不赞成某些人(某些书)说的总是使用{@link ConcurrentHashMap}代替{@link HashMap}，
     * Q: 它们的理由是什么呢？
     * A: 因为性能差不多。 - 这理由简直可以拖出去枪毙5分钟了。
     * <p>
     * Q: 那什么时候使用{@link ConcurrentHashMap}？
     * A: 如果你盲目的将{@link HashMap}替换成{@link ConcurrentHashMap}，可能能解决问题，但也有可能将问题隐藏，甚至可能什么用也没有！
     * <i>解决了问题</i>和<i>什么用都没有</i>是最好的两种情况，如果是将问题给隐藏了，那么你就是给自己埋了一个更大的坑，以后出现问题更难分析，更难解决，
     * 因为你会觉得使用了{@link ConcurrentHashMap}，这里没有问题。
     * <p>
     * 什么时候使用{@link ConcurrentHashMap}并不重要，重要的是你要清楚你面临的真正问题，这个需要扎实的多线程基础！<br>
     * 建议：优先消除同步逻辑！当你无法消除同步的时候，再考虑{@link ConcurrentHashMap}这种并发组件。<br>
     * 我习惯于：先想办法消除同步，再考虑其它的并发组件。这套框架的底层，也都是这样做的！我在这里的时候很自然的发现{@link ConcurrentHashMap}很适合，就用了。
     */
    private final ConcurrentMap<Long, JVMPort> jvmPortMap = new ConcurrentHashMap<>();

    @Inject
    public JVMPortMrg() {

    }

    /**
     * 发布一个JVMPort，使得其它线程可以通过该jvmPort建立连接。
     * 注意：该方法必须在world将自己注册到zookeeper之前执行！以保证其它world在zookeeper上发现它以后一定能取出它的jvmPort.
     *
     * @param worldGuid worldGuid
     * @param jvmPort   该world监听的jvm端口
     */
    public void register(long worldGuid, JVMPort jvmPort) {
        final JVMPort exist = jvmPortMap.putIfAbsent(worldGuid, jvmPort);
        if (null != exist) {
            throw new IllegalArgumentException("worldGuid " + worldGuid + " is already registered");
        }
    }

    public JVMPort deregister(long worldGuid) {
        return jvmPortMap.remove(worldGuid);
    }

    public JVMPort getJVMPort(long worldGuid) {
        return jvmPortMap.get(worldGuid);
    }
}
