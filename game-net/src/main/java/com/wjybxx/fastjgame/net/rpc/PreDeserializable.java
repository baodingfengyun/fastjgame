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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.binary.EntitySerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示rpc方法中的某个参数可提前序列化的，与{@link LazySerializable}配对，它表示需要网络层帮我提前反序列化。<br>
 * 它需要知道被调用的方法的信息，才可以做到。<br>
 * <p>
 * 注意：
 * 1. 该注解只可以用在非byte[]参数，否则编译报错。<br>
 * 2. 代理方法参数类型为byte[]。<br>
 * <p>
 * 对生成代码的影响：<br>
 * 1. 对非byte[]类型且带有{@link LazySerializable} 的参数替换为{@link byte[]}类型。<br>
 * 2. 为提高编码速度，必须进行索引，避免不必要的遍历。
 * <p>
 * Q: 它的主要目的？<br>
 * A: 当网关接受到玩家消息之后，会读取为byte[]，而不进行解码操作，这个byte[]要发送到场景服务器等，
 * 我们希望场景服在接收该参数之前，网络层能提前进行解码操作，而不必应用层自己解码。
 * （现在通过手动实现{@link EntitySerializer}的话也可以达到相同目的了）
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/10
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface PreDeserializable {

}
