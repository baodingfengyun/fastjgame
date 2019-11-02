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

package com.wjybxx.fastjgame.misc;

/**
 * 数据库类型，避免直接使用字符串;
 * 数据库名字建议小写；
 * <p>
 * 如果每个服数据库是独立的，使用枚举更加简单；
 * 如果数据库全局唯一的，那么数据库的名字需要有区分度。
 * (之前为 MongoDataBaseName，由于某些原因进行了修改)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/21 17:50
 * github - https://github.com/hl845740757
 */
public enum MongoDBType {

    /**
     * 中心服数据库(单服数据库)
     */
    CENTER,

    /**
     * 战区数据库
     */
    WARZONE,

    /**
     * 全局数据库(账号数据库)
     */
    GLOBAL;
}
