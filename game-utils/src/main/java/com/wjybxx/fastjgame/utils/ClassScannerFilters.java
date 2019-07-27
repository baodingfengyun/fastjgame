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

package com.wjybxx.fastjgame.utils;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 使用时建议静态导入该类中的所有方法。
 * {@code
 *      import static com.wjybxx.fastjgame.utils.ClassScannerFilters.*;
 * }
 * 了解以下方法会很有帮助。
 * {@link Predicate#and(Predicate)}
 * {@link Predicate#or(Predicate)}
 * {@link Predicate#negate()}
 *
 * {@code
 *      annotationWith(Deprecated.class).and(childClass(Object.class));
 * }
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/20 14:33
 * github - https://github.com/hl845740757
 */
public class ClassScannerFilters {

    private ClassScannerFilters() {

    }

    /**
     * 所有的类文件都有效，不论判断名字还是class对象
     * @return
     */
    public static <T> Predicate<T> all(){
        return obj -> true;
    }

    // ------------------------------------针对class对象的过滤器--------------------------
    /**
     * 被注解的注释的类
     * (一定要注意注解的生命周期！由于是扫描class，因此只对{@link RetentionPolicy#RUNTIME})有效。
     *
     * 细看{@link Retention} {@link RetentionPolicy}
     * @param annotationClass 注解
     * @return
     */
    public static Predicate<Class<?>> annotationWith(Class<? extends Annotation> annotationClass){
        return clazz -> clazz.isAnnotationPresent(annotationClass);
    }

    /**
     * 是某个类的子类
     * @param superClass 超类
     * @return
     */
    public static Predicate<Class<?>> childClass(Class<?> superClass){
        return superClass::isAssignableFrom;
    }

    // -------------------------------针对className的过滤器-----------------------------
    /**
     * 除了内部类以外
     * @return
     */
    public static Predicate<String> exceptInnerClass(){
        // 内部类含有 "$"
        return clazzName -> !clazzName.contains("$");
    }

    /**
     * 除了指定包路径以外的类
     * @param pkgNameArray 包名 com.wjybxx.fastjgame
     * @return
     */
    public static Predicate<String> exceptPkgs(@Nonnull String... pkgNameArray) {
        return className -> Arrays.stream(pkgNameArray).anyMatch(pkgName -> className.startsWith(pkgName + "."));
    }

    /**
     * 除了满足表达式的类
     * @param regex 正则表达式
     * @return
     */
    public static Predicate<String> exceptRegex(String regex) {
        return className -> !className.matches(regex);
    }
}
