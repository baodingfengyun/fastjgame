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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 编解码工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 */
public final class CodecUtils {

    private CodecUtils() {

    }

    public static byte[] getBytesUTF8(@Nonnull String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static String newStringUTF8(@Nonnull byte[] binaryData) {
        return new String(binaryData, StandardCharsets.UTF_8);
    }

    // ------------------------------ BASE64 编解码字节数组 ------------------------------

    /**
     * BAS64编码一个字节数组
     *
     * @param binaryData 需要被编码的字节数组
     * @return UTF8表示形式的字节数组
     */
    public static byte[] encodeBase64(@Nonnull byte[] binaryData) {
        return Base64.encodeBase64(binaryData);
    }

    /**
     * 解码一个BASE64编码的字节数组
     *
     * @param base64Data 被编码的字节数组
     * @return UTF8表示形式的字节数组
     */
    public static byte[] decodeBase64(@Nonnull byte[] base64Data) {
        return Base64.decodeBase64(base64Data);
    }

    // ------------------------------  BASE64 编解码字符串 ------------------------------

    /**
     * BAS64编码一个字符串
     *
     * @param data 需要被BASE64编码的字符串
     * @return UTF8表示形式的字符串
     */
    public static String encodeBase64String(@Nonnull String data) {
        return Base64.encodeBase64String(getBytesUTF8(data));
    }

    /**
     * BAS64编码一个字符串
     *
     * @param base64String 被BASE64编码的字符串
     * @return UTF8表示形式的字符串
     */
    public static String decodeBase64String(@Nonnull String base64String) {
        return newStringUTF8(Base64.decodeBase64(base64String));
    }

    // ---------------------------------  url 编解码 ----------------------------------------

    /**
     * 以URL编码格式编码编码一个字符串
     *
     * @param data 需要以URL格式编码的数据
     * @return URL编码后的字符串
     */
    public static String urlEncode(String data, Charset charset) {
        return URLEncoder.encode(data, charset);
    }

    /**
     * 解码一个被URL格式编码的数据
     *
     * @param urlData 被URL格式编码的数据
     * @return 解码后的字符串
     */
    public static String urlDecode(String urlData, Charset charset) {
        return URLDecoder.decode(urlData, charset);
    }
    // ---------------------------------  MD5 编码 ------------------------------------------

    public enum Md5Mode {

        UPPER_CASE,
        LOWER_CASE;

        public boolean isLowerCase() {
            return this == LOWER_CASE;
        }
    }

    /**
     * 计算字节数组的MD5，并返回一个32个字符的十六进制字符串
     *
     * @param data 待计算的字节数组
     * @return 32个字符的十六进制字符串
     */
    public static String md5Hex(@Nonnull byte[] data, Md5Mode mode) {
        return new String(Hex.encodeHex(DigestUtils.md5(data), mode.isLowerCase()));
    }

    /**
     * 计算字符串的MD5，并返回一个32个字符的十六进制字符串
     *
     * @param data 待计算的字符串
     * @return 32个字符的十六进制字符串
     */
    public static String md5Hex(@Nonnull String data, Md5Mode mode) {
        return new String(Hex.encodeHex(DigestUtils.md5(data), mode.isLowerCase()));
    }

    /**
     * 计算文件的MD5，并返回一个32个字符的十六进制字符串。
     *
     * @param file 要计算md5的文件
     * @return 32个字符的十六进制字符串
     */
    public static String md5Hex(@Nonnull File file, Md5Mode mode) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return md5Hex(fileInputStream, mode);
        }
    }

    /**
     * 计算inputStream剩余内容的MD5，并返回一个32个字符的十六进制字符串
     *
     * @param data 待计算的输入流，注意：该输入流并不会自动关闭！
     * @return 32个字符的十六进制字符串
     */
    public static String md5Hex(@Nonnull InputStream data, Md5Mode mode) throws IOException {
        return new String(Hex.encodeHex(DigestUtils.md5(data), mode.isLowerCase()));
    }

    // -----------------------------------  测试 --------------------------------------------

    public static void main(String[] args) throws IOException {
        String md5 = md5Hex(CodecUtils.class.getCanonicalName(), Md5Mode.UPPER_CASE);
        System.out.println(md5);
    }
}
