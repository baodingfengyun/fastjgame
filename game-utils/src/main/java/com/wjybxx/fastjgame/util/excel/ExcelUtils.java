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

import com.wjybxx.fastjgame.util.function.FunctionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

    private ExcelUtils() {
    }

    /**
     * 读取一个excel页签
     *
     * @param file excel文件
     * @return
     */
    public static Map<String, Sheet> readExcel(File file, CellValueParser parser, Predicate<String> sheetNameFilter) throws Exception {
        return readExcel(file, parser, sheetNameFilter, 16 * 1024);
    }

    /**
     * 读取一个excel页签
     *
     * @param file       excel文件
     * @param bufferSize 缓冲区大小
     * @return
     */
    public static Map<String, Sheet> readExcel(File file, CellValueParser parser, Predicate<String> sheetNameFilter, int bufferSize) throws Exception {
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

    public static void main(String[] args) throws Exception {
        // 测试表格放在了config目录下
        final String path = "./config/test.xlsx";
        final File file = new File(path);
        final Map<String, Sheet> sheetMap = readExcel(file, new DefaultCellValueParser(), FunctionUtils.alwaysTrue());
        System.out.println(sheetMap);

        final Sheet skillParam = sheetMap.get("TestParam");
        skillParam.getSheetContent().asParamSheetContent().valueCells()
                .forEach(System.out::println);

        System.out.println("----------------------------------");
        System.out.println(Arrays.toString(skillParam.getValueCell("ONE_DIMENSIONAL_ARRAY").readAsArray(int[].class)));
        System.out.println(Arrays.deepToString(skillParam.getValueCell("TWO_DIMENSIONAL_ARRAY").readAsArray(int[][].class)));
    }

}
