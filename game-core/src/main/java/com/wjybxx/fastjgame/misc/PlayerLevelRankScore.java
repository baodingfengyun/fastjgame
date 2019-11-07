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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.zset.generic.ScoreHandler;

/**
 * 玩家等级分数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/7
 * github - https://github.com/hl845740757
 */
public class PlayerLevelRankScore implements RankScore {

    private final int level;
    private final long timeStamp;

    public PlayerLevelRankScore(int level, long timeStamp) {
        this.level = level;
        this.timeStamp = timeStamp;
    }

    public int getLevel() {
        return level;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String toString() {
        return "PlayerLevelRankScore{" +
                "level=" + level +
                ", timeStamp=" + timeStamp +
                '}';
    }

    public static ScoreHandler<PlayerLevelRankScore> handler() {
        return PlayerLevelRankScoreHandler.INSTANCE;
    }

    /**
     * 玩家等级分数处理器
     */
    private static class PlayerLevelRankScoreHandler implements ScoreHandler<PlayerLevelRankScore> {

        private static final PlayerLevelRankScoreHandler INSTANCE = new PlayerLevelRankScoreHandler();

        @Override
        public int compare(PlayerLevelRankScore o1, PlayerLevelRankScore o2) {
            // 等级逆序
            final int levelCompareR = Integer.compare(o2.level, o1.level);
            if (levelCompareR != 0) {
                return levelCompareR;
            }
            // 时间戳升序
            return Long.compare(o1.timeStamp, o2.timeStamp);
        }

        @Override
        public PlayerLevelRankScore sum(PlayerLevelRankScore oldScore, PlayerLevelRankScore increment) {
            throw new UnsupportedOperationException();
        }
    }
}
