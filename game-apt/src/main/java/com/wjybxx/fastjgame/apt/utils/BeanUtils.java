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

import com.squareup.javapoet.TypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaBean工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/8
 * github - https://github.com/hl845740757
 */
public class BeanUtils {

    public static final String INDEXABLE_ENTITY_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.entity.IndexableEntity";
    public static final String FOR_INDEX_METHOD_NAME = "forIndex";
    public static final String GET_INDEX_METHOD_NAME = "getIndex";

    public static final String NUMBER_ENUM_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.entity.NumericalEntity";
    public static final String FOR_NUMBER_METHOD_NAME = "forNumber";
    public static final String GET_NUMBER_METHOD_NAME = "getNumber";

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
     * 获取类的所有字段和方法，包含继承得到的字段和方法
     * {@link Elements#getAllMembers(TypeElement)}只包含父类的公共属性，不包含私有的东西。
     */
    public static List<Element> getAllFieldsAndMethodsWithInherit(TypeElement typeElement) {
        final List<TypeElement> flatInherit = AutoUtils.flatInheritAndReverse(typeElement);
        return flatInherit.stream()
                .flatMap(e -> e.getEnclosedElements().stream())
                .filter(e -> e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    /**
     * 是否包含非private的setter方法
     */
    public static boolean isContainerNotPrivateSetterMethod(Types typeUtils, VariableElement variableElement, List<? extends Element> allFieldsAndMethodWithInherit) {
        final String fieldName = variableElement.getSimpleName().toString();
        final String setterMethodName = BeanUtils.setterMethodName(fieldName, BeanUtils.isPrimitiveBoolean(variableElement.asType()));

        return allFieldsAndMethodWithInherit.stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE))
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getParameters().size() == 1)
                .filter(e -> typeUtils.isSameType(variableElement.asType(), e.getParameters().get(0).asType()))
                .anyMatch(e -> {
                    final String methodName = e.getSimpleName().toString();
                    return methodName.equals(setterMethodName);
                });
    }

    /**
     * 是否包含非private的getter方法
     */
    public static boolean isContainerNotPrivateGetterMethod(Types typeUtils, VariableElement variableElement, List<? extends Element> allFieldsAndMethodWithInherit) {
        final String fieldName = variableElement.getSimpleName().toString();
        final String getterMethodName = BeanUtils.getterMethodName(fieldName, BeanUtils.isPrimitiveBoolean(variableElement.asType()));
        return allFieldsAndMethodWithInherit.stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> !e.getModifiers().contains(Modifier.PRIVATE))
                .map(e -> (ExecutableElement) e)
                .filter(e -> typeUtils.isSameType(variableElement.asType(), e.getReturnType()))
                .anyMatch(e -> {
                    final String methodName = e.getSimpleName().toString();
                    return methodName.equals(getterMethodName);
                });
    }

    /**
     * 是否是基本类型的boolean
     */
    public static boolean isPrimitiveBoolean(TypeMirror typeMirror) {
        return typeMirror.getKind() == TypeKind.BOOLEAN;
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
     * @param filedName          字段名字
     * @param isPrimitiveBoolean 是否是bool值 - 坑太多了，只有基本类型的boolean才会变成is，包装类型的不会
     * @return 方法名
     */
    public static String getterMethodName(String filedName, boolean isPrimitiveBoolean) {
        if (isFirstOrSecondCharUpperCase(filedName)) {
            // 这里参数名一定不是is开头
            // 前两个字符任意一个大写，则参数名直接拼在get/is后面
            if (isPrimitiveBoolean) {
                return "is" + filedName;
            } else {
                return "get" + filedName;
            }
        }
        // 到这里前两个字符都是小写
        if (isPrimitiveBoolean) {
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
    public static String setterMethodName(String filedName, boolean isPrimitiveBoolean) {
        if (isFirstOrSecondCharUpperCase(filedName)) {
            // 这里参数名一定不是is开头
            // 前两个字符任意一个大写，则参数名直接拼在set后面
            return "set" + filedName;

        }
        // 到这里前两个字符都是小写 - is 还要特殊处理。
        if (isPrimitiveBoolean) {
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
     * 是否是boolean或Boolean类型
     *
     * @param typeName 类型描述名
     * @return 如果boolean类型或Boolean则返回true
     */
    public static boolean isPrimitiveBoolean(TypeName typeName) {
        return typeName == TypeName.BOOLEAN;
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

}
