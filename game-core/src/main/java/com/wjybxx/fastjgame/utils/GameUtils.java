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

import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.PortRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 游戏帮助类;
 * (不知道放哪儿的方法就放这里)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/12 15:33
 * github - https://github.com/hl845740757
 */
public class GameUtils {

    private static final Logger logger = LoggerFactory.getLogger(GameUtils.class);

    /**
     * TCP监听端口
     */
    public static final PortRange INNER_TCP_PORT_RANGE = new PortRange(20001, 20500);
    /**
     * http监听端口
     */
    public static final PortRange INNER_HTTP_PORT_RANGE = new PortRange(21001, 21500);
    /**
     * localhost:X类型地址
     * 两台服务器在同一台机器上时，不走网卡。
     */
    public static final PortRange LOCAL_TCP_PORT_RANGE = new PortRange(22001, 22500);
    /**
     * 与玩家之间通信端口
     */
    public static final PortRange OUTER_TCP_PORT_RANGE = new PortRange(23001, 23500);
    public static final PortRange OUTER_WS_PORT_RANGE = new PortRange(24001, 24500);

    private GameUtils() {
    }

    /**
     * 安静的关闭，忽略产生的异常
     *
     * @param closeable 实现了close方法的对象
     */
    public static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Throwable e) {
                logger.info("", e);
            }
        }
    }

    /**
     * 将int序列化为字符串字节数组，字符串具有更好的可读性
     *
     * @param integer
     * @return
     */
    public static byte[] serializeToStringBytes(int integer) {
        return String.valueOf(integer).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从字符串字节数组中解析一个int
     *
     * @param bytes
     * @return
     */
    public static int parseIntFromStringBytes(byte[] bytes) {
        return Integer.parseInt(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * 使用UTF-8字符集创建字符串
     *
     * @param utf8Bytes 使用UTF-8编码的字节数组
     * @return
     */
    public static String newString(byte[] utf8Bytes) {
        return new String(utf8Bytes, StandardCharsets.UTF_8);
    }

    /**
     * 当json对象字节数组表示一个map对象时，返回对应的map对象
     *
     * @param jsonBytes json序列化的对象
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> newJsonMap(byte[] jsonBytes) {
        return JsonUtils.parseJsonBytesToMap(jsonBytes, LinkedHashMap.class, String.class, String.class);
    }


    /**
     * 是否是null字符串或空字符串
     *
     * @param str 待检查的字符串
     * @return true or false
     */
    public static boolean isNullOrEmptyString(String str) {
        return null == str || str.length() == 0 || str.trim().length() == 0;
    }

    // db

    /**
     * 获取中心服的数据库名字
     *
     * @param platformType   运营平台
     * @param actualServerId 服id
     * @return dbName
     */
    public static String centerDBName(PlatformType platformType, int actualServerId) {
        // platform的名字可能被修改，但是数字标记不可以被修改
        return "center_" + platformType.getNumber() + "_" + actualServerId;
    }

    /**
     * 战区数据库
     *
     * @param warzoneId 战区id
     * @return dbName
     */
    public static String warzoneDBName(int warzoneId) {
        return "warzone_" + warzoneId;
    }

    /**
     * 全局数据库名字
     *
     * @return dbName
     */
    public static String globalDBName() {
        return "global";
    }

}