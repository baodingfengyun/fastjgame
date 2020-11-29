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

package com.wjybxx.fastjgame.reload.mgr;

/**
 * 表格数据管理器。
 * <h3>约定</h3>
 * 1. 该类最好不要有任何额外依赖，只用来保存表格引用，只是一个简单的表格数据容器。
 * 2. 该类存储所有的读表结果，包括多表建立的缓存。
 * 3. 请将数据平铺赋值到对象上。
 *
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
public interface SheetDataMgr {

    /**
     * @return 一个新的对象，不包含任何表格数据的对象。
     */
    SheetDataMgr newInstance();

}
