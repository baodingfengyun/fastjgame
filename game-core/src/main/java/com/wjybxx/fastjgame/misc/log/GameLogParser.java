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

import com.wjybxx.fastjgame.core.LogParser;
import com.wjybxx.fastjgame.core.LogRecordDTO;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;

/**
 * 游戏日志解析器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/11
 * github - https://github.com/hl845740757
 */
public class GameLogParser implements LogParser<GameLogRecord> {

    @Override
    public GameLogRecord parse(LogRecordDTO recordDTO) {
        final String[] kvPairs = StringUtils.split(recordDTO.data(), GameLogDirector.ENTRY_SEPARATOR);
        final LinkedHashMap<String, String> dataMap = CollectionUtils.newLinkedHashMapWithExpectedSize(kvPairs.length);
        for (String pair : kvPairs) {
            final String[] keyAndValue = StringUtils.split(pair, GameLogDirector.KV_SEPARATOR);
            assert keyAndValue.length == 2;
            dataMap.put(keyAndValue[0], keyAndValue[1]);
        }

        final String logType = dataMap.get(LogKey.LOG_TYPE.toString());
        return new GameLogRecord(LogType.valueOf(logType), dataMap);
    }
}
