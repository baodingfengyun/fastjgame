package com.wjybxx.fastjgame.util.concurrent;

/**
 * 如果一个异常实现了该接口，那么当其被捕获时，我们并不为其记录日志。
 * <p>
 * Q: 为什么这么实现？
 * A: 本想通过异常是否允许填充堆栈以确定是否需要打印日志的，但是并不能简单获取不到这个信息（得使用{@link Throwable}里重量级的api），
 * 为避免不必要的开销，我们通过接口来标记类型。
 * <p>
 * 注意：实现该接口的异常通常应该禁止填充堆栈，也不应包含额外数据。也就是说，实现该接口的异常通常应该是个单例。
 */
public interface NoLogRequiredException {

}