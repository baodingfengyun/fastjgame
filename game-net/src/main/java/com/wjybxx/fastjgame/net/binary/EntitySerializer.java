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

package com.wjybxx.fastjgame.net.binary;

import com.wjybxx.fastjgame.net.annotation.SerializableClass;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.function.IntFunction;

/**
 * 实体类序列化工具类，生成的代码会实现该接口。
 * <br>---------------------------------------------------------<br>
 * 注意：
 * 1. 一般而言，建议使用注解{@link SerializableClass}，并遵循相关规范，由注解处理器生成的类负责解析，而不是实现{@link EntitySerializer}。
 * 2. 仅当反射编解码的类存在性能瓶颈时，才应该考虑手动实现{@link EntitySerializer}负责编解码相关的类。
 * 3. 扫描器通过泛型参数获取负责序列化的类型，因此只要进行了实现，就可以被自动加入。
 * <br>---------------------------------------------------------<br>
 * 如果手动实现该接口：
 * 1. 必须保证线程安全，最好是无状态的。
 * 2. 最好实现为目标类的静态内部类，且最好是private级别，不要暴露给外层。
 * 3. 必须有一个无参构造方法(可以private)。
 * 4. 必须{@link EntityInputStream#readMap(IntFunction)} {@link EntityInputStream#readCollection(IntFunction)} 去读取map
 * <br>---------------------------------------------------------<br>
 * 本来我的想法是这样的：
 * 1. 有这样三个api {@code newInstance()} {@code readDeclaredFields(Object, EntityInputStream)}
 * {@code writeDeclaredFields(Object, EntityOutputStream)}
 * 2. 读的时候：先用工厂方法创建一个对象，然后每个{@link EntitySerializer}只负责读当前类定义的字段，然后一直递归到{@link Object}类型。
 * 3. 写的时候，每个{@link EntitySerializer}只负责写当前类定义的字段，然后一直递归到{@link Object}类型。
 * 这样的话逻辑简单且清晰，但是这将导致手动实现很痛苦，生成的代码和手写的代码也不能很好的兼容，
 * 要支持手写代码和生成代码之间兼容的话，只能将继承关系在实现类中处理，每个类不依赖其它类。
 * 所以又给生成代码增加了一层难度。
 *
 * @param <T> 要序列化的bean的类型
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface EntitySerializer<T> {

    T readObject(@Nonnull EntityInputStream inputStream) throws Exception;

    void writeObject(@Nonnull T instance, @Nonnull EntityOutputStream outputStream) throws Exception;

}
