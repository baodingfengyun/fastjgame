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

package com.wjybxx.fastjgame.misc.log;

import com.wjybxx.fastjgame.configwrapper.Params;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 游戏日志的消费视图
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public class GameLogRecord extends Params {

    private final LogType logType;

    private final Map<String, String> dataMap;

    public GameLogRecord(LogType logType, Map<String, String> dataMap) {
        this.logType = logType;
        this.dataMap = dataMap;
    }

    public LogTopic getTopic() {
        return logType.topic;
    }

    public LogType getLogType() {
        return logType;
    }

    public Map<String, String> getDataMap() {
        return dataMap;
    }

    @Nullable
    @Override
    public String getAsString(String key) {
        return dataMap.get(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(dataMap.keySet());
    }
}
