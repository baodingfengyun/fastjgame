package com.wjybxx.fastjgame.util.log;

import org.slf4j.event.Level;

/**
 * debug日志级别（没有使用而枚举是故意的）。
 * 注意：通常我们开启这里的日志，表示我们期望一定输出日志。
 * 因此调用{@link org.slf4j.Logger}的方法时并不是它的{@link Level#DEBUG}，而是{@link Level#INFO}.
 */
public class DebugLogLevels {

    /**
     * 不打印日志
     */
    public static final int NONE = 0;
    /**
     * 打印简单日志
     */
    public static final int SIMPLE = 1;
    /**
     * 打印详细日志
     */
    public static final int DETAIL = 2;

    public static int checkedLevel(int level) {
        if (level < NONE) {
            return NONE;
        }
        if (level > DETAIL) {
            return DETAIL;
        }
        return level;
    }
}
