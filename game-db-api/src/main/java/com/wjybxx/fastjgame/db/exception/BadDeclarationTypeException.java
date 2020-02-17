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

package com.wjybxx.fastjgame.db.exception;

/**
 * 抛出该异常时，表示无法根据字段/参数的声明类型无法确定它的解析类型。
 * 请创建一个实体类，并在类中给该字段给出足够的信息。
 * 更多信息请查看{@link com.wjybxx.fastjgame.db.annotation.Impl}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
public class BadDeclarationTypeException extends RuntimeException {

    public BadDeclarationTypeException(String message) {
        super(message);
    }
}
