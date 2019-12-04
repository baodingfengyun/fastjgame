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

import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.misc.RoleType;
import org.apache.commons.lang3.StringUtils;
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

    // 内网服务器间通信端口
    public static final PortRange INNER_TCP_PORT_RANGE = new PortRange(20001, 20500);
    public static final PortRange INNER_HTTP_PORT_RANGE = new PortRange(21001, 21500);

    // 外网与玩家通信端口
    public static final PortRange OUTER_TCP_PORT_RANGE = new PortRange(23001, 23500);
    public static final PortRange OUTER_WS_PORT_RANGE = new PortRange(24001, 24500);
    public static final PortRange OUTER_HTTP_PORT_RANGE = new PortRange(25001, 25500);

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
     */
    public static byte[] serializeToStringBytes(int integer) {
        return String.valueOf(integer).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 从字符串字节数组中解析一个int
     */
    public static int parseIntFromStringBytes(byte[] bytes) {
        return Integer.parseInt(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * 当json对象字节数组表示一个map对象时，返回对应的map对象
     *
     * @param jsonBytes json序列化的对象
     * @return jsonMap
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> newJsonMap(byte[] jsonBytes) {
        return JsonUtils.parseJsonBytesToMap(jsonBytes, LinkedHashMap.class, String.class, String.class);
    }

    /**
     * 是否是null字符串或空白字符串
     *
     * @param str 待检查的字符串
     * @return true or false
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    // db

    /**
     * 获取中心服的数据库名字
     *
     * @param actualServerId 服id
     * @return dbName
     */
    public static String centerDBName(CenterServerId actualServerId) {
        // platform的名字可能被修改，但是数字标记不可以被修改
        return RoleType.CENTER + "_" + actualServerId.getPlatformType().getNumber() + "_" + actualServerId.getInnerServerId();
    }

    /**
     * 战区数据库
     *
     * @param warzoneId 战区id
     * @return dbName
     */
    public static String warzoneDBName(int warzoneId) {
        return RoleType.WARZONE + "_" + warzoneId;
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