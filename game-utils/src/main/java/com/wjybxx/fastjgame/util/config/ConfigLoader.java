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

package com.wjybxx.fastjgame.util.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 配置文件加载器(properties文件)
 * 注意：会在 本地文件夹(./config) 和 classPath(resources文件夹)尝试加载配置文件。
 * 当一个参数在两个配置文件都存在时，本地文件夹中的参数生效。
 * => 旨在可以使用外部配置文件代替jar包内配置。
 * <p>
 * 如果你不能修改Jar包中的配置文件，请注意查看日志，会打印加载文件的路径，你可以新建一个配置文件，
 * 把自己要修改的属性配置进去即可，不修改的不配置，会将两份配置文件合并，重复属性以本地文件为准。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/26 22:36
 * github - https://github.com/hl845740757
 */
public final class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    /**
     * 游戏文件夹(加载文件时，优先从该文件夹中加载文件，其次从resources下寻找)
     * 默认文件夹路径在jar包所在文件夹的config文件夹下
     * ./config
     */
    public static final String GAME_CONFIG_DIR;
    public static final ValueParser VALUE_PARSER;

    static {
        final String defaultGameConfigDir = new File("").getAbsolutePath() + File.separator + "config";
        GAME_CONFIG_DIR = System.getProperty("fastjgameConfigDir", defaultGameConfigDir);
        logger.info("GAME_CONFIG_DIR = {} ", GAME_CONFIG_DIR);

        VALUE_PARSER = DefaultValueParser.INSTANCE;
    }

    private ConfigLoader() {

    }

    /**
     * 优先在当前运行环境目录下寻找，如果当前运行环境不存在，则在jar环境下寻找。
     * 该方法仅仅是一个简便方法，使用ConfigLoader的classLoader来加载文件。
     */
    public static Params loadConfig(String fileName) throws IOException {
        return loadConfig(Thread.currentThread().getContextClassLoader(), fileName);
    }

    /**
     * 会在 本地文件夹(GameConfigDir) 和 classPath(resources文件夹)尝试加载配置文件。
     * 当一个参数在两个配置文件都存在时，本地文件夹中的参数生效。
     */
    public static Params loadConfig(ClassLoader classLoader, String fileName) throws IOException {
        final Params cfgFromGameConfigDir = tryLoadCfgFromGameConfigDir(fileName);
        final Params cfgFromJarResources = tryLoadCfgFromJarResources(classLoader, fileName);

        // 两个配置文件都不存在
        if (cfgFromGameConfigDir == null && cfgFromJarResources == null) {
            throw new FileNotFoundException(fileName);
        }

        // 两个都存在，需要合并(gameConfig下的替换jar包中的)
        if (cfgFromGameConfigDir != null && cfgFromJarResources != null) {
            return DefaultParams.merge(cfgFromJarResources, cfgFromGameConfigDir, VALUE_PARSER);
        }

        // 哪个存在返回哪个
        if (cfgFromGameConfigDir != null) {
            return cfgFromGameConfigDir;
        } else {
            return cfgFromJarResources;
        }
    }

    /**
     * 尝试在游戏配置文件夹中寻找，找不到则返回null
     */
    @Nullable
    public static Params tryLoadCfgFromGameConfigDir(String fileName) {
        try {
            final Params cfgFromGameConfigDir = loadCfgFromGameConfigDir(fileName);
            logger.info("load {} from gameConfigDir success!", fileName);
            return cfgFromGameConfigDir;
        } catch (IOException e) {
            logger.info("load {} from gameConfigDir failed", fileName, e);
            return null;
        }
    }

    /**
     * 在游戏配置文件夹中寻找
     */
    public static Params loadCfgFromGameConfigDir(String fileName) throws IOException {
        final String path = GAME_CONFIG_DIR + File.separator + fileName;
        logger.info("loadCfgFromGameConfigDir {}", path);

        File file = new File(path);
        if (file.exists() && file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(inputStreamReader);
                return DefaultParams.ofProperties(properties, VALUE_PARSER);
            }
        }
        throw new FileNotFoundException(fileName);
    }

    /**
     * 尝试在jar包resources下环境中寻找，找不到则返回null
     *
     * @param classLoader 运行环境根路径
     * @param fileName    配置文件名字
     */
    @Nullable
    public static Params tryLoadCfgFromJarResources(ClassLoader classLoader, String fileName) {
        try {
            final Params cfgFromJarResources = loadCfgFromJarResources(classLoader, fileName);
            logger.info("load {} from jarResources success!", fileName);
            return cfgFromJarResources;
        } catch (IOException e) {
            logger.info("load {} from jarResources failed", fileName, e);
            return null;
        }
    }

    /**
     * 在jar包resources下环境中寻找
     */
    public static Params loadCfgFromJarResources(ClassLoader classLoader, String fileName) throws IOException {
        logger.info("-Step1 loadCfgFromJarResources {}", fileName);
        final URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new FileNotFoundException(fileName);
        }

        logger.info("-Step2 loadCfgFromJarResources {}", resource.getPath());
        try (InputStream inputStream = resource.openStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return DefaultParams.ofProperties(properties, VALUE_PARSER);
        }
    }
}
