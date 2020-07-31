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

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;
import com.wjybxx.fastjgame.util.dsl.IndexableEnum;

import java.util.Objects;

/**
 * 范围
 * <p>
 * 为避免重复代码，这里成员字段声明为long类型，但考虑到可能有大量的强制类型转换，提供了int返回值的快捷方法：
 * {@link #getIntBegin()} {@link #getIntEnd()} {@link #intLength()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/12
 */
public class Range {

    public final long begin;
    public final long end;
    public final Mode mode;

    public Range(long begin, long end) {
        this(begin, end, Mode.DEFAULT);
    }

    public Range(long begin, long end, Mode mode) {
        validateRange(begin, end);
        Objects.requireNonNull(mode, "mode");

        this.begin = begin;
        this.end = end;
        this.mode = mode;
    }

    private static void validateRange(long begin, long end) {
        if (begin > end) {
            throw new IllegalArgumentException("Bad Range: " + "[" + begin + "," + end + "]");
        }
    }

    public long getBegin() {
        return begin;
    }

    public long getEnd() {
        return end;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * 计算范围的长度。
     * 注意：用户自己需要清楚能否调用该方法。
     *
     * @throws ArithmeticException 如果计算结果超过long的表述范围，则抛出该异常。
     */
    public long length() {
        return mode.length(begin, end);
    }

    public boolean isEmpty() {
        return mode.isEmpty(begin, end);
    }

    /**
     * 是否在区间段内
     *
     * @param value 待检测的值
     */
    public boolean withinRange(long value) {
        return mode.withinRange(begin, end, value);
    }

    // =================================  int的转换   =====================================

    public int getIntBegin() {
        // 不校验，因为使用该方法的时候，客户端构造对象时传入的应该是int
        return (int) begin;
    }

    public int getIntEnd() {
        // 不校验，因为使用该方法的时候，客户端构造对象时传入的应该是int
        return (int) end;
    }

    public int intLength() {
        // 需要校验，因为即使都是int，其差值length也可能溢出
        return Math.toIntExact(length());
    }

    @Override
    public String toString() {
        return "Range[" + begin + "," + end + "," + mode + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Range that = (Range) o;
        return begin == that.begin
                && end == that.end
                && mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(begin);
        result = 31 * result + Long.hashCode(end);
        result = 31 * result + mode.hashCode();
        return result;
    }

    // =================================  开放工具方法   =====================================

    /**
     * 是否在区间段内
     *
     * @param begin 起始值
     * @param end   结束值
     * @param value 测试值
     * @param mode  模式
     */
    public static boolean withinRange(long begin, long end, long value, Mode mode) {
        validateRange(begin, end);
        return mode.withinRange(begin, end, value);
    }

    public enum Mode implements IndexableEnum {

        /**
         * 左闭右开
         */
        LEFT_CLOSED_RIGHT_OPEN(0) {
            @Override
            public long lengthImpl(long begin, long end) {
                return end - begin;
            }

            @Override
            public boolean withinRange(long begin, long end, long value) {
                return value >= begin && value < end;
            }
        },

        /**
         * 左开右闭
         */
        LEFT_OPEN_RIGHT_CLOSED(1) {
            @Override
            public long lengthImpl(long begin, long end) {
                return end - begin;
            }

            @Override
            public boolean withinRange(long begin, long end, long value) {
                return value > begin && value <= end;
            }
        },

        /**
         * 闭区间
         */
        CLOSED(2) {
            @Override
            public long lengthImpl(long begin, long end) {
                return end - begin + 1;
            }

            @Override
            public boolean withinRange(long begin, long end, long value) {
                return value >= begin && value <= end;
            }
        },

        /**
         * 开区间
         */
        OPEN(3) {
            @Override
            public long lengthImpl(long begin, long end) {
                if (end == begin) {
                    return 0;
                }
                return end - begin - 1;
            }

            @Override
            public boolean withinRange(long begin, long end, long value) {
                return value > begin && value < end;
            }
        };

        public final int number;

        /**
         * 默认区间：左闭右开
         */
        public static final Mode DEFAULT = Mode.LEFT_CLOSED_RIGHT_OPEN;

        Mode(int number) {
            this.number = number;
        }

        @Override
        public int getNumber() {
            return number;
        }

        public static Mode forNumber(int number) {
            return values()[number];
        }

        final boolean isEmpty(long begin, long end) {
            return length(begin, end) == 0;
        }

        final long length(long begin, long end) {
            final long result = lengthImpl(begin, end);
            // 溢出检查，long值相减可能溢出，这里并不能检测到所有情况
            if (result < 0) {
                throw new ArithmeticException("long overflow");
            }
            return result;
        }

        abstract long lengthImpl(long begin, long end);

        abstract boolean withinRange(long begin, long end, long value);
    }

    @SuppressWarnings("unused")
    private static class RangeCodec implements PojoCodecImpl<Range> {

        @Override
        public Class<Range> getEncoderClass() {
            return Range.class;
        }

        @Override
        public Range readObject(ObjectReader reader) throws Exception {
            final long start = reader.readLong();
            final long end = reader.readLong();
            final Mode mode = Mode.forNumber(reader.readInt());
            return new Range(start, end, mode);
        }

        @Override
        public void writeObject(Range instance, ObjectWriter writer) throws Exception {
            writer.writeLong(instance.begin);
            writer.writeLong(instance.end);
            writer.writeInt(instance.mode.number);
        }
    }
}