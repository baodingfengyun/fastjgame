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

package com.wjybxx.fastjgame.agent;

import com.sun.tools.attach.VirtualMachine;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 * Instrumentation开发指南 - https://www.ibm.com/developerworks/cn/java/j-lo-jse61/index.html
 * 类热更新代理。
 * 1. 由于代理必须以jar包形式存在，因此文件检测，执行更新等逻辑，请写在自己的业务逻辑包中，不要写在这里，方便扩展。
 * 2. 务必使用JDK11，JDK9之前{@link Instrumentation#redefineClasses(ClassDefinition...)}存在bug，有崩溃风险。
 * 3. 热更新时不要一次更新太多类，否则可能导致停顿时间过长。
 * <p>
 * debug下使用{@link #agentmain(String, Instrumentation)}比较方便，直接在ide中添加启动参数就可以。
 * 线上使用{@link #premain(String, Instrumentation)}方式比较方便。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/11
 * github - https://github.com/hl845740757
 */
public class ClassReloadAgent {

    private static volatile Instrumentation instrumentation;

    private static void setInstrumentation(Instrumentation instrumentation) {
        ClassReloadAgent.instrumentation = Objects.requireNonNull(instrumentation);
    }

    /**
     * 这是instrument开发规范规定的固定格式的方法，当java程序启动时，如果指定了javaagent参数(classpath下的jar包名字)，则会自动调用到这个方法。
     * 注意：
     * 1. 需要在启动参数中指定 javaagent参数。eg:
     * -javaagent:game-classreloadagent-1.0.jar=test
     * 则agentArgs为test(若无等号则为null)
     * 2. 不能方便的调试，必须在命令行中启动。
     *
     * @param agentArgs       启动参数
     * @param instrumentation 我们需要的实例，需要将其保存下来
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("premain invoked, agentArgs: " + agentArgs);
        ClassReloadAgent.setInstrumentation(instrumentation);
    }

    /**
     * 使用动态attach的方式获取{@link Instrumentation}。
     * 这是instrument开发规范规定的固定格式的方法，当使用{@link VirtualMachine#loadAgent(String, String)}连接到JVM时，会触发该方法。
     * <p>
     * 注意
     * 1. 如果要attach到自身所在JVM，需要添加启动参数 -Djdk.attach.allowAttachSelf=true 否则会抛出异常。
     * 2. 它的参数来自{@link VirtualMachine#loadAgent(String, String)}的第二个参数(options)
     * 3. 可直接在debug环境下使用（在debug参数中指定vm options即可）。
     *
     * @param agentArgs       {@link VirtualMachine#loadAgent(String, String)}中的options
     * @param instrumentation 我们需要的实例，需要将其保存下来
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("agentmain invoked, agentArgs: " + agentArgs);
        ClassReloadAgent.setInstrumentation(instrumentation);
    }

    /**
     * 查询是否启用了重定义类功能。
     * 注意：该返回值与{@link #isModifiableClass(Class)}返回值没有关系。
     */
    public static boolean isRedefineClassesSupported() {
        return instrumentation.isRedefineClassesSupported();
    }

    /**
     * 查询一个类是否可以被修改（被重定义），是否可以用于{@link #redefineClasses(ClassDefinition...)}方法。
     * 注意：该返回值与{@link #isRedefineClassesSupported()}的返回值没有关系。
     * 所以：要热更新类时需要保证{@link #isRedefineClassesSupported()}为true，且要更新的类调用当前方法返回值为true。
     * 基本上：你在ide中能热更新，那么这里就能热更新，所以最好现在ide上测试一下。
     *
     * @param theClass 要热更新的类
     * @return true/false
     */
    public static boolean isModifiableClass(Class<?> theClass) {
        return instrumentation.isModifiableClass(theClass);
    }

    /**
     * 重定义类文件（热更新类文件）
     * 注意：请保证{@link #isRedefineClassesSupported()}为true，且所有要更新的类{@link #isModifiableClass(Class)}为true。
     *
     * @param definitions 要热更新的类（要重定义的类）
     *                    注意：该方法的参数是数组，如果类之间有关系的话，最好一起更新（原子方式更新）。
     */
    public static void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
        instrumentation.redefineClasses(definitions);
    }

    /**
     * 添加指定jar包到<b>根类加载器</b>路径中
     *
     * @param jarfile 要加载的jar包
     */
    public static void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
    }

    /**
     * 添加指定jar包到<b>系统类加载器</b>路径中
     *
     * @param jarfile 要加载的jar包
     */
    public static void appendToSystemClassLoaderSearch(JarFile jarfile) {
        instrumentation.appendToSystemClassLoaderSearch(jarfile);
    }
}
