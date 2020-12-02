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
package com.wjybxx.fastjgame.util.excel;

import java.util.List;

/**
 * Excel表的一页
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:09
 * github - https://github.com/hl845740757
 */
public final class Sheet {

    /**
     * 文件名字
     * 如: bag.xlsx
     */
    private final String fileName;
    /**
     * 页签名
     * 如：bag
     */
    private final String sheetName;
    /**
     * 文件的第几页，默认应该为0
     * 索引0开始
     */
    private final int sheetIndex;

    /**
     * 表格内容
     */
    private final SheetContent sheetContent;

    /**
     * new instance
     *
     * @param fileName     文件名
     * @param sheetName    页签名
     * @param sheetIndex   第几页
     * @param sheetContent 表格内容
     */
    public Sheet(String fileName, String sheetName, int sheetIndex, SheetContent sheetContent) {
        this.fileName = fileName;
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.sheetContent = sheetContent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public SheetContent getSheetContent() {
        return sheetContent;
    }

    /**
     * 普通表格使用该方法读取每一行
     */
    public List<ValueRow> getValueRows() {
        if (sheetContent instanceof DefaultSheetContent) {
            return ((DefaultSheetContent) sheetContent).getValueRows();
        }
        throw notDefaultSheetException();
    }

    /**
     * 普通表格获取内容
     */
    public DefaultSheetContent getContentAsDefaultSheet() {
        return sheetContent.asDefaultSheetContent();
    }

    /**
     * param表使用这种方法读取每一个字段
     */
    public ParamSheetContent getContentAsParamSheet() {
        return sheetContent.asParamSheetContent();
    }

    /**
     * @return 获取表格的总行数
     */
    public int totalRowCount() {
        return sheetContent.totalRowCount();
    }

    /**
     * @return 表格的有效内容行行数
     */
    public int valueRowCount() {
        return sheetContent.valueRowCount();
    }

    static RuntimeException notDefaultSheetException() {
        return new IllegalStateException("this sheet might be a param sheet?");
    }

    static IllegalStateException notParamSheetException() {
        return new IllegalStateException("this sheet might be a normal sheet?");
    }

    @Override
    public String toString() {
        return "Sheet{" +
                "fileName='" + fileName + '\'' +
                ", sheetName='" + sheetName + '\'' +
                ", sheetIndex=" + sheetIndex +
                ", content=" + sheetContent +
                '}';
    }
}
