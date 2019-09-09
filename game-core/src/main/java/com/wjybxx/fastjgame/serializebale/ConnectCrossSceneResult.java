package com.wjybxx.fastjgame.serializebale;

import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

/**
 * 连接跨服节点结果
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/22
 * github - https://github.com/hl845740757
 */
@SerializableClass
public class ConnectCrossSceneResult {

    /**
     * 配置的区域
     */
    @SerializableField(number = 1)
    private List<Integer> configuredRegions;
    /**
     * 实际启动的区域
     */
    @SerializableField(number = 2)
    private List<Integer> activeRegions;

    private ConnectCrossSceneResult() {
        // 序列化专用
    }

    public ConnectCrossSceneResult(IntList configuredRegions, IntList activeRegions) {
        this.configuredRegions = configuredRegions;
        this.activeRegions = activeRegions;
    }

    public List<Integer> getConfiguredRegions() {
        return configuredRegions;
    }

    public List<Integer> getActiveRegions() {
        return activeRegions;
    }
}
