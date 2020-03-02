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

package com.wjybxx.fastjgame.utils.example;

import com.wjybxx.fastjgame.utils.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.utils.eventbus.DefaultEventBus;
import com.wjybxx.fastjgame.utils.eventbus.EventBus;
import com.wjybxx.fastjgame.utils.eventbus.GenericEvent;
import com.wjybxx.fastjgame.utils.eventbus.Subscribe;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadFactory;

/**
 * {@link Subscribe}使用例子。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/24
 * github - https://github.com/hl845740757
 */
public class SubscriberExample {

    @Subscribe
    public void hello(String name) {
        System.out.println("hello-" + name);
    }

    @Subscribe
    public void onEvent(String name) {
        System.out.println("onEvent-" + name);
    }

    @Subscribe
    public void onEvent(Integer age) {
        System.out.println("onEvent-" + age);
    }

    @Subscribe(onlySubEvents = true, subEvents = {
            Integer.class,
            String.class
    })
    public void onEvent(Object event) {
        System.out.println("onEvent - Object " + event);
    }

    @Subscribe(subEvents = {
            DefaultThreadFactory.class,
            InternalThreadFactory.class
    })
    public void threadFactoryEvent(ThreadFactory threadFactory) {
        System.out.println("threadFactoryEvent - ThreadFactory " + threadFactory);
    }

    @Subscribe(subEvents = {
            LinkedHashSet.class,
    })
    public void hashSetEvent(HashSet<String> hashSet) {

    }

    @Subscribe
    public void genericEventString(TestGenericEvent<String> event) {
        System.out.println("genericEventString: " + event);
    }

    @Subscribe
    public void genericEventInt(TestGenericEvent<Integer> event) {
        System.out.println("genericEventInt: " + event);
    }

    @Subscribe(onlySubEvents = true, subEvents = {
            Integer.class,
            String.class
    })
    public void genericEventObject(TestGenericEvent<Object> event) {
        System.out.println("genericEventObject: " + event);
    }

    public static void main(String[] args) {
        final EventBus bus = new DefaultEventBus();
        SubscriberExampleBusRegister.register(bus, new SubscriberExample());
        SubscribeInnerExampleBusRegister.register(bus, new SubscribeInnerExample());

        bus.post(123456);
        System.out.println("-----------------------------------------");

        bus.post("123456");
        System.out.println("-----------------------------------------");

        bus.post(new DefaultThreadFactory("bus"));
        System.out.println("-----------------------------------------");

        bus.post(new InternalThreadFactory());
        System.out.println("-----------------------------------------");

        bus.post(new TestGenericEvent<>("hello"));
        System.out.println("-----------------------------------------");

        bus.post(new TestGenericEvent<>(123456));
    }

    public static class InternalThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            return null;
        }
    }

    /**
     * 内部类事件监听
     */
    public static class SubscribeInnerExample {

        @Subscribe
        public void onEvent(String event) {
            System.out.println("onEvent inner: " + event);
        }
    }

    public static class TestGenericEvent<T> implements GenericEvent<T> {

        private final T child;

        public TestGenericEvent(T child) {
            this.child = child;
        }

        @Nonnull
        @Override
        public T child() {
            return child;
        }

        @Override
        public String toString() {
            return "TestGenericEvent{" +
                    "child=" + child +
                    '}';
        }
    }
}
