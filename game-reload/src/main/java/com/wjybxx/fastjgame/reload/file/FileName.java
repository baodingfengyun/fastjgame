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

package com.wjybxx.fastjgame.reload.file;

import com.wjybxx.fastjgame.util.constant.AbstractConstant;
import com.wjybxx.fastjgame.util.constant.ConstantPool;

import java.util.Objects;

/**
 * 文件名信息，用于定于文件名信息。
 * 该常量允许外部扩展，即可以在其它地方定义常量，开放{@link #valueOf(String)}{@link #newInstance(String)}方法。
 *
 * @param <T> 文件的读取结果
 * @author wjybxx
 * date - 2020/11/27
 * github - https://github.com/hl845740757
 */
public final class FileName<T> extends AbstractConstant<FileName<T>> {

    private FileName(int id, String name) {
        super(id, name);
    }

    /**
     * @return 常量的名字是相对资源目录的路径
     */
    public final String getRelativePath() {
        return name();
    }

    /**
     * 必须作为第一个字段，在其它字段使用它之前初始化。
     */
    private static final ConstantPool<FileName<Object>> NAME_POOL = new ConstantPool<>(FileName::new);

    /**
     * @param relativePath 相对项目资源目录的路径
     * @return 文件名对应的常量
     */
    @SuppressWarnings("unchecked")
    public static <T> FileName<T> valueOf(String relativePath) {
        return (FileName<T>) NAME_POOL.valueOf(relativePath);
    }

    /**
     * @param relativePath 相对项目资源目录的路径
     * @return 文件名对应的常量
     * @throws IllegalArgumentException 如果该路径对于的文件名场景已存在，则抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T> FileName<T> newInstance(String relativePath) {
        return (FileName<T>) NAME_POOL.newInstance(relativePath);
    }

    /**
     * @param relativePath 相对项目资源目录的路径
     * @return 文件名对应的常量
     * @throws IllegalArgumentException 如果文件名对应的常量不存在，则抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T> FileName<T> forNameThrowable(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        return (FileName<T>) NAME_POOL.getOrThrow(relativePath);
    }

    // TODO 公共常量定义在下面f
}
