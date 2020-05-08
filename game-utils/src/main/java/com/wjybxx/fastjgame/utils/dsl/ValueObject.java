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

package com.wjybxx.fastjgame.utils.dsl;

import javax.annotation.concurrent.Immutable;

/**
 * 值对象，与DDD中的值对象保持一致。
 * <p>
 * 值对象一般没有唯一标识，它根据对象的属性和状态判断相等性。
 * 有时候也会有唯一标识，但并不代表它是实体，一般值对象的唯一标识用于去重等特殊用途。
 * <p>
 * 该接口仅仅是一个标记接口，并不强制所有的值对象都实现该接口，但是实现该接口可以更加明确。
 * 不论是否实现该接口，一个值对象实现都应该满足值对象的要求，如：不可变。
 * <p>
 * Q: 枚举是值对象吗？
 * A: 是。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/4/19
 */
@Immutable
public interface ValueObject {

}
