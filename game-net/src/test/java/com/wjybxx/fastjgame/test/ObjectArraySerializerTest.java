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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.net.example.ExampleConstants;

import java.util.Arrays;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class ObjectArraySerializerTest {

    public static void main(String[] args) throws Exception {
        final Object[] objects = {"hello", "world", 1, 2, 3, Integer.class};
        System.out.println(Arrays.toString(objects));

        final Object[] cloneObjects = (Object[]) ExampleConstants.BINARY_SERIALIZER.cloneObject(objects);
        System.out.print(Arrays.toString(cloneObjects));

        System.out.print(Arrays.equals(objects, cloneObjects));
    }
}
