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

package com.wjybxx.fastjgame.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * gzip 压缩/解压缩工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/23
 * github - https://github.com/hl845740757
 */
public class GZipUtils {

    public static byte[] gzip(byte[] content) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPOutputStream gos = new GZIPOutputStream(baos);
        gos.write(content, 0, content.length);
        gos.flush();
        // 这里有点违反常规，用不了try-with-resource，必须先显式调用close....
        gos.close();
        return baos.toByteArray();
    }

    public static byte[] unGzip(byte[] content) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(content));
        final byte[] buffer = new byte[1024];
        int n;
        while ((n = gis.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    public static byte[] gzipString(String content, Charset charset) throws IOException {
        return gzip(content.getBytes(charset));
    }

    public static String unGzipString(byte[] content, Charset encode) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(content));
        final byte[] buffer = new byte[1024];
        int n;
        while ((n = gis.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toString(encode);
    }

}