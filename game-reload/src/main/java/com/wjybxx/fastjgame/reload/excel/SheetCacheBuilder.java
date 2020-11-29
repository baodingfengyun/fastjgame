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

import com.wjybxx.fastjgame.reload.mgr.SheetDataMgr;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * 该builder用于构建多表之间的缓存
 * <h3>实现约定</h3>
 * 1. 实现类必须是无状态的，读表过程是无依赖的（这允许多线程读取）。
 * 2. 必须放在指定包下，这样我们可以通过反射扫描自动加入。
 *
 * @author wjybxx
 * date - 2020/11/24
 * github - https://github.com/hl845740757
 */
public interface SheetCacheBuilder<T> {

    /**
     * 关注的表格页签 - 如果关注的表格中的某一个或多个表格发生变更，则会触发{@link #build(SheetDataProvider)}操作。
     * 建议每次new，不会频繁调用该方法，这可以减少不必要的常量。
     */
    @Nonnull
    Set<SheetName<?>> sheetNames();

    /**
     * 根据多个表格的内容构建对应的缓存。
     * 注意：构建过程应该对依赖的表格进行必要的校验。
     * <p>
     * Q: 为什么参数不直接是{@link SheetDataMgr}？
     * A: 无法限制实现类访问的范围。比如：你在{@link #sheetNames()}中只返回了两个表格的名字，却在{@link SheetDataMgr}中获取了3个表格的读表结果。
     * <p>
     * Q: 这会导致什么问题呢？
     * A: 无法在热更时准确的重建缓存！因为在{@link #sheetNames()}与该方法之间存在一致性问题。
     * 因此，为了保证{@link #sheetNames()}与该方法的一致性，我们只传入你关心的表格的内容，以保证正确性。
     * 另外，我们为多表构建缓存的情况并不多，因此该复杂度影响的范围有限。
     * <p>
     * Q: 假如我希望在GoodsParam的读表结果{@code GoodsParamTemplate}中缓存与goods相关的其它表格的数据，如何实现？
     * A: 两种方式：
     * 方式一：获取GoodsParam的读表结果，修改{@code GoodsParamTemplate}中的数据，然后返回null或{@code GoodsParamTemplate}对象。
     * 方式二：获取GoodsParam的读表结果，将{@code GoodsParamTemplate}作为缓存对象的成员属性，最后返回缓存对象。
     * 注意：
     * 1. 方式一实现简单，且不会增加类，这有助于减少概念和使用复杂度。其缺点是无法实现不可变类，只能实现语义上的不可变对象。
     * 2. 方式二可以实现不可变对象，更佳安全，但会增加额外的类，这会增加概念和使用上的复杂度。
     * 如果外部类不可以修改对象的属性，建议使用方式一。
     *
     * @param sheetDataProvider 用于获取关联的表格的读表结果，只有你关注的表格的读表结果会被传入。
     * @return 缓存结果
     */
    @Nonnull
    T build(SheetDataProvider sheetDataProvider);

    /**
     * 将构建的缓存结果赋值到目标{@link SheetDataMgr}
     * 注意：如果将缓存结果存储到了其它表格的读表结果上，那么这里可以空实现。
     *
     * @param cacheData    {@link #build(SheetDataProvider)} 构建的缓存结果。
     * @param sheetDataMgr 目标templateMrg
     */
    void assignTo(T cacheData, SheetDataMgr sheetDataMgr);

    /**
     * 校验与其它表格之间的一致性
     * （其实暂时还未想到具体的需求）
     *
     * @param sheetDataMgr 用于获取其它表格的数据
     */
    void validateOther(SheetDataMgr sheetDataMgr);

    /**
     * 用于{@link SheetCacheBuilder}获取关联的表格数据
     */
    interface SheetDataProvider {

        /**
         * 获取表格的读表结果
         *
         * @param sheetName 表格名字
         * @param <T>       用于类型转换
         * @return 该表格的读表结果
         * @throws IllegalArgumentException 如果请求的表格不在{@link SheetCacheBuilder#sheetNames()}中，则抛出该异常
         */
        <T> T getSheetData(SheetName<T> sheetName);
    }
}
