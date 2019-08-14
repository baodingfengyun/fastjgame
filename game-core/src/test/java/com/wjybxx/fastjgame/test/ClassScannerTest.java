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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.utils.ClassScanner;

import java.util.Set;

import static com.wjybxx.fastjgame.utils.ClassScannerFilters.all;
import static com.wjybxx.fastjgame.utils.ClassScannerFilters.exceptInnerClass;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/20 14:41
 * github - https://github.com/hl845740757
 */
public class ClassScannerTest {

    public static void main(String[] args) {
        Set<Class<?>> allClass = ClassScanner.findClasses("com.wjybxx.fastjgame",
                exceptInnerClass(),
                all());

        allClass.forEach(e -> System.out.println(e.getSimpleName()));
    }
}
