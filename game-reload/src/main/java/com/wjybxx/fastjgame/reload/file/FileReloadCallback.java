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

package com.wjybxx.fastjgame.reload.file;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author wjybxx
 * date - 2020/12/3
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface FileReloadCallback {

    /**
     * 当热更新请求完成时，该方法将会被调用。
     *
     * @param changedFiles 改变的文件。该值不为null，即使更新失败，也会使用空集合
     * @param cause        热更失败的原因。如果该值不为null，表明热更新失败
     */
    void onCompleted(@Nonnull Set<FileName<?>> changedFiles, @Nullable Throwable cause);

}
