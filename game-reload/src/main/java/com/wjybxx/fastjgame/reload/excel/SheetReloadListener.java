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

package com.wjybxx.fastjgame.reload.excel;

import java.util.Set;

/**
 * 表格热更新回调。
 * <p>
 * 注意：实现必须幂等！即：当表格相同时，无论执行多少次，其结果必须是相同的。
 * 换言之，无论表格是否改变，该代码的执行都不会影响正确性，这要求你仔细思考你的设计。
 * <p>
 * 强制校验：为确保大家的实现符合要求，测试期间会不定期调用，以检验其正确性。
 *
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
public interface SheetReloadListener {

    /**
     * 当关心的表格中的一个或多个发生改变时，将调用该方法。
     *
     * @param sheetNames        注册时绑定的sheet名字。
     * @param changedSheetNames 变化的sheet的名字。
     */
    void afterReload(Set<SheetName<?>> sheetNames, Set<SheetName<?>> changedSheetNames) throws Exception;

}
