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

/**
 * 注意：虽然定义为{@link com.wjybxx.fastjgame.annotation.RpcService}，但是架构并不是基于service的，
 * 而是基于{@link com.wjybxx.fastjgame.world.World}的，rpc是用于world（模块）之间通信的，正常的开发都是在world内完成的。
 *
 * <p>
 * Q: 为什么跨module的RPC接口必须定义在core模块？不能编译其它模块的时候将代码生成在core模块吗？<br>
 * A: 我之前确实是这个思路，不过遇见了一个奇怪的现象，那就是java文件确确实实存在于core模块下，写代码的时候
 * 也没提示任何错误，但是一旦开始编译，就会提示<b>找不到符号</b>！类明明在那里啊，为啥找不到符号啊？？？
 * --> 后来才想明白，编译的顺序是这样的：
 * <pre>
 *   compile game-util
 *   compile game-net
 *   compile game-core
 *   compile game-center
 *   compile game-scene
 *   ......
 * </pre>
 * 在编译center或scene的时候，将生成的代码放入到game-core，确实是放进去了，但是game-core已经完成编译了！<br>
 * 放进去的类本轮是无法参与到编译的，也就找不到对应的类！<br>
 * 如果将生成的类放在src下，可以在下一轮参与编译，如果将生成的类放在target下，那么莫得用。<br>
 * <p>
 * Q: 那为什么不将生成的类放在src下呢？<br>
 * A: 1. 它是根据当前代码生成的，是冗余的，不应该存在于版本库的。 2.多次编译才能得到正确结果，这样很不安全，也很繁琐。
 * <p>
 * 最近可能加班加多了，这几天脑子老是短路，下班路上才想明白，也得遇见过这个坑的才会明白，查是查不到的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/22
 */
package com.wjybxx.fastjgame.rpcservice;