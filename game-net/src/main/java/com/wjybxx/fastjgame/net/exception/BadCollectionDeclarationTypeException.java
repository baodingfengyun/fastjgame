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

package com.wjybxx.fastjgame.net.exception;

import com.wjybxx.fastjgame.db.exception.BadDeclarationTypeException;

/**
 * 出现这种情况时，如果对集合类型没有特殊需求，可以声明为{@link java.util.Collection} 或 {@link java.util.Map}。
 * 如果有特殊需求，请将集合放入一个实体对象中，并使用{@link com.wjybxx.fastjgame.db.annotation.Impl}注解提供安全的解析类型。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 */
public class BadCollectionDeclarationTypeException extends BadDeclarationTypeException {

    private static final String MSG = "If you new a special collection or map, you need to declare a entity class, " +
            "and provide enough information to your demand.";

    public BadCollectionDeclarationTypeException() {
        super(MSG);
    }
}
