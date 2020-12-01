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

import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

/**
 * Q: 它的意义？
 * A: 在执行部分逻辑时，可能在执行失败的情况下需要向客户端发送提示，该类作为应对这种情况的抽象。
 * <p>
 * Q: 为什么不再使用枚举？
 * A: 增加了额外的复杂度，但意义不大。注意：即使不使用枚举，仍然通过错误码类的常量使用错误码，每一个错误码是一个int常量。
 * 对于服务器，除非热更新需要新增错误码，否则一定不要在代码中直接使用数字。
 * 对于客户端，如果不能生成错误码类，请使用字符串id，一定不要使用数字id，代码里使用数字id极其混乱。
 * <p>
 * 错误码类最好是根据表格生成，需要一个简单的工具。表格设计：
 * <pre>
 *            |---------------|--------------|---------------|---------------|---------------|
 *  cs标记行   |     cs        |     cs       |       c       |               |      c        |
 *            |---------------|--------------|---------------|---------------|---------------|
 *  类型行     |    string     |    int32     |    string     |               |     int[]     |
 *            |---------------|--------------|---------------|---------------|---------------|
 *  命名行     |     name      |     code     |    content    |     desc      |   channels    |
 *            |---------------|--------------|---------------|---------------|---------------|
 *  描述行     |    字符串id   |     数字id    |      内容      |     desc      |   显示的频道   |
 *            |---------------|--------------|---------------|---------------|---------------|
 *  内容行     |  LEVEL_LIMIT  |      1       |    等级不足    |    通用提示    |      {1}      |
 *            |---------------|--------------|---------------|---------------|---------------|
 * </pre>
 * 注意：如果将提示内容放在语言表，那么建议name就是语言表的name。
 *
 * @author wjybxx
 * date - 2020/12/1
 * github - https://github.com/hl845740757
 */
public class ErrorResult {

    public static final int CODE_SUCCESS = 0;
    public static final ErrorResult SUCCESS = valueOf(CODE_SUCCESS);

    private final int errorCode;
    private final String[] params;

    private ErrorResult(int errorCode, String[] params) {
        this.errorCode = errorCode;
        this.params = params;
    }

    public static ErrorResult valueOf(int errorCode) {
        if (errorCode == CODE_SUCCESS) {
            return SUCCESS;
        }
        return new ErrorResult(errorCode, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    public static ErrorResult valueOf(int errorCode, String... params) {
        return new ErrorResult(errorCode, params);
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Nonnull
    public String[] getParams() {
        return params;
    }

    public boolean isSuccess() {
        return errorCode == CODE_SUCCESS;
    }

    public boolean isFailure() {
        return errorCode != CODE_SUCCESS;
    }
}
