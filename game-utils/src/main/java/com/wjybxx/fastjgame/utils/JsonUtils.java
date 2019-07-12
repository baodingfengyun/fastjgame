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

package com.wjybxx.fastjgame.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.IOException;
import java.util.Map;

/**
 * Json工具类（字符集UTF-8）。
 *
 *
 * 最终还是选择了Jackson。
 * 建议：Gson也可以一用，但是最好不要使用FastJson，fastJson生成的json格式并不标准，有兼容性问题，此外代码质量不好。
 * jackson的代码质量真的很不错，扩展性很好。
 *
 * 这些方法真的很强大易用：
 * {@link TypeFactory#constructMapType(Class, Class, Class)}
 * {@link TypeFactory#constructArrayType(Class)}
 * {@link TypeFactory#constructCollectionType(Class, Class)}
 *
 * 简单运用：
 * {@link #parseJsonToMap(String, Class, Class, Class)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/12 23:05
 * github - https://github.com/hl845740757
 */
public class JsonUtils {

    /**
     * ObjectMapper自称自己是线程安全的，但是好像还是有点bug
     */
    private static final ThreadLocal<ObjectMapper> MAPPER_THREAD_LOCAL = ThreadLocal.withInitial(ObjectMapper::new);

    /**
     * 如果提供的现有的方法，不能满足方法，可以获取mapper对象。
     * @return ObjectMapper
     */
    public static ObjectMapper getMapper(){
        return MAPPER_THREAD_LOCAL.get();
    }

    // ---------------------------------- 基本支持 ---------------------------
    /**
     * 将一般bean转换为json字符串
     * @param obj bean
     * @return String
     */
    public static String toJson(Object obj) {
        try {
            return getMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // 之所以捕获，是因为，出现异常的地方应该是非常少的
            throw new RuntimeException(e);
        }
    }

    /**
     * 将一般bean转换为json对应的字节数组
     * @param obj bean
     * @return bytes
     */
    public static byte[] toJsonBytes(Object obj) {
        try {
            return getMapper().writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            // 之所以捕获，是因为，出现异常的地方应该是非常少的
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析json字符串为java对象。
     * @param json json字符串
     * @param clazz json字节数组对应的类
     * @param <T> 对象类型
     * @return 反序列化得到的对象
     */
    public static <T> T parseJson(String json, Class<T> clazz) {
        try {
            return getMapper().readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析json字符串对应的字节数组为java对象。
     * @param jsonBytes json字符串UTF-8编码后的字节数组
     * @param clazz json字节数组对应的类
     * @param <T> 对象类型
     * @return 反序列化得到的对象
     */
    public static <T> T parseJsonBytes(byte[] jsonBytes, Class<T> clazz) {
        try {
            return getMapper().readValue(jsonBytes, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------- Map支持 ---------------------------

    /**
     * 解析json字符串为map对象。
     * @param json json字符串
     * @param mapClass map的具体类型
     * @param keyClass key的具体类型
     * @param valueClass value的具体类型
     * @param <M> map类型
     * @return map
     */
    public static <M extends Map> M parseJsonToMap(String json, Class<M> mapClass, Class<?> keyClass, Class<?> valueClass) {
        ObjectMapper mapper = getMapper();
        MapType mapType = mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        try {
            return mapper.readValue(json, mapType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析json字符串的字节数组为map对象。
     * @param jsonBytes json字符串对应的字节数组
     * @param mapClass map的具体类型
     * @param keyClass key的具体类型
     * @param valueClass value的具体类型
     * @param <M> map类型
     * @return map
     */
    public static <M extends Map> M parseJsonBytesToMap(byte[] jsonBytes, Class<M> mapClass, Class<?> keyClass, Class<?> valueClass) {
        ObjectMapper mapper = getMapper();
        MapType mapType = mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        try {
            return mapper.readValue(jsonBytes, mapType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Int2IntMap data = new Int2IntOpenHashMap();
        data.put(1,5);
        data.put(6,7);

        String json = toJson(data);
        System.out.println("json = " + json);

        Int2IntMap rData = parseJsonToMap(json,Int2IntOpenHashMap.class, Integer.class, Integer.class);
        System.out.println("map = " + rData);

        System.out.println("equals = " + data.equals(rData));
    }
}
