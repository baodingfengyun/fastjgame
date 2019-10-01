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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.net.common.RpcCallback;
import com.wjybxx.fastjgame.net.common.RpcResponse;

/**
 * 更加安全的{@link RpcCallback}，它的目的是建议大家尽量使用方法引用代替lambda表达式。
 * <p>
 * Q: 什么意思呢？
 * A: lambda表达式带来了很多便利。但是由于部分人对lambda表达式带来的危险性一无所知，可能导致系统一些奇奇怪怪的问题。
 * 最常见的就是内存泄漏，其次是逻辑错误。这些问题的一个主要原因：捕获了不该捕获的对象！而使用方法引用 + context 或 内部类，
 * 你会更清楚的知道自己应该保存什么作为上下文。
 *
 * <p>
 * Q: 内存泄漏是怎么产生的呢？
 * A: lambda表达式内部会捕获引用到的变量，而实际上你可能只是需要里被捕获的变量里的一个属性，无意间就造成了内存泄漏。
 * <p>
 * Q: 逻辑错误有示例吗？
 * A: 逻辑错误常见的情况是这样的，lambda表达式捕获了一个可变对象，lambda基于该可变对象的某些属性进行计算，而外部可能修改lambda表达式依赖的属性。
 * eg: 以下两个表达式含义完全不一样！
 * <pre>{@code
 *      // 该lambda表达式捕获了一个 point2d对象，该对象的x坐标是可能改变的，同一个x的计算结果可能是不同的
 *      return (x) -> x + point2d.getX();
 *      // 该lambda表达式没有捕获对象，只是捕获了一个float类型，同一个x的计算结果一定是一样的。
 *      float deltaX = point2d.getX();
 *      return (x) -> x + deltaX;
 * }</pre>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/21
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface SaferRpcCallback<T> {

    /**
     * 当rpc调用完成时
     *
     * @param rpcResponse rpc调用结果
     * @param context     发起rpc调用时保存的上下文
     */
    void onComplete(RpcResponse rpcResponse, T context);
}
