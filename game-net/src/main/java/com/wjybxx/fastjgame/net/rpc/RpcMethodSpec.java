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


import com.wjybxx.fastjgame.net.binary.EntityInputStream;
import com.wjybxx.fastjgame.net.binary.EntityOutputStream;
import com.wjybxx.fastjgame.net.binary.EntitySerializer;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认的rpc方法结构体，可以根据{@link #serviceId} 和{@link #methodId}确定唯一的一个方法。
 * <p>
 * Q: 为什么不是个接口？
 * A: 因为某些类并不是我们手写的代码（主要为兼容客户端与服务器不同语言时采用的协议文件）。
 *
 * <p>
 * 警告：不要修改对象的内容，否则可能引发bug(并发错误)。
 *
 * <h3>实现者需要注意</h3>
 * 千万不要把{@link RpcSupportHandler}和{@link RpcMethodSpec}对象绑定。
 * 重命名为{@link RpcMethodSpec}就是为了避免错误的联想！
 *
 * @param <V> the type of return type
 * @author wjybxx
 * @version 1.1
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class RpcMethodSpec<V> implements com.wjybxx.fastjgame.utils.async.MethodSpec<V> {

    /**
     * 远程服务id
     * 不使用 服务名 + 方法名 + 方法具体参数信息，传输的内容量过于庞大，性能不好。
     */
    private final short serviceId;
    /**
     * 远程方法id
     */
    private final short methodId;

    /**
     * 方法参数列表
     */
    private final List<Object> methodParams;

    /**
     * 需要延迟到网络层序列化为byte[]的参数位置信息。
     * <p>
     * Q: 为什么要序列化？
     * A: 我们希望可以转发该对象，中间的代理需要有原始的方法描述信息。
     */
    private final int lazyIndexes;
    /**
     * 需要网络层提前反序列化的参数位置信息 - 需要序列化到接收方。
     */
    private final int preIndexes;

    public RpcMethodSpec(short serviceId, short methodId, List<Object> methodParams, int lazyIndexes, int preIndexes) {
        this.serviceId = serviceId;
        this.methodId = methodId;
        this.methodParams = methodParams;
        this.lazyIndexes = lazyIndexes;
        this.preIndexes = preIndexes;
    }

    public short getServiceId() {
        return serviceId;
    }

    public short getMethodId() {
        return methodId;
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
     * 负责{@link RpcMethodSpec}的序列化工作 - 会被自动扫描到。
     */
    @SuppressWarnings("unused")
    private static class RpcMethodSpecSerializer implements EntitySerializer<RpcMethodSpec> {

        @Override
        public Class<RpcMethodSpec> getEntityClass() {
            return RpcMethodSpec.class;
        }

        @Override
        public RpcMethodSpec readObject(EntityInputStream inputStream) throws Exception {
            final short serviceId = inputStream.readShort();
            final short methodId = inputStream.readShort();
            final List<Object> methodParams = inputStream.readCollection(ArrayList::new);
            final int lazyIndexes = inputStream.readInt();
            final int preIndexes = inputStream.readInt();
            return new RpcMethodSpec(serviceId, methodId, methodParams, lazyIndexes, preIndexes);
        }

        @Override
        public void writeObject(RpcMethodSpec instance, EntityOutputStream outputStream) throws Exception {
            outputStream.writeShort(instance.getServiceId());
            outputStream.writeShort(instance.getMethodId());
            outputStream.writeCollection(instance.getMethodParams());
            outputStream.writeInt(instance.getLazyIndexes());
            outputStream.writeInt(instance.getPreIndexes());
        }
    }
}
