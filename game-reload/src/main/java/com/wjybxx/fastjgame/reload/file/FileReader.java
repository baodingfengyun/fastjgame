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

import java.io.File;

/**
 * <h3>实现约定</h3>
 * 1. reader必须是无状态的，这允许在文件数较多时扩展为多线程读取。
 * 2. 建议作为对应的模板类的静态内部类存在。
 * 3. 需要放指定的包下，这样我们可以通过反射扫描自动加入。
 * 4. 如果需要对多个文件的读取结果做缓存，那么将这些文件的读取逻辑内聚到一个{@link FileCacheBuilder}。
 *
 * @author wjybxx
 * date - 2020/11/27
 * github - https://github.com/hl845740757
 */
public interface FileReader<T> {

    /**
     * @return 关联的文件，相对路径
     */
    FileName<T> fileName();

    /**
     * 读取关联的表格的内容。
     * 注意：这里应当完成自身内容的校验。
     *
     * @param file 关联的表格
     * @return 读取结果
     */
    T read(File file) throws Exception;

    /**
     * 将读取结果赋值到指定的{@link FileDataMgr}
     * 一般情况下建议展开，平铺赋值到{@link FileDataMgr}。
     *
     * @param fileData    {@link #read(File)}读取的数据
     * @param fileDataMgr 保存文件数据的地方
     */
    void assignTo(T fileData, FileDataMgr fileDataMgr);

    /**
     * 校验与其它文件之间的一致性
     *
     * @param fileDataMgr 用于获取其它文件的数据
     */
    void validateOther(FileDataMgr fileDataMgr);

    /**
     * TODO 暂未实现
     *
     * @return 如果返回true，表示文件可以在后台检测到变化后就更新。
     */
    default boolean autoReload() {
        return false;
    }
}
