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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.ChannelHandler;

import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedList;

/**
 * 不可共享的带有缓冲区的sender，只允许用户线程调用发送消息。
 * <p>
 * Q：为何也不允许responsechannel跨线程使用？
 * A：会导致微妙的时序问题。
 * eg：用户将responsechannel传递给另外一个线程，另一个线程会在后台完成计算，并将结果直接写入responsechannel。
 * 当用户感知到任务完成时，会给用户造成一种结果已经写入缓冲区（或网络层）的错觉，用户如果再发送新的消息，可能就造成了时序错误！
 * <p>
 * Q：为何说是错觉？
 * A：用户感知到任务完成，完全可能在响应写入缓冲区之前。最简单的情况就是阻塞式调用，此时后台线程提交的消息还在用户队列中等待。
 * <p>
 * 这种情况不一定会出现，出现也不一定会出现逻辑错误，即使出现了逻辑错误，可能也没有明显表现，很难测试出来，也很难复现，就这样神不知鬼不觉的过去了。
 * 即使你进行了说明，用户还是可能用错。反而不进行支持，抛出异常是更好的方式，因为它安全！
 * <p>
 * 其实在Netty中就存在这样的问题，在{@link ChannelHandler}中发送的消息和在外部发送的消息之间没有顺序保证，只是说我们一般不会这样用。
 * <p>
 * 该类是线程安全的 --- 因为其它线程调用直接抛错......
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/4
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class UnsharableSender extends AbstractSender {

    /**
     * 用户缓存的消息
     */
    private LinkedList<SenderTask> buffer = new LinkedList<>();

    public UnsharableSender(AbstractSession session) {
        super(session);
    }

    @Override
    protected void write(SenderTask task) {
        if (userEventLoop().inEventLoop()) {
            buffer.add(task);
            // 检查是否需要清空缓冲区
            if (buffer.size() >= session.getNetConfigManager().flushThreshold()) {
                flushBuffer();
            }
        } else {
            throw new IllegalStateException("unsharable");
        }
    }

    @Override
    protected void writeAndFlush(SenderTask task) {
        if (userEventLoop().inEventLoop()) {
            if (buffer.size() == 0) {
                netEventLoop().execute(task);
            } else {
                buffer.add(task);
                flushBuffer();
            }
        } else {
            throw new IllegalStateException("unsharable");
        }
    }

    @Override
    public void flush() {
        if (userEventLoop().inEventLoop()) {
            if (buffer.size() == 0) {
                return;
            }
            if (session.isActive()) {
                flushBuffer();
            }
            // else 等待关闭
        } else {
            throw new IllegalStateException("unsharable");
        }
    }

    /**
     * 清空缓冲区(用户线程下) - 批量提交，减少竞争
     */
    private void flushBuffer() {
        if (buffer.size() == 1) {
            netEventLoop().execute(buffer.pollFirst());
        } else {
            final LinkedList<SenderTask> oldBuffer = exchangeBuffer();
            netEventLoop().execute(() -> {
                for (SenderTask senderTask : oldBuffer) {
                    senderTask.run();
                }
            });
        }


    }

    /**
     * 交换缓冲区(用户线程下)
     *
     * @return oldBuffer
     */
    private LinkedList<SenderTask> exchangeBuffer() {
        LinkedList<SenderTask> result = this.buffer;
        this.buffer = new LinkedList<>();
        return result;
    }

    /**
     * 网络层请求清空缓冲器 - 因为数据只能用户线程访问，因此需要提交执行。
     */
    @Override
    public void clearBuffer() {
        ConcurrentUtils.tryCommit(userEventLoop(), this::cancelAll);
    }

    private void cancelAll() {
        SenderTask senderTask;
        while ((senderTask = buffer.pollFirst()) != null) {
            ConcurrentUtils.safeExecute((Runnable) senderTask::cancel);
        }
    }

}
