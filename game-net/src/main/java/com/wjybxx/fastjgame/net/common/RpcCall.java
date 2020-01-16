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

package com.wjybxx.fastjgame.net.common;


import com.wjybxx.fastjgame.async.MethodSpec;
import com.wjybxx.fastjgame.misc.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.List;

/**
 * 较为标准的rpc调用。
 * 推荐使用该方式，但并不限制rpc调用的形式！
 * 注意：如果使用该形式的rpc调用，请保证{@link RpcCall}在{@link MessageMapper}中存在。
 * 警告：不要修改对象的内容，否则可能引发bug(并发错误)。
 * <p>
 * 1.1 版本开始由{@link RpcCallSerializer}负责编解码操作。
 *
 * @param <V> the type of return type
 * @author wjybxx
 * @version 1.1
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcCall<V> implements MethodSpec<V> {

    /**
     * 调用的远程方法，用于确定一个唯一的方法。不使用 服务名 + 方法名 + 方法具体参数信息，传输的内容量过于庞大，性能不好。
     */
    private final int methodKey;

    /**
     * 方法参数列表
     */
    private final List<Object> methodParams;

    /**
     * 需要延迟到网络层序列化为byte[]的参数位置信息。
     * <p>
     * 2020年1月4日修改为需要序列化，原因：我们希望可以转发rpcCall对象，中间的代理需要有原始的rpcCall信息。
     */
    private final int lazyIndexes;
    /**
     * 需要网络层提前反序列化的参数位置信息 - 需要序列化到接收方。
     */
    private final int preIndexes;

    // 反射创建对象
    private RpcCall() {
        methodKey = -1;
        methodParams = null;
        lazyIndexes = 0;
        preIndexes = 0;
    }

    public RpcCall(int methodKey, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.methodKey = methodKey;
        this.methodParams = methodParams;
        this.lazyIndexes = lazyIndexes;
        this.preIndexes = preIndexes;
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

    public int getPreIndexes() {
        return preIndexes;
    }

    /**
     * 负责rpcCall的解析 - 会被扫描到
     */
    private static class RpcCallSerializer implements BeanSerializer<RpcCall<?>> {

        @Override
        public void write(RpcCall<?> instance, BeanOutputStream outputStream) throws IOException {
            outputStream.writeObject(WireType.INT, instance.methodKey);
            outputStream.writeObject(WireType.LIST, instance.methodParams);
            outputStream.writeObject(WireType.INT, instance.lazyIndexes);
            outputStream.writeObject(WireType.INT, instance.preIndexes);
        }

        @Override
        public RpcCall<?> read(BeanInputStream inputStream) throws IOException {
            final Integer methodKey = inputStream.readObject(WireType.INT);
            final List<Object> methodParams = inputStream.readObject(WireType.LIST);
            final Integer lazyIndexes = inputStream.readObject(WireType.INT);
            final Integer preIndexes = inputStream.readObject(WireType.INT);
            return new RpcCall<>(methodKey, methodParams, lazyIndexes, preIndexes);
        }

        @Override
        public RpcCall<?> clone(RpcCall<?> instance, BeanCloneUtil util) throws IOException {
            final List<Object> methodParams = util.clone(WireType.LIST, instance.methodParams);
            return new RpcCall<>(instance.methodKey, methodParams, instance.lazyIndexes, instance.preIndexes);
        }
    }
}
