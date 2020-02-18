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
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 代码生成通用工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
public class AutoUtils {

    /**
     * 生成的代码的源码版本
     */
    public static final SourceVersion SOURCE_VERSION = SourceVersion.RELEASE_11;
    /**
     * 由于生成的代码不能很好的处理泛型等信息，因此需要抑制警告
     */
    public static final AnnotationSpec SUPPRESS_UNCHECKED_ANNOTATION = AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "{\"unchecked\", \"rawtypes\"}")
            .build();

    private AutoUtils() {

    }

    /**
     * 为生成代码的注解处理器创建一个通用注解
     *
     * @param processorType 注解处理器
     * @return 代码生成信息注解
     */
    public static AnnotationSpec newProcessorInfoAnnotation(Class<? extends AbstractProcessor> processorType) {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", processorType.getCanonicalName())
                .build();
    }

    /**
     * 筛选出java源文件 - 去除带有注解的class文件
     *
     * @param typeElementSet 带有注解的所有元素
     * @param elementUtils   用于获取元素的完整类名
     * @return 过滤后只有java源文件的元素
     */
    public static Set<TypeElement> selectSourceFile(Set<TypeElement> typeElementSet, Elements elementUtils) {
        return typeElementSet.stream()
                .filter(e -> {
                    try {
                        // 如果注解的保留策略是runtime，则会把已经编译成class的文件再统计进来，这里需要过滤。
                        // 不能使用getSystemClassLoader()，会加载不到。
                        Class.forName(elementUtils.getBinaryName(e).toString());
                        return false;
                    } catch (Exception ignore) {
                        return true;
                    }
                })
                .collect(Collectors.toSet());
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
    }

    // ----------------------------------------------------- 分割线 -----------------------------------------------

    /**
     * 通过名字查找对应的方法
     */
    public static ExecutableElement findMethodByName(TypeElement typeElement, String methodName) {
        return typeElement.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(e -> e.getSimpleName().toString().equals(methodName))
                .findFirst().orElse(null);
    }

    /**
     * 将继承体系展开，不包含实现的接口。
     */
    public static List<TypeElement> flatInherit(TypeElement typeElement) {
        final List<TypeElement> result = new LinkedList<>();
        result.add(typeElement);

        for (TypeMirror typeMirror = typeElement.getSuperclass(); typeMirror.getKind() != TypeKind.NONE; ) {
            final TypeElement parentTypeElement = (TypeElement) (getDeclaredType(typeMirror).asElement());
            result.add(parentTypeElement);
            typeMirror = parentTypeElement.getSuperclass();
        }
        return result;
    }

    /**
     * 将继承体系展开，不包含实现的接口。
     * 并逆序返回。
     */
    public static List<TypeElement> flatInheritAndReverse(TypeElement typeElement) {
        final List<TypeElement> result = flatInherit(typeElement);
        Collections.reverse(result);
        return result;
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
    public static Optional<? extends AnnotationMirror> findFirstAnnotationWithoutInheritance(Types typeUtils, Element element, TypeMirror targetType) {
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
    public static Optional<? extends AnnotationMirror> findFirstAnnotationWithDefaults(Types typeUtils, Elements elementUtils, Element element, TypeMirror targetType) {
        // 查找该字段上的注解
        return elementUtils.getAllAnnotationMirrors(element).stream()
                .filter(annotationMirror -> typeUtils.isSameType(annotationMirror.getAnnotationType(), targetType))
                .findFirst();
    }

    @Nullable
    public static AnnotationValue getAnnotationValueNotDefault(AnnotationMirror annotationMirror, String propertyName) {
        return annotationMirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取注解上的某一个属性的值，包含default值
     *
     * @param annotationMirror 注解编译信息
     * @param propertyName     属性的名字
     * @return object
     */
    @Nonnull
    public static AnnotationValue getAnnotationValueWithDefaults(Elements elementUtils, AnnotationMirror annotationMirror, String propertyName) {
        final Optional<? extends AnnotationValue> first = elementUtils.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(propertyName))
                .map(Map.Entry::getValue)
                .findFirst();
        assert first.isPresent();
        return first.get();
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
    public static <T> T getAnnotationValueValueNotDefault(AnnotationMirror annotationMirror, String propertyName) {
        return (T) Optional.ofNullable(getAnnotationValueNotDefault(annotationMirror, propertyName))
                .map(AnnotationValue::getValue)
                .orElse(null);
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
    public static <T> T getAnnotationValueValueWithDefaults(Elements elementUtils, AnnotationMirror annotationMirror, String propertyName) {
        return (T) Optional.of(getAnnotationValueWithDefaults(elementUtils, annotationMirror, propertyName))
                .map(AnnotationValue::getValue)
                .get();
    }

    /**
     * 获取注解中引用的class对象的类型
     */
    public static TypeMirror getAnnotationValueTypeMirror(AnnotationValue annotationValue) {
        return annotationValue.accept(new SimpleAnnotationValueVisitor8<>() {
            @Override
            public TypeMirror visitType(TypeMirror t, Object o) {
                return t;
            }
        }, null);
    }


    /**
     * 忽略两个类型的泛型参数，判断第一个是否是第二个的子类型。
     * 如果考虑泛型的话，会存在区别
     */
    public static boolean isSubTypeIgnoreTypeParameter(Types typeUtil, TypeMirror first, TypeMirror second) {
        return typeUtil.isSubtype(typeUtil.erasure(first), typeUtil.erasure(second));
    }

    /**
     * 忽略两个类型的泛型参数，判断第一个和第二个类型是否相同
     * 如果考虑泛型的话，会存在区别
     */
    public static boolean isSameTypeIgnoreTypeParameter(Types typeUtil, TypeMirror first, TypeMirror second) {
        return typeUtil.isSameType(typeUtil.erasure(first), typeUtil.erasure(second));
    }

    /**
     * 获取一个元素的声明类型
     */
    public static DeclaredType getDeclaredType(TypeMirror typeMirror) {
        return typeMirror.accept(new SimpleTypeVisitor8<>() {
            @Override
            public DeclaredType visitDeclared(DeclaredType t, Object o) {
                return t;
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
     * 获取第一个参数的真实类型
     */
    @Nullable
    public static TypeMirror getFirstParameterActualType(TypeMirror typeMirror) {
        return typeMirror.accept(new SimpleTypeVisitor8<TypeMirror, Void>() {
            @Override
            public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
                if (t.getTypeArguments().size() == 0) {
                    return null;
                }

                final TypeMirror typeArgument = t.getTypeArguments().get(0);
                if (typeArgument.getKind() == TypeKind.DECLARED) {
                    return typeArgument;
                }

                if (typeArgument.getKind() == TypeKind.WILDCARD) {
                    // may nullable
                    return ((WildcardType) typeArgument).getSuperBound();
                }

                if (typeArgument.getKind() == TypeKind.TYPEVAR) {
                    // may object
                    return ((TypeVariable) typeArgument).getUpperBound();
                }

                return null;
            }

            @Override
            protected TypeMirror defaultAction(TypeMirror e, Void aVoid) {
                return null;
            }

        }, null);
    }

    // ------------------------------------------ 分割线 ------------------------------------------------

    /**
     * @param originTypeElement 原始类文件，用于获取包名，以及打印错误
     */
    public static void writeToFile(final TypeElement originTypeElement, final TypeSpec.Builder typeBuilder,
                                   final Elements elementUtils, final Messager messager, final Filer filer) {
        final TypeSpec typeSpec = typeBuilder.build();
        final JavaFile javaFile = JavaFile
                .builder(getPackageName(originTypeElement, elementUtils), typeSpec)
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
            messager.printMessage(Diagnostic.Kind.ERROR, "writeToFile caught exception!", originTypeElement);
        }
    }

    public static String getPackageName(TypeElement typeElement, Elements elementUtils) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }

    /**
     * 获取堆栈信息，避免引入commons-lang3
     */
    public static String getStackTrace(Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}
