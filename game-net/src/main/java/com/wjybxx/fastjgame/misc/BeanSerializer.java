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

import java.io.IOException;

/**
 * JavaBean序列化工具类超类，生成的代码实现该接口
 *
 * @param <T> 要序列化的bean的类型
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 * github - https://github.com/hl845740757
 */
public interface BeanSerializer<T> {

    /**
     * 将对象写入输出流
     */
    void write(T instance, BeanOutputStream outputStream) throws IOException;

    /**
     * 从输入流中读取一个对象
     */
    T read(BeanInputStream inputStream) throws IOException;

    /**
     * 克隆一个对象
     *
     * @param instance 期望克隆的对象
     * @param util     真正实现clone的工具类
     * @return newInstance
     */
    T clone(T instance, BeanCloneUtil util) throws IOException;
}
