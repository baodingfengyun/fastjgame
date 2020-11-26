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

package com.wjybxx.fastjgame.util.misc;

import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用于监测每一步的耗时和总耗时。
 * 循环模式示例:
 * <pre>{@code
 *  public void tick() {
 *      final StepWatch stepWatch = new StepWatch("tick");
 *      while(true) {
 *          // 重启计时器
 *          stepWatch.resetAndStart();
 *
 *          doSomethingA();
 *          stepWatch.logStep("step1");
 *
 *          doSomethingB();
 *          stepWatch.logStep("step2");
 *
 *          doSomethingC();
 *          stepWatch.logStep("step3");
 *
 *          doSomethingD();
 *          stepWatch.logStep("step4");
 *
 *          // 可不调用stop
 *          stepWatch.stop();
 *          // 输出日志
 *          System.out.println(stepWatch.getLog());
 *      }
 *  }
 * }
 * </pre>
 * <p>
 * 非循环模式示例<pre>{@code
 *  public void execute() {
 *      final StepWatch stepWatch = new StepWatch("tick");
 *      stepWatch.start();
 *
 *      doSomethingA();
 *      stepWatch.logStep("step1");
 *
 *      doSomethingB();
 *      stepWatch.logStep("step2");
 *
 *      doSomethingC();
 *      stepWatch.logStep("step3");
 *
 *      doSomethingD();
 *      stepWatch.logStep("step4");
 *
 *      // 可不调用stop
 *      stepWatch.stop();
 *      // 输出日志
 *      System.out.println(stepWatch.getLog());
 *  }
 * }
 * </pre>
 *
 * @author wjybxx
 * date - 2020/11/26
 * github - https://github.com/hl845740757
 */
public class StepWatch {

    private final String name;
    private final StopWatch delegate = new StopWatch();
    private final List<Item> itemList = new ArrayList<>(8);
    private final StringBuilder sb = new StringBuilder(120);

    public StepWatch(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * 开始计时。
     * 重复调用start之前，必须调用{@link #reset()}
     */
    public void start() {
        delegate.start();
    }

    /**
     * 记录该步骤的耗时
     *
     * @param stepName 该步骤的名称
     */
    public void logStep(String stepName) {
        if (itemList.isEmpty()) {
            itemList.add(new Item(stepName, delegate.getTime()));
        } else {
            itemList.add(new Item(stepName, delegate.getTime() - delegate.getSplitTime()));
        }
        delegate.split();
    }

    /**
     * 如果希望停止计时，则调用该方法。
     */
    public void stop() {
        delegate.stop();
    }

    /**
     * 注意：为了安全起见，请要么在代码的开始重置，要么在finally块中重置。
     */
    public void reset() {
        delegate.reset();
        itemList.clear();
        sb.setLength(0);
    }

    /**
     * {@link #reset()}和{@link #start()}的快捷方法
     */
    public void restart() {
        reset();
        start();
    }

    /**
     * 获取start时的时间戳
     */
    public long getStartTime() {
        return delegate.getStartTime();
    }

    /**
     * 如果尚未stop，则返回从start到当前的已消耗的时间。
     * 如果已经stop，则返回从start到stop时消耗的时间。
     */
    public long getCostTime() {
        return delegate.getTime();
    }

    /**
     * 获取按照时间消耗排序后的log。
     * 注意：可以在不调用{@link #stop()}的情况下调用该方法。
     * (获得了一个规律，也失去了一个规律，可能并不如未排序的log看着舒服)
     */
    public String getSortedLog() {
        // 排序开销还算比较小
        itemList.sort(null);
        return getLog();
    }

    /**
     * 获取按照时间消耗排序后的log。
     * 注意：可以在不调用{@link #stop()}的情况下调用该方法。
     * <p>
     * 格式: [name={name}ms][a={a}ms,b={b}ms...]
     * 其中{@code {x}}表示x的耗时，前半部分为总耗时，后半部分为各步骤耗时。
     */
    public String getLog() {
        final StringBuilder sb = this.sb;
        final List<Item> itemList = this.itemList;
        // 总耗时
        sb.append('[').append(name).append('=').append(delegate.getTime()).append("ms]");
        // 每个步骤耗时
        sb.append('[');
        for (int i = 0; i < itemList.size(); i++) {
            final Item item = itemList.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(item.stepName).append('=').append(item.costTimeMs).append("ms");
        }
        sb.append(']');
        return sb.toString();
    }

    private static class Item implements Comparable<Item> {

        final String stepName;
        final long costTimeMs;

        Item(String stepName, long costTimeMs) {
            this.stepName = stepName;
            this.costTimeMs = costTimeMs;
        }

        @Override
        public int compareTo(Item that) {
            final int timeCompareResult = Long.compare(costTimeMs, that.costTimeMs);
            if (timeCompareResult != 0) {
                // 时间逆序
                return -1 * timeCompareResult;
            } else {
                // 字母自然序
                return stepName.compareTo(that.stepName);
            }
        }
    }

}
