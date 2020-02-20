/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 能实现为静态内部类的强烈建议使用静态内部类，避免对外开放，除非有开放需求，否则不要写在外部。
 * 以后新增自定义类型都使用{@link com.wjybxx.fastjgame.net.binary.EntitySerializer}实现编解码，不在新增codec类。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 * github - https://github.com/hl845740757
 */
package com.wjybxx.fastjgame.net.serializer;