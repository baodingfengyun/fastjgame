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

package com.wjybxx.fastjgame.net.socket;

import com.wjybxx.fastjgame.net.socket.outer.OuterSocketMessage;

import java.util.LinkedList;

/**
 * 消息队列，可与tcp的收发缓冲区比较
 * （知识点：滑动窗口，捎带确认）
 * <pre>
 * 消息队列的视图大致如下：
 *
 *  ack   ~    ack
 *  ↓           ↓
 *  ↓           ↓nextSequence
 * |---------------------------
 * | sentQueue |  unsentQueue  |
 * | --------------------------|
 * |    0~n    |      0~n      |
 * |---------------------------
 *
 * </pre>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:43
 * github - https://github.com/hl845740757
 */
public final class MessageQueue {

    public static final int INIT_SEQUENCE = 0;
    public static final int INIT_ACK = INIT_SEQUENCE + 1;

    /**
     * 消息号分配器
     */
    private long sequencer = INIT_SEQUENCE;

    /**
     * 期望的下一个消息号
     */
    private long ack = INIT_ACK;

    /**
     * 已发送待确认的消息队列，只要发送过就不会再放入{@link #unsentQueue}
     * Q: 为什么不使用arrayList?
     * A: 1.存在大量的删除操作 2.ArrayList存在空间浪费。3.遍历很少
     */
    private LinkedList<OuterSocketMessage> sentQueue = new LinkedList<>();

    /**
     * 未发送的消息队列,还没有尝试过发送的消息
     */
    private LinkedList<OuterSocketMessage> unsentQueue = new LinkedList<>();

    /**
     * 对方发送过来的ack是否有效。
     * (期望的下一个消息号是否合法)
     */
    public boolean isAckOK(long ack) {
        return ack >= getAckLowerBound() && ack <= getAckUpperBound();
    }

    /**
     * 获取上一个已确认的消息号
     */
    private long getAckLowerBound() {
        // 如果有消息未确认，那么ack的最小值就是未确认的第一个消息
        if (sentQueue.size() > 0) {
            return sentQueue.getFirst().getSequence();
        }
        // 都已确认，那么期望的消息号就是我的下一个消息
        return sequencer + 1;
    }

    /**
     * 获取下一个可能的最大ackGuid，
     */
    private long getAckUpperBound() {
        // 对方期望的最大消息号就是我的下一个消息
        return sequencer + 1;
    }

    /**
     * 根据对方发送的ack更新已发送队列
     *
     * @param ack 对方发来的ack
     */
    public void updateSentQueue(long ack) {
        while (sentQueue.size() > 0) {
            if (sentQueue.getFirst().getSequence() >= ack) {
                break;
            }
            sentQueue.removeFirst();
        }
    }

    public int getPendingMessages() {
        return sentQueue.size();
    }

    public int getCacheMessages() {
        return sentQueue.size();
    }

    /**
     * 生成ack信息
     *
     * @param ack 服务器发送来的ack
     */
    public String generateAckErrorInfo(long ack) {
        return String.format("{ack=%d, lastAckGuid=%d, nextMaxAckGuid=%d}", ack, getAckLowerBound(), getAckUpperBound());
    }

    /**
     * 分配下一个包的编号
     */
    public long nextSequence() {
        return ++sequencer;
    }

    public long getAck() {
        return ack;
    }

    public void setAck(long ack) {
        this.ack = ack;
    }

    public LinkedList<OuterSocketMessage> getSentQueue() {
        return sentQueue;
    }

    public LinkedList<OuterSocketMessage> getUnsentQueue() {
        return unsentQueue;
    }

    /**
     * 交换未发送的缓冲区
     */
    public LinkedList<OuterSocketMessage> exchangeUnsentMessages() {
        LinkedList<OuterSocketMessage> result = unsentQueue;
        unsentQueue = new LinkedList<>();
        return result;
    }

    /**
     * 删除已发送和未发送的消息队列
     * help gc
     */
    public void detachMessageQueue() {
        sentQueue = null;
        unsentQueue = null;
    }

    @Override
    public String toString() {
        return "MessageQueue{" +
                "sequencer=" + sequencer +
                ", ack=" + ack +
                ", sentQueueSize=" + sentQueue.size() +
                ", needSendQueueSize=" + unsentQueue.size() +
                "}";
    }
}
