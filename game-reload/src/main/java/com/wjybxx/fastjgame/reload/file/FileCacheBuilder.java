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


import com.wjybxx.fastjgame.reload.mgr.FileDataMgr;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * 多文件缓存构建器
 *
 * @author wjybxx
 * date - 2020/11/28
 * github - https://github.com/hl845740757
 */
public interface FileCacheBuilder<T> {

    @Nonnull
    Set<FileName<?>> fileNames();

    /**
     * 读取文件数据到缓存对象。
     * 注意：读表过程必须包含必要的校验。
     */
    @Nonnull
    T build(FileDataProvider fileDataProvider);

    /**
     * 将读取结果赋值到指定的{@link FileDataMgr}
     *
     * @param cacheData   {@link #build(FileDataProvider)}读取的数据
     * @param fileDataMgr 保存文件数据的地方
     */
    void assignTo(T cacheData, FileDataMgr fileDataMgr);

    /**
     * 校验与其它文件之间的一致性
     *
     * @param fileDataMgr 用于获取其它文件的数据
     */
    void validateOther(FileDataMgr fileDataMgr);

    interface FileDataProvider {

        /**
         * @param fileName 文件名
         * @throws IllegalArgumentException 如果请求的文件名不在{@link FileCacheBuilder#fileNames()}中，则抛出异常
         */
        <T> T getFileData(@Nonnull FileName<T> fileName);
    }
}
