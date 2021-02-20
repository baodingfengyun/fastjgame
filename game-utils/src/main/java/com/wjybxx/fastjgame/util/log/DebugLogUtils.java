package com.wjybxx.fastjgame.util.log;

import javax.annotation.Nullable;

public class DebugLogUtils {

    public static String logOf(int level, @Nullable Object object) {
        if (object == null) {
            return "null";
        }
        if (level == DebugLogLevels.SIMPLE) {
            return (object instanceof DebugLogFriendlyObject) ? ((DebugLogFriendlyObject) object).toSimpleLog() : object.toString();
        } else {
            return (object instanceof DebugLogFriendlyObject) ? ((DebugLogFriendlyObject) object).toDetailLog() : object.toString();
        }
    }

}