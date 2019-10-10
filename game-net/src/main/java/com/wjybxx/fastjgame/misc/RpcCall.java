/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.misc;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;

import java.util.Collections;
import java.util.List;

/**
 * 较为标准的rpc调用，推荐格式，但是仍然不限制rpc调用的形式！
 * 注意：如果使用该形式的rpc调用，请保证{@link RpcCall}在{@link MessageMapper}中存在。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@SerializableClass
public class RpcCall {

    /**
     * 调用的远程方法，用于确定一个唯一的方法。不使用 服务名 + 方法名 + 方法具体参数信息，传输的内容量过于庞大，性能不好。
     */
    @SerializableField(number = 1)
    private final int methodKey;

    /**
     * 方法参数列表，无参时为{@link Collections#emptyList()}
     */
    @SerializableField(number = 2)
    private final List<Object> methodParams;

    /**
     * 需要延迟序列化为byte[]的参数位置信息 - 不序列化。
     */
    @JsonIgnore
    private final int lazyIndexes;

    // 反射创建对象
    private RpcCall() {
        methodKey = -1;
        methodParams = null;
        lazyIndexes = 0;
    }

    public RpcCall(int methodKey, List<Object> methodParams, int lazyIndexes) {
        this.methodKey = methodKey;
        this.methodParams = methodParams;
        this.lazyIndexes = lazyIndexes;
    }

    public int getMethodKey() {
        return methodKey;
    }

    public List<Object> getMethodParams() {
        return methodParams;
    }

    public int getLazyIndexes() {
        return lazyIndexes;
    }
}
