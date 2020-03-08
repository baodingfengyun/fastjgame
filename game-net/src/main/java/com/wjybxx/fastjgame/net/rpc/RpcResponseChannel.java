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

import com.wjybxx.fastjgame.utils.concurrent.Promise;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 返回rpc结果的通道。
 * 注意：该channel是一次性的，只可以使用一次(返回一次结果)，多次调用将抛出异常。
 * 当该参数在Rpc方法的参数中出现时，代码生成工具会捕获泛型T的类型，作为返回类型，且{@link RpcResponseChannel}不会出现在代理方法参数中。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface RpcResponseChannel<T> extends Promise<T> {


}
