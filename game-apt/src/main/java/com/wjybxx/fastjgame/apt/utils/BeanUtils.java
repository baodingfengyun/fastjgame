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

package com.wjybxx.fastjgame.apt.utils;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * JavaBean工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/8
 * github - https://github.com/hl845740757
 */
public class BeanUtils {

    /**
     * 判断一个类是否包含无参构造方法
     */
    public static boolean containsNoArgsConstructor(TypeElement typeElement) {
        return getNoArgsConstructor(typeElement) != null;
    }

    /**
     * 查找无参构造方法
     */
    @Nullable
    public static ExecutableElement getNoArgsConstructor(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getParameters().size() == 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * 首字符大写
     *
     * @param str content
     * @return 首字符大写的字符串
     */
    public static String firstCharToUpperCase(@Nonnull String str) {
        if (str.length() > 1) {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        } else {
            return str.toUpperCase();
        }
    }

    /**
     * 首字母小写
     *
     * @param str content
     * @return 首字符小写的字符串
     */
    public static String firstCharToLowerCase(@Nonnull String str) {
        if (str.length() > 1) {
            return str.substring(0, 1).toLowerCase() + str.substring(1);
        } else {
            return str.toLowerCase();
        }
    }

    /**
     * 获取getter方法的名字
     *
     * @param filedName 字段名字
     * @param isBoolean 是否是bool值
     * @return 方法名
     */
    public static String getterMethodName(String filedName, boolean isBoolean) {
        if (isFirstOrSecondCharUpperCase(filedName)) {
            // 这里参数名一定不是is开头
            // 前两个字符任意一个大写，则参数名直接拼在get/is后面
            if (isBoolean) {
                return "is" + filedName;
            } else {
                return "get" + filedName;
            }
        }
        // 到这里前两个字符都是小写
        if (isBoolean) {
            // 如果参数名以 is 开头，则直接返回，否则 is + 首字母大写 - is 还要特殊处理
            if (filedName.length() > 2 && filedName.startsWith("is")) {
                return filedName;
            } else {
                return "is" + firstCharToUpperCase(filedName);
            }
        } else {
            return "get" + firstCharToUpperCase(filedName);
        }
    }

    /**
     * 获取setter方法的名字
     *
     * @param filedName 字段名字
     * @return 方法名
     */
    public static String setterMethodName(String filedName, boolean isBoolean) {
        if (isFirstOrSecondCharUpperCase(filedName)) {
            // 这里参数名一定不是is开头
            // 前两个字符任意一个大写，则参数名直接拼在set后面
            return "set" + filedName;

        }
        // 到这里前两个字符都是小写 - is 还要特殊处理。
        if (isBoolean) {
            if (filedName.length() > 2 && filedName.startsWith("is")) {
                return "set" + firstCharToUpperCase(filedName.substring(2));
            } else {
                return "set" + firstCharToUpperCase(filedName);
            }
        } else {
            return "set" + firstCharToUpperCase(filedName);
        }
    }

    /**
     * 查询名字的第一个或第二个字符是否是大写
     */
    private static boolean isFirstOrSecondCharUpperCase(String name) {
        if (name.length() > 0 && Character.isUpperCase(name.charAt(0))) {
            return true;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return true;
        }
        return false;
    }

    /**
     * 是否是boolean或Boolean类型
     *
     * @param typeName 类型描述名
     * @return 如果boolean类型或Boolean则返回true
     */
    public static boolean isBoolean(TypeName typeName) {
        if (typeName == TypeName.BOOLEAN) {
            return true;
        }
        if (typeName.isBoxedPrimitive() && typeName.unbox() == TypeName.BOOLEAN) {
            return true;
        }
        return false;
    }

    /**
     * 创建属性的getter方法
     *
     * @param field 字段描述符
     * @return getter方法
     */
    public static MethodSpec createGetter(FieldSpec field) {
        final String methodName = getterMethodName(field);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return this.$L", field.name)
                .build();
    }

    /**
     * 创建属性的setter方法
     *
     * @param field 字段描述符
     * @return setter方法
     */
    public static MethodSpec createSetter(FieldSpec field) {
        final String methodName = setterMethodName(field);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return this.$L", field.name)
                .build();
    }

    /**
     * 获取一个字段的getter方法名字
     */
    public static String getterMethodName(FieldSpec field) {
        return getterMethodName(field.name, isBoolean(field.type));
    }

    /**
     * 获取一个字段的setter方法名字
     */
    public static String setterMethodName(FieldSpec field) {
        return setterMethodName(field.name, isBoolean(field.type));
    }
}
