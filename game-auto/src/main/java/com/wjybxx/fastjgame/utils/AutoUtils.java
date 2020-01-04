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

import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
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

    public static final SourceVersion SOURCE_VERSION = SourceVersion.RELEASE_11;

    public static final AnnotationSpec SUPPRESS_UNCHECKED_ANNOTATION = AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "$S", "unchecked, rawtypes")
            .build();

    private AutoUtils() {

    }

    /**
     * @param processorType 注解处理器
     * @return 代码生成信息注解
     */
    public static AnnotationSpec newProcessorInfoAnnotation(Class<? extends AbstractProcessor> processorType) {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", processorType.getCanonicalName())
                .build();
    }

    /**
     * 复制一个方法信息，当然不包括代码块。
     *
     * @param method 方法信息
     * @return builder
     */
    public static MethodSpec.Builder copyMethod(@Nonnull ExecutableElement method) {
        final String methodName = method.getSimpleName().toString();
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);

        // 访问修饰符
        copyModifiers(builder, method);
        // 泛型变量
        copyTypeVariables(builder, method);
        // 返回值类型
        copyReturnType(builder, method);
        // 方法参数
        copyParameters(builder, method);
        // 异常信息
        copyExceptionsTable(builder, method);
        // 是否是变长参数类型
        builder.varargs(method.isVarArgs());

        return builder;
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
    public static void copyTypeVariables(MethodSpec.Builder builder, ExecutableElement method) {
        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            builder.addTypeVariable(TypeVariableName.get(var));
        }
    }

    /**
     * 拷贝返回值类型
     */
    public static void copyReturnType(MethodSpec.Builder builder, @Nonnull ExecutableElement method) {
        builder.returns(TypeName.get(method.getReturnType()));
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
    public static Optional<? extends AnnotationMirror> findFirstAnnotationWithoutInheritance(Types typeUtils, Element element, DeclaredType targetType) {
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
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T getAnnotationValueNotDefault(AnnotationMirror annotationMirror, String propertyName) {
        final Optional<Object> property = annotationMirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(entry -> entry.getValue().getValue())
                .findFirst();
        return (T) property.orElse(null);
    }

    /**
     * 获取注解上的某一个属性的值，包含default值
     *
     * @param annotationMirror 注解编译信息
     * @param propertyName     属性的名字
     * @return object
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T> T getAnnotationValue(Elements elementUtils, AnnotationMirror annotationMirror, String propertyName) {
        final Optional<Object> property = elementUtils.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(entry -> entry.getValue().getValue())
                .findFirst();
        assert property.isPresent();
        return (T) property.get();
    }

    /**
     * 检查变量的声明类型是否是指定类型
     * 对于一个Type：
     * 1. 如果其声明类型是具体类型，eg: {@code String}， 那么会走到{@link SimpleTypeVisitor8#visitDeclared(DeclaredType, Object)}。
     * 2. 如果其声明类型是泛型类型，eg: {@code E}， 那么会走到{@link SimpleTypeVisitor8#visitTypeVariable(TypeVariable, Object)}。
     *
     * @param variableElement 变量
     * @param matcher         变量的声明类型匹配器
     * @return 满足条件则返回true
     */
    public static boolean isTargetDeclaredType(VariableElement variableElement, Predicate<DeclaredType> matcher) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>() {

            @Override
            public Boolean visitDeclared(DeclaredType t, Void param) {
                // 访问声明的类型 eg: String str
                return matcher.test(t);
            }

            @Override
            protected Boolean defaultAction(TypeMirror e, Void param) {
                return false;
            }

        }, null);
    }

    /**
     * 是否是指定基本类型数组
     *
     * @param variableElement 变量
     * @param primitiveType   基本类型
     * @return true/false
     */
    public static boolean isTargetPrimitiveArrayType(VariableElement variableElement, TypeKind primitiveType) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>() {

            @Override
            public Boolean visitArray(ArrayType t, Void aVoid) {
                return t.getComponentType().getKind() == primitiveType;
            }

            @Override
            protected Boolean defaultAction(TypeMirror e, Void aVoid) {
                return false;
            }

        }, null);
    }

    /**
     * 判断一个类型是否包含泛型
     */
    public static boolean containsTypeVariable(final TypeMirror typeMirror) {
        return typeMirror.accept(new SimpleTypeVisitor8<Boolean, Void>() {

            @Override
            public Boolean visitDeclared(DeclaredType t, Void aVoid) {
                // method(Set<String> value)
                return t.getTypeArguments().size() > 0;
            }

            @Override
            public Boolean visitTypeVariable(TypeVariable t, Void aVoid) {
                // method(T value)
                return true;
            }

            @Override
            protected Boolean defaultAction(TypeMirror e, Void aVoid) {
                return false;
            }
        }, null);
    }

    // ------------------------------------------ 分割线 ------------------------------------------------

    public static void writeToFile(final TypeElement typeElement, final TypeSpec.Builder typeBuilder,
                                   final Elements elementUtils, final Messager messager, final Filer filer) {
        final TypeSpec typeSpec = typeBuilder.build();
        final JavaFile javaFile = JavaFile
                .builder(getPackageName(typeElement, elementUtils), typeSpec)
                // 不用导入java.lang包
                .skipJavaLangImports(true)
                // 4空格缩进
                .indent("    ")
                .build();
        try {
            // 输出到processingEnv.getFiler()会立即参与编译
            // 如果自己指定路径，可以生成源码到指定路径，但是可能无法被编译器检测到，本轮无法参与编译，需要再进行一次编译
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "writeToFile caught exception!", typeElement);
        }
    }

    public static String getClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString();
    }

    public static String getPackageName(TypeElement typeElement, Elements elementUtils) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }

}
