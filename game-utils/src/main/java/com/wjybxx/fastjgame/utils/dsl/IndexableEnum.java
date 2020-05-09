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


import javax.annotation.Nonnull;

/**
 * 可索引的枚举，要求项目中的所有可序列化的枚举必须实现该接口。
 *
 * <h>稳定性</h>
 * 相对于{@link Enum#ordinal()}和{@link Enum#name()}，我们自定义的{@link #getNumber()}会更加稳定。<br>
 * 因此，在序列化和持久化时，都使用{@link #getNumber()}，因此我们需要尽可能的保持{@link #getNumber()}的稳定性。<br>
 * 注意：如果一个{@link IndexableEnum}是可序列化的，必须提供非私有的静态的{@code forNumber}方法。<br>
 * <pre>{@code
 *      static T forNumber(int number) {
 *          ...
 *      }
 * }
 * </pre>
 *
 * <p>
 * Q: 名字的由来？<br>
 * A: 如果一个对象是可索引的，且其索引值是整形，我们可以认为它就是一个枚举。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 13:35
 * github - https://github.com/hl845740757
 */
public interface IndexableEnum extends IndexableObject<Integer> {

    int getNumber();

    /**
     * @deprecated use {@link #getNumber()} instead
     */
    @Deprecated
    @Nonnull
    @Override
    default Integer getIndex() {
        return getNumber();
    }
}
