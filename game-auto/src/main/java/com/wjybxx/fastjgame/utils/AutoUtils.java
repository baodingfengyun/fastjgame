/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.utils;

import com.squareup.javapoet.*;

import javax.annotation.Nonnull;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


/**
 * 代码生成工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
public class AutoUtils {

    private AutoUtils() {

    }


    // ------------------------------------------ 类顺序 ------------------------------------------

    /**
     * 判断一个字段是否是boolean
     *
     * @param field 字段
     * @return 如果是boolean类型或Boolean类型，则返回true
     */
    public static boolean isBoolean(Field field) {
        Class<?> clazz = field.getType();
        return clazz == boolean.class || clazz == Boolean.class;
    }


    public static String getterMethodName(Field field) {
        return getterMethodName(field.getName(), isBoolean(field));
    }

    public static String setterMethodName(Field field) {
        return setterMethodName(field.getName());
    }

    // ------------------------------------------ 编译属性 ------------------------------------------

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

    public static String getterMethodName(FieldSpec field) {
        return getterMethodName(field.name, isBoolean(field.type));
    }

    public static String setterMethodName(FieldSpec field) {
        return setterMethodName(field.name);
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
        final String methodName = setterMethodName(field.name);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return this.$L", field.name)
                .build();
    }

    // -------------------------------------------- common -------------------------------------

    /**
     * 字符串首字符大写
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
    private static String getterMethodName(String filedName, boolean isBoolean) {
        if (isBoolean) {
            // bool类型，如果使用is开头，那么会产生些奇怪的问题
            return filedName.startsWith("is") ? filedName : "is" + firstCharToUpperCase(filedName);
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
    private static String setterMethodName(String filedName) {
        return "set" + firstCharToUpperCase(filedName);
    }

    // ------------------------------------------ 分割线 ------------------------------------------------

    /**
     * 复制一个方法，当然不包括代码块。
     *
     * @param method 方法信息
     * @return builder
     */
    public static MethodSpec.Builder copyMethod(@Nonnull ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);

        // 访问修饰符
        copyModifiers(builder, method);
        // 返回值类型
        copyReturnType(builder, method);
        // 泛型变量
        copyTypeVariableNames(builder, method);
        // 方法参数
        copyParameters(builder, method);
        // 异常信息
        copyExceptionsTable(builder, method);
        // 是否是变长参数类型
        builder.varargs(method.isVarArgs());

        return builder;
    }

    /**
     * 拷贝返回值类型
     */
    public static void copyReturnType(MethodSpec.Builder builder, @Nonnull ExecutableElement method) {
        builder.returns(TypeName.get(method.getReturnType()));
    }

    /**
     * 拷贝一个方法的修饰符
     */
    public static void copyModifiers(MethodSpec.Builder builder, @Nonnull ExecutableElement method) {
        builder.addModifiers(method.getModifiers());
    }

    /**
     * 拷贝一个方法的所有泛型参数
     */
    public static void copyTypeVariableNames(MethodSpec.Builder builder, ExecutableElement method) {
        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            builder.addTypeVariable(TypeVariableName.get(var));
        }
    }

    /**
     * 拷贝一个方法的所有参数
     */
    public static void copyParameters(MethodSpec.Builder builder, ExecutableElement method) {
        copyParameters(builder, method.getParameters());
    }

    /**
     * 拷贝这些方法参数
     */
    public static void copyParameters(MethodSpec.Builder builder, List<? extends VariableElement> parameters) {
        for (VariableElement parameter : parameters) {
            builder.addParameter(ParameterSpec.get(parameter));
        }
    }

    /**
     * 拷贝一个方法的异常表
     */
    public static void copyExceptionsTable(MethodSpec.Builder builder, ExecutableElement method) {
        for (TypeMirror thrownType : method.getThrownTypes()) {
            builder.addException(TypeName.get(thrownType));
        }
        ;
    }

    // ----------------------------------------------------- 分割线 -----------------------------------------------

    /**
     * 查找出现的第一个注解，不包含继承的部分
     *
     * @param typeUtils  类型工具
     * @param element    要查询的element
     * @param targetType 目标注解类型
     * @return optional
     */
    public static Optional<? extends AnnotationMirror> findFirstAnnotationNotInheritance(Types typeUtils, Element element, DeclaredType targetType) {
        // 查找该字段上的注解
        return element.getAnnotationMirrors().stream()
                .filter(annotationMirror -> typeUtils.isSameType(annotationMirror.getAnnotationType(), targetType))
                .findFirst();
    }

    /**
     * 查找出现的第一个注解，包含继承的注解
     *
     * @param typeUtils    类型工具
     * @param elementUtils 元素工具
     * @param element      要查询的element
     * @param targetType   目标注解类型
     * @return optional
     */
    public static Optional<? extends AnnotationMirror> findFirstAnnotation(Types typeUtils, Elements elementUtils, Element element, DeclaredType targetType) {
        // 查找该字段上的注解
        return elementUtils.getAllAnnotationMirrors(element).stream()
                .filter(annotationMirror -> typeUtils.isSameType(annotationMirror.getAnnotationType(), targetType))
                .findFirst();
    }

    /**
     * 获取注解上的某一个属性的值，不包含default值
     *
     * @param annotationMirror 注解编译信息
     * @param propertyName     属性的名字
     * @return object
     */
    public static Object getAnnotationValueNotDefault(AnnotationMirror annotationMirror, String propertyName) {
        final Optional<Object> property = annotationMirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(entry -> entry.getValue().getValue())
                .findFirst();
        assert property.isPresent();
        return property.get();
    }

    /**
     * 获取注解上的某一个属性的值，包含default值
     *
     * @param annotationMirror 注解编译信息
     * @param propertyName     属性的名字
     * @return object
     */
    public static Object getAnnotationValue(Elements elementUtils, AnnotationMirror annotationMirror, String propertyName) {
        final Optional<Object> property = elementUtils.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(entry -> entry.getValue().getValue())
                .findFirst();
        assert property.isPresent();
        return property.get();
    }

    /**
     * 检查变量的声明类型是否是指定类型
     * 对于一个Type：
     * 1. 如果其声明类型是具体类型，eg: {@code String}， 那么会走到{@code visitDeclared}。
     * 2. 如果其声明类型是泛型类型，eg: {@code E}， 那么会走到{@code visitTypeVariable}
     * <p>
     * {@link SimpleTypeVisitor8#visitDeclared(DeclaredType, Object)} 访问类型的声明类型
     * {@link SimpleTypeVisitor8#visitTypeVariable(TypeVariable, Object)} 访问类型的泛型类型
     *
     * @param variableElement 变量
     * @param matcher         变量的声明类型匹配器
     * @return 满足条件则返回true
     */
    public static boolean isTargetDeclaredType(VariableElement variableElement, Predicate<DeclaredType> matcher) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>() {

            @Override
            public Boolean visitDeclared(DeclaredType t, Void aVoid) {
                // 访问声明的类型 eg: String str
                return matcher.test(t);
            }

            @Override
            public Boolean visitTypeVariable(TypeVariable t, Void aVoid) {
                // 泛型变量 eg: E element
                return false;
            }

            @Override
            protected Boolean defaultAction(TypeMirror e, Void aVoid) {
                return false;
            }

        }, null);
    }

}
