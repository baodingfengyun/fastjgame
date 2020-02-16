/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net.serializer;

import com.wjybxx.fastjgame.net.annotation.SerializableClass;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

/**
 * JavaBean序列化工具类超类，生成的代码实现该接口。
 * <p>
 * 注意：
 * 1. 一般而言，建议使用注解{@link SerializableClass}，并遵循相关规范，由注解处理器生成的类负责解析，而不是实现{@link EntitySerializer}。
 * 2. 仅当反射编解码的类存在性能瓶颈时，才应该考虑实现{@link EntitySerializer}负责编解码相关的类。
 * <p>
 * 如果手动实现该接口：
 * 1. 必须保证线程安全，最好是无状态的。
 * 2. 最好实现为目标类的静态内部类，且最好是private级别，不要暴露给外层。
 * 3. 必须有一个无参构造方法(可以private)。
 * 4. 扫描器通过泛型参数获取负责序列化的类型。
 *
 * @param <T> 要序列化的bean的类型
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface EntitySerializer<T> {

    /**
     * 创建一个对象
     * 该工厂方法的必要性: 要支持继承必须如此。
     */
    T newInstance();

    /**
     * 从输入流中读取实例类定义的字段
     */
    void readFields(T instance, EntityInputStream inputStream) throws IOException;

    /**
     * 将对象中要序列化的字段写入输出流
     */
    void writeFields(T instance, EntityOutputStream outputStream) throws IOException;

}
