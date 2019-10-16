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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.utils.ConfigLoader;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;

/**
 * 网络层配置管理器，不做热加载，网络层本身不需要经常修改，而且很多配置也无法热加载。
 * 参数含义及单位见get方法或配置文件
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 15:09
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetConfigManager {

    /**
     * 网络包配置文件名字
     */
    public static final String NET_CONFIG_NAME = "net_config.properties";

    /**
     * 原始配置文件
     */
    private final ConfigWrapper configWrapper;

    private final int httpRequestTimeout;
    private final int httpSessionTimeout;

    @Inject
    public NetConfigManager() throws IOException {
        configWrapper = ConfigLoader.loadConfig(NetConfigManager.class.getClassLoader(), NET_CONFIG_NAME);

        httpRequestTimeout = configWrapper.getAsInt("httpRequestTimeout");
        httpSessionTimeout = configWrapper.getAsInt("httpSessionTimeout");
    }

    /**
     * 获取原始的config保证其，以获取不在本类中定义的属性
     */
    public ConfigWrapper properties() {
        return configWrapper;
    }

    /**
     * okHttpClient请求超时时间(秒)
     */
    public int httpRequestTimeout() {
        return httpRequestTimeout;
    }

    /**
     * http会话超时时间(秒)
     * 此外，它也是检查session超时的间隔
     */
    public int httpSessionTimeout() {
        return httpSessionTimeout;
    }

}
