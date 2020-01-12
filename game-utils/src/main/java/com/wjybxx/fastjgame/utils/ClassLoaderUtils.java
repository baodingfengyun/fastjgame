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

package com.wjybxx.fastjgame.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 类加载器工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/12
 * github - https://github.com/hl845740757
 */
public class ClassLoaderUtils {

    private ClassLoaderUtils() {
    }

    /**
     * 从class文件中加载类信息
     *
     * @param classFile 要加载的class文件
     * @return class bytes
     * @throws IOException error
     */
    public static byte[] loadClassFromFile(File classFile) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(classFile)) {
            final int available = fileInputStream.available();
            final byte[] result = new byte[available];
            fileInputStream.read(result);
            return result;
        }
    }

}