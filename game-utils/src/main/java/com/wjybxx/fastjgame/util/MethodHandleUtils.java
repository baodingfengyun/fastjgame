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

package com.wjybxx.fastjgame.util;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/8/11
 */
public class MethodHandleUtils {

    private static final MethodType SUPPLIER_INVOKE_TYPE = MethodType.methodType(Supplier.class);
    private static final MethodType SUPPLIER_GET_METHOD_TYPE = MethodType.methodType(Object.class);

    private MethodHandleUtils() {
        throw new AssertionError();
    }


    /**
     * 将无参构造方法转换为{@link Supplier}的方法引用
     *
     * @param lookup            {@link MethodHandles#lookup()}
     * @param noArgsConstructor 无参构造方法
     * @param returnType        构造方法返回的对象类型，因为在查找构造方法的{@link MethodHandle}的时候，返回值类型是{@code void.class}，
     *                          因为无法从{@code noArgsConstructor}获得返回类型
     * @return Supplier
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<? extends T> noArgsConstructorToSupplier(MethodHandles.Lookup lookup, MethodHandle noArgsConstructor, Class<T> returnType) throws Throwable {
        if (noArgsConstructor.type().parameterCount() != 0) {
            throw new IllegalArgumentException("supplier methodHandle parameterCount expected 0");
        }
        return (Supplier<? extends T>) toMethodReference(lookup,
                "get", SUPPLIER_INVOKE_TYPE, SUPPLIER_GET_METHOD_TYPE,
                noArgsConstructor, MethodType.methodType(returnType));
    }


    /**
     * 将一个{@link MethodHandle}转换为lambda的方法引用。
     * <p>
     * Q: 与{@code ()-> methodHandle.invoke()}的区别？
     * A: 以上代码其实多了一层lambda，实际测试影响还挺大。
     * <pre>
     * {@code
     * public class LambdaTest {
     *     public static void main(String[] args) {
     *         Runnable r = () -> System.out.println(Arrays.toString(args));
     *         r.run();
     *     }
     * }
     * }
     * invokeName: "run"
     * invokeType: MethodType.methodType(Runnable.class, String[].class)
     * samMethodType: MethodType.methodType(void.class)
     * implMethod: caller.findStatic(LambdaTest.class,"lambda$main$0", methodType(void.class))
     * instantiatedMethodType: MethodType.methodType(void.class)
     *
     * @param invokedName            要实现的方法的名字
     * @param invokedType            调用点的签名（返回类型是要实现的函数式接口类型，参数类型表示捕获变量的类型）
     * @param samMethodType          single abstract method，函数式接口定义的方法的签名(返回类型+参数)
     * @param implMethod             一个直接的方法句柄，描述了在调用时应调用的实现方法（对参数类型，返回类型进行适当的调整，并在调用参数之前添加捕获的参数）。
     *                               通常我们使用的是方法的{@link MethodHandle}，而不是lambda生成的{@link MethodHandle}
     * @param instantiatedMethodType 函数式接口实例化对象的方法签名信息，一般和samMethodType相同，如果函数式接口中方法是泛型的，则可能不一样。
     * @return 实现的函数式接口对象
     */
    private static Object toMethodReference(MethodHandles.Lookup caller,
                                            String invokedName,
                                            MethodType invokedType,
                                            MethodType samMethodType,
                                            MethodHandle implMethod,
                                            MethodType instantiatedMethodType) throws Throwable {
        // 注意：invoke会适配参数和返回值类型，而invokeExact不会适配参数和返回值类型，可能导致调用失败
        return LambdaMetafactory.metafactory(caller,
                invokedName, invokedType, samMethodType,
                implMethod, instantiatedMethodType)
                .getTarget()
                .invoke();
    }

}
