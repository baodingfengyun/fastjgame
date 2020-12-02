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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:18
 * github - https://github.com/hl845740757
 */
public final class ExcelUtils {

    public static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private ExcelUtils() {
    }

    /**
     * 读取excel的所有页签
     */
    public static Map<String, Sheet> readExcel(File file, CellValueParser parser, Predicate<String> sheetNameFilter) throws IOException {
        return readExcel(file, parser, sheetNameFilter, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 读取excel的所有页签
     */
    public static Map<String, Sheet> readExcel(File file, CellValueParser parser, Predicate<String> sheetNameFilter, int bufferSize) throws IOException {
        try (final ExcelReader reader = new ExcelReader(file, parser, sheetNameFilter, bufferSize)) {
            return reader.readSheets();
        }
    }

    /**
     * 获取excel的所有sheet名字
     *
     * @param file            excel文件
     * @param sheetNameFilter sheet过滤器
     * @return 所有sheet页的名字
     */
    public static List<String> readExcelSheetNames(File file, Predicate<String> sheetNameFilter) throws IOException {
        return ExcelReader.readExcelSheetNames(file, sheetNameFilter);
    }

    /**
     * 只读取excel的第一个sheet，且使用文件的简单名作为sheetName。
     * simpleName: 文件名去掉类型后缀，比如：bag.xlsx，则sheetName为bag
     * <p>
     * 部分项目excel只有一个sheet，且sheet是没有名称的，是通过文件名区分的，需要这种方式读取。
     */
    public static Sheet readSheetUseFileSimpleName(File file, CellValueParser parser) throws IOException {
        return ExcelReader.readSheetUseFileSimpleName(file, parser, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 只读取excel的第一个sheet，且使用文件的简单名作为sheetName。
     * simpleName: 文件名去掉类型后缀，比如：bag.xlsx，则sheetName为bag
     * <p>
     * 部分项目excel只有一个sheet，且sheet是没有名称的，是通过文件名区分的，需要这种方式读取。
     */
    public static Sheet readSheetUseFileSimpleName(File file, CellValueParser parser, int bufferSize) throws IOException {
        return ExcelReader.readSheetUseFileSimpleName(file, parser, bufferSize);
    }

}
