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
import java.util.Map;

/**
 * Excel或CSV等配置表的一页
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:09
 * github - https://github.com/hl845740757
 */
public final class Sheet {

    /**
     * 文件名字
     * 如: bag.xlsx / bag.csv
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
     * 表头行
     */
    private final List<SheetRow> headerRows;

    /**
     * 表格的内容
     */
    private final SheetContent sheetContent;

    /**
     * new instance
     *
     * @param fileName     文件名
     * @param sheetName    页签名字
     * @param sheetIndex   第几页
     * @param headerRows   表头行
     * @param sheetContent 内容部分
     */
    public Sheet(String fileName, String sheetName, int sheetIndex, List<SheetRow> headerRows, SheetContent sheetContent) {
        this.fileName = fileName;
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.headerRows = headerRows;
        this.sheetContent = sheetContent;
    }

    public List<SheetRow> getContentAsList() {
        if (sheetContent instanceof DefaultSheetContent) {
            return ((DefaultSheetContent) sheetContent).getSheetRows();
        }
        throw new IllegalStateException("this sheet may be a param sheet?");
    }

    public Map<String, CellValue> getContentAsMap() {
        if (sheetContent instanceof ParamSheetContent) {
            return ((ParamSheetContent) sheetContent).getName2CellValueMap();
        }
        throw new IllegalStateException("this sheet may be a normal sheet?");
    }

    public CellValue getCellValue(String name) {
        if (sheetContent instanceof ParamSheetContent) {
            return ((ParamSheetContent) sheetContent).getCellValue(name);
        }
        throw new IllegalStateException("this sheet may be a normal sheet?");
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

    public List<SheetRow> getHeaderRows() {
        return headerRows;
    }

    public SheetContent getSheetContent() {
        return sheetContent;
    }

    public int headerRowCount() {
        return headerRows.size();
    }

    public int contentRowCount() {
        return sheetContent.rowCount();
    }

    /**
     * @return 获取总行数
     */
    public int allRowCont() {
        return headerRowCount() + contentRowCount();
    }

    @Override
    public String toString() {
        return "Sheet{" +
                "fileName='" + fileName + '\'' +
                ", sheetName='" + sheetName + '\'' +
                ", sheetIndex=" + sheetIndex +
                ", headerRows=" + headerRows +
                ", sheetContent=" + sheetContent +
                '}';
    }
}
