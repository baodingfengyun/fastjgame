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

package com.wjybxx.fastjgame.annotationprocessor;

import com.google.auto.service.AutoService;
import com.wjybxx.fastjgame.utils.AutoUtils;
import com.wjybxx.fastjgame.utils.BeanUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 分析{@code SerializableField#number()}是否重复，以及是否在 [0,65535之间]
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class SerializableNumberProcessor extends AbstractProcessor {

    // 使用这种方式可以脱离对utils，net包的依赖
    private static final String SERIALIZABLE_CLASS_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.SerializableClass";
    private static final String SERIALIZABLE_FIELD_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.SerializableField";
    private static final String NUMBER_ENUM_CANONICAL_NAME = "com.wjybxx.fastjgame.enummapper.NumericalEnum";

    private static final String NUMBER_METHOD_NAME = "number";
    private static final String FOR_NUMBER_METHOD_NAME = "forNumber";

    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;

    private TypeElement serializableClassElement;
    private DeclaredType serializableFieldDeclaredType;
    private DeclaredType numericalEnumDeclaredType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(SERIALIZABLE_CLASS_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return AutoUtils.SOURCE_VERSION;
    }

    private void ensureInited() {
        if (serializableClassElement != null) {
            return;
        }
        serializableClassElement = elementUtils.getTypeElement(SERIALIZABLE_CLASS_CANONICAL_NAME);
        serializableFieldDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SERIALIZABLE_FIELD_CANONICAL_NAME));
        numericalEnumDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(NUMBER_ENUM_CANONICAL_NAME));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();
        // 该注解只有类可以使用
        @SuppressWarnings("unchecked")
        Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(serializableClassElement);
        for (TypeElement typeElement : typeElementSet) {
            checkNumber(typeElement);
        }
        return false;
    }

    private void checkNumber(TypeElement typeElement) {
        if (typeElement.getKind() == ElementKind.ENUM) {
            // 检查真正的枚举
            checkEnum(typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) {
            if (isNumericalEnum(typeElement)) {
                // 检查伪枚举
                checkForNumberMethod(typeElement);
            } else {
                // 检查普通类型
                checkClass(typeElement);
            }
        } else {
            // 只允许枚举和类使用，其它类型抛出编译错误
            messager.printMessage(Diagnostic.Kind.ERROR, "serializable class does not allow here", typeElement);
        }
    }

    private boolean isNumericalEnum(TypeElement typeElement) {
        return typeUtils.isSubtype(typeUtils.getDeclaredType(typeElement), numericalEnumDeclaredType);
    }

    private void checkClass(TypeElement typeElement) {
        final Set<Integer> numberSet = new HashSet<>(typeElement.getEnclosedElements().size());
        for (Element element : typeElement.getEnclosedElements()) {
            // 非成员属性
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            // 该注解只有Field可以使用
            final VariableElement variableElement = (VariableElement) element;
            // 查找该字段上的注解
            final Optional<? extends AnnotationMirror> first = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, variableElement, serializableFieldDeclaredType);
            // 该成员属性没有serializableField注解
            if (!first.isPresent()) {
                continue;
            }
            // 不能是static
            if (variableElement.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "serializable field can't be static", variableElement);
                continue;
            }
            // value中，基本类型会被封装为包装类型，number是int类型
            final Integer number = AutoUtils.getAnnotationValueNotDefault(first.get(), NUMBER_METHOD_NAME);
            // 取值范围检测
            if (number == null || number < 0 || number > 65535) {
                messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " must between [0, 65535]", variableElement);
                continue;
            }
            // 重复检测
            if (!numberSet.add(number)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " is duplicate!", variableElement);
            }
        }

        // 无参构造方法检测
        if (!BeanUtils.containsNoArgsConstructor(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "SerializableClass " + typeElement.getSimpleName() + " must contains no-args constructor, private is ok!",
                    typeElement);
        }
    }


    private void checkEnum(TypeElement typeElement) {
        // 要序列化的枚举必须实现 NumericalEnum 接口
        if (!isNumericalEnum(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR, typeElement.getSimpleName() + " must implements NumericalEnum!", typeElement);
        }
        checkForNumberMethod(typeElement);
    }

    /**
     * 检查forNumber静态方法
     */
    private void checkForNumberMethod(TypeElement typeElement) {
        if (!isContainStaticForNumberMethod(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    String.format("%s must contains 'static %s forNumber(int)' method, private is ok!", typeElement.getSimpleName(), typeElement.getSimpleName()),
                    typeElement);
        }
    }

    /**
     * 是否包含静态的forNumber方法 - static T forNumber(int)
     */
    private boolean isContainStaticForNumberMethod(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> method.getSimpleName().toString().equals(FOR_NUMBER_METHOD_NAME))
                .filter(method -> method.getParameters().size() == 1)
                .filter(method -> method.getParameters().get(0).asType().getKind() == TypeKind.INT)
                .anyMatch(method -> typeUtils.isSameType(method.getReturnType(), typeUtils.getDeclaredType(typeElement)));
    }
}
