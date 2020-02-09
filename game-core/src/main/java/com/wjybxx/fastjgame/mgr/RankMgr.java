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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.misc.rank.PlayerLevelRankScore;
import com.wjybxx.fastjgame.misc.rank.RankType;
import com.wjybxx.zset.long2object.Long2ObjectZSet;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

/**
 * 排行榜组件
 * <p>
 * 游戏内的排序很多时候较为复杂，不能简单的描述为一个double或long，redis的zset不能很好的支持游戏内的排序功能。
 * 此外，由于数据存储在redis中，因此不能很好的做实时排行。
 * <p>
 * 参考redis的zset实现了java版的zset，作为该项目将来的排行榜组件。
 * <p>
 * ZSET可能还会添加新特性，因此还未合并到该项目，需要先下载到本地，再注册到本地maven仓库(也可以从game-lib模块中安装)。
 * <p>
 * ZSET代码地址：- https://github.com/hl845740757/java-zset
 * <p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/7
 * github - https://github.com/hl845740757
 */
public class RankMgr {

    /**
     * 排行榜信息
     */
    private final EnumMap<RankType, Long2ObjectZSet<Object>> rankInfo = new EnumMap<>(RankType.class);

    @Inject
    public RankMgr() {

    }

    // 等级等级排行榜测试
    public static void main(String[] args) {
        final Long2ObjectZSet<PlayerLevelRankScore> playerLevelRankZSet = Long2ObjectZSet.newZSet(PlayerLevelRankScore.handler());

        // 插入数据
        LongStream.range(1, 10000).forEach(playerGuid -> {
            playerLevelRankZSet.zadd(randomLevelScore(), playerGuid);
        });

        // 覆盖数据
        LongStream.rangeClosed(1, 10000).forEach(playerGuid -> {
            playerLevelRankZSet.zadd(randomLevelScore(), playerGuid);
        });

        System.out.println("------------------------- dump ----------------------");
        System.out.println(playerLevelRankZSet.dump());
        System.out.println();
    }

    private static PlayerLevelRankScore randomLevelScore() {
        return new PlayerLevelRankScore(ThreadLocalRandom.current().nextInt(1, 100), System.currentTimeMillis());
    }

}
