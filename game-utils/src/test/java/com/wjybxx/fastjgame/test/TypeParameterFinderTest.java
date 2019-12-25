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

import com.wjybxx.fastjgame.reflect.TypeParameterFinder;

import java.util.Set;

/**
 * 泛型参数查找器简单示例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/24
 * github - https://github.com/hl845740757
 */
public class TypeParameterFinderTest {

    public static void main(String[] args) throws Exception {
        final Child instance = new Child("hello");
        System.out.println(TypeParameterFinder.findTypeParameter(instance, SuperInterface.class, "T"));
        System.out.println(TypeParameterFinder.findTypeParameter(instance, SuperInterface.class, "U"));

        System.out.println();
        System.out.println(TypeParameterFinder.findTypeParameter(instance, Parent.class, "E"));
    }

    private interface SuperInterface<T, U> {

        void onEvent(T context, U msg);
    }

    private static class Parent<E> {

        private final E element;

        Parent(E element) {
            this.element = element;
        }

        public E getElement() {
            return element;
        }
    }

    private static class Child extends Parent<String> implements SuperInterface<Set<String>, Integer> {

        Child(String element) {
            super(element);
        }

        @Override
        public void onEvent(Set<String> context, Integer msg) {

        }
    }
}
