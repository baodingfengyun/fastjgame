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
import com.wjybxx.fastjgame.util.excel.Sheet;

import javax.annotation.Nonnull;

/**
 * <h3>实现约定</h3>
 * 1. 实现类必须是无状态的，读表过程是无依赖的（这允许多线程读取）。
 * 2. 读表结果应只与当前表格有关，即：不要一个表格直接引用另一个表格的数据。
 * 3. 对于普通表格，一般一行关联一个类，建议命名：{@code XXXTemplate}，如{@code GoodsTemplate}。
 * 一般读取结果为一个map或list；如果读取为多个数据结构，此时可以自定义bean或使用元组返回读表结果。
 * 4. 对于参数表(paramSheet)，务必定义一个类型，建议命名{@code XXXParamTemplate}，如{@code GoodsParamTemplate}。
 * 5. 校验过程不要依赖表格和常量以外的东西。
 * 6. 如果需要为多表之间建立缓存，请再定义一个{@link SheetCacheBuilder}的实现。
 * 7. 建议作为表格对应的模板类的静态内部类存在。
 * 8. 需要放指定的包下，这样我们可以通过反射扫描自动加入。
 * 9. 对于param表，可以使用{@link Sheet}作为构造方法参数，在构造方法中解析表格。
 * <p>
 * excel于普通文件的最大区别：excel有多页签，而普通文件其实就只有一个页签。
 * 对于使用CSV或JSON文件的项目，可以只使用文件的管理方式。
 * <b>NOTES</b>: 如果excel设计的好，也可以采用文件的形式读，
 *
 * @param <T> 读表结果类型
 * @author wjybxx
 * date - 2020/11/18
 * github - https://github.com/hl845740757
 */
public interface SheetReader<T> {

    /**
     * 关联的表格名字 - 如果关联的表格发生变更，则会触发{@link #read(Sheet)}操作。
     */
    @Nonnull
    SheetName<T> sheetName();

    /**
     * 读取关联的表格的内容。
     * 注意：这里应当完成自身内容的校验。
     *
     * @param sheet 对应的表格内容
     */
    @Nonnull
    T read(Sheet sheet) throws Exception;

    /**
     * 将读取到的结果赋值到给定的{@link SheetDataMgr}。
     * 注意：对于读表结果为多个数据结构的表格，务必将结果平铺赋值给{@link SheetDataMgr}，这可以避免类型变更导致的大规模修改。
     *
     * @param sheetData    read读取到的结果
     * @param sheetDataMgr 用于接收结果的{@link SheetDataMgr}
     */
    void assignTo(T sheetData, SheetDataMgr sheetDataMgr);

    /**
     * 校验与其它表格之间的一致性
     *
     * @param sheetDataMgr 用于获取其它表格的数据
     */
    void validateOther(SheetDataMgr sheetDataMgr);

}
