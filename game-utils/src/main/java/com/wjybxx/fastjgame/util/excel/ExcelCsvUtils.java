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
import java.nio.charset.Charset;

/**
 * Excel和Csv表格的辅助工具
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/11 16:18
 * github - https://github.com/hl845740757
 */
public final class ExcelCsvUtils {

    private ExcelCsvUtils() {
    }

    // ---------------------------------- CSV ----------------------------

    /**
     * 读取CSV表格。
     * 默认编码格式 GBK
     *
     * @param file         csv文件
     * @param nameRowIndex 属性名所在行
     */
    public static Sheet readCsv(File file, int nameRowIndex) throws Exception {
        try (final CSVReader reader = new CSVReader()) {
            return reader.readCfg(file, nameRowIndex);
        }
    }

    /**
     * CSV 不支持分页，但是可以指定第几行为属性名
     *
     * @param file         csv文件
     * @param nameRowIndex 属性名所在行索引
     * @param charset      指定csv文件编码格式
     */
    public static Sheet readCsv(File file, int nameRowIndex, Charset charset) throws Exception {
        try (final CSVReader reader = new CSVReader(charset)) {
            return reader.readCfg(file, nameRowIndex);
        }
    }

    // ---------------------------------- EXCEL ----------------------------

    /**
     * 读取一个excel页签
     *
     * @param file         excel文件
     * @param sheetName    页签名字
     * @param nameRowIndex 属性名所在行
     */
    public static Sheet readExcel(File file, String sheetName, int nameRowIndex) throws Exception {
        return readExcel(file, sheetName, nameRowIndex, 16 * 1024);
    }

    /**
     * 读取一个excel页签
     *
     * @param file         excel文件
     * @param sheetName    页签名字
     * @param nameRowIndex 属性名所在行
     * @param bufferSize   缓冲区大小
     */
    public static Sheet readExcel(File file, String sheetName, int nameRowIndex, int bufferSize) throws Exception {
        try (final ExcelReader reader = new ExcelReader(sheetName, bufferSize)) {
            return reader.readCfg(file, nameRowIndex);
        }
    }

    public static void main(String[] args) throws Exception {
        final String path = "D:\\work\\desgin\\config\\Chapter.xlsx";
        final File file = new File(path);
        final Sheet sheet = readExcel(file, "Sample", 0);
        System.out.println(sheet);
    }

}
