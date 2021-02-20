package com.wjybxx.fastjgame.util.log;

import javax.annotation.Nonnull;

/**
 * 对debug友好的对象
 * 如果一个对象实现了该接口，则在打印日志时会根据日志环境调用对应的方法，如果没有实现该对象，则始终调用对象的{@link #toString()}方法。
 */
public interface DebugLogFriendlyObject {

    /**
     * @return 生成简单的日志信息，关键信息必须包含
     */
    @Nonnull
    String toSimpleLog();

    /**
     * @return 生成详细的日志信息
     */
    @Nonnull
    String toDetailLog();

}