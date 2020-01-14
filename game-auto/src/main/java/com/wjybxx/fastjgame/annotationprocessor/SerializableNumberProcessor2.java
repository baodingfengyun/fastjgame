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
import com.squareup.javapoet.*;
import com.wjybxx.fastjgame.utils.AutoUtils;
import com.wjybxx.fastjgame.utils.BeanUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.wjybxx.fastjgame.utils.AutoUtils.getClassName;

/**
 * 为要序列化的类生成序列化代理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/13
 */
@AutoService(Processor.class)
public class SerializableNumberProcessor2 extends AbstractProcessor {

    private static final String WIRETYPE_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.WireType";
    private static final String SERIALIZER_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.BeanSerializer";
    private static final String OUTPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.BeanOutputStream";
    private static final String INPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.BeanInputStream";
    private static final String CLONE_UTIL_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.BeanCloneUtil";

    private static final String WRITE_METHOD_NAME = "write";
    private static final String READ_METHOD_NAME = "read";
    private static final String CLONE_METHOD_NAME = "clone";
    private static final String FINDTYPE_METHOD_NAME = "findType";


    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    private AnnotationSpec processorInfoAnnotation;

    private TypeName wireTypeTypeName;
    private TypeElement serializerTypeElement;
    private TypeElement outputStreamTypeElement;
    private TypeElement inputStreamTypeElement;
    private TypeElement cloneUtilTypeElement;

    private TypeElement serializableClassElement;
    private DeclaredType serializableFieldDeclaredType;
    private DeclaredType numericalEnumDeclaredType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();

        processorInfoAnnotation = AutoUtils.newProcessorInfoAnnotation(getClass());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(SerializableNumberProcessor.SERIALIZABLE_CLASS_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return AutoUtils.SOURCE_VERSION;
    }

    private void ensureInited() {
        if (wireTypeTypeName != null) {
            return;
        }
        wireTypeTypeName = TypeName.get(elementUtils.getTypeElement(WIRETYPE_CANONICAL_NAME).asType());
        serializerTypeElement = elementUtils.getTypeElement(SERIALIZER_CANONICAL_NAME);
        outputStreamTypeElement = elementUtils.getTypeElement(OUTPUT_STREAM_CANONICAL_NAME);
        inputStreamTypeElement = elementUtils.getTypeElement(INPUT_STREAM_CANONICAL_NAME);
        cloneUtilTypeElement = elementUtils.getTypeElement(CLONE_UTIL_CANONICAL_NAME);

        serializableClassElement = elementUtils.getTypeElement(SerializableNumberProcessor.SERIALIZABLE_CLASS_CANONICAL_NAME);
        serializableFieldDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SerializableNumberProcessor.SERIALIZABLE_FIELD_CANONICAL_NAME));
        numericalEnumDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SerializableNumberProcessor.NUMBER_ENUM_CANONICAL_NAME));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();
        // 该注解只有类可以使用
        @SuppressWarnings("unchecked")
        Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(serializableClassElement);
        for (TypeElement typeElement : typeElementSet) {
            try {
                generateSerializer(typeElement);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), typeElement);
            }
        }
        return true;
    }

    private void generateSerializer(TypeElement typeElement) {
        if (typeElement.getKind() == ElementKind.CLASS) {
            if (isNumericalEnum(typeElement)) {
                genEnumSerializer(typeElement);
            } else {
                genClassSerializer(typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.ENUM) {
            genEnumSerializer(typeElement);
        }
    }

    private boolean isNumericalEnum(TypeElement typeElement) {
        return typeUtils.isSubtype(typeUtils.getDeclaredType(typeElement), numericalEnumDeclaredType);
    }

    private void genEnumSerializer(TypeElement typeElement) {
        // 现在还不是很有必要
    }

    private void genClassSerializer(final TypeElement typeElement) {
        if (!isAvailable(typeElement)) {
            return;
        }

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getSerializerClassName(typeElement));
        final CodeBlock.Builder wireTypeStaticCodeBlock = CodeBlock.builder();

        final TypeName instanceRawTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));
        final MethodSpec.Builder writeMethodBuilder = newWriteMethodBuilder(instanceRawTypeName);

        final MethodSpec.Builder readMethodBuilder = newReadMethodBuilder(instanceRawTypeName);
        readMethodBuilder.addStatement("$T instance = new $T()", instanceRawTypeName, instanceRawTypeName);

        final MethodSpec.Builder cloneMethodBuilder = newCloneMethodBuilder(instanceRawTypeName);
        cloneMethodBuilder.addStatement("$T result = new $T()", instanceRawTypeName, instanceRawTypeName);

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

            // value中，基本类型会被封装为包装类型，number是int类型
            final Integer number = AutoUtils.getAnnotationValueNotDefault(first.get(), SerializableNumberProcessor.NUMBER_METHOD_NAME);

            TypeName fieldRawTypeName = ParameterizedTypeName.get(typeUtils.erasure(variableElement.asType()));
            if (fieldRawTypeName.isPrimitive()) {
                fieldRawTypeName = fieldRawTypeName.box();
            }

            typeBuilder.addField(byte.class, "wireType" + number, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
            wireTypeStaticCodeBlock.addStatement("wireType$L = $T.$L($T.class)", number, wireTypeTypeName, FINDTYPE_METHOD_NAME, fieldRawTypeName);

            final String getterName = BeanUtils.getterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));
            final String setterName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));

            writeMethodBuilder.addStatement("outputStream.writeObject(wireType$L, instance.$L())", number, getterName);
            readMethodBuilder.addStatement("instance.$L(inputStream.readObject(wireType$L))", setterName, number);
            cloneMethodBuilder.addStatement("result.$L(util.clone(wireType$L, instance.$L()))", setterName, number, getterName);
        }

        readMethodBuilder.addStatement("return instance");
        cloneMethodBuilder.addStatement("return result");

        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addSuperinterface(TypeName.get(typeUtils.getDeclaredType(serializerTypeElement, typeUtils.erasure(typeElement.asType()))))
                .addStaticBlock(wireTypeStaticCodeBlock.build())
                .addMethod(writeMethodBuilder.build())
                .addMethod(readMethodBuilder.build())
                .addMethod(cloneMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    /**
     * 是否可以为其生成序列化工具类
     * 1. 必须非Private有无参构造方法
     * 2. 所有要序列化字段必须有对应的getter setter
     */
    private boolean isAvailable(TypeElement typeElement) {
        final ExecutableElement noArgsConstructor = BeanUtils.getNoArgsConstructor(typeElement);
        if (null == noArgsConstructor || noArgsConstructor.getModifiers().contains(Modifier.PRIVATE)) {
            return false;
        }

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

            TypeName fieldTypeName = TypeName.get(variableElement.asType());
            if (fieldTypeName.isPrimitive()) {
                fieldTypeName.box();
            }

            final String getterMethodName = BeanUtils.getterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldTypeName));
            final ExecutableElement getterMethod = AutoUtils.findMethodByName(typeElement, getterMethodName);
            if (null == getterMethod || getterMethod.getModifiers().contains(Modifier.PRIVATE)) {
                return false;
            }

            final String setterMethodName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldTypeName));
            final ExecutableElement setterMethod = AutoUtils.findMethodByName(typeElement, setterMethodName);
            if (null == setterMethod || setterMethod.getModifiers().contains(Modifier.PRIVATE)) {
                return false;
            }
        }
        return true;
    }

    private MethodSpec.Builder newWriteMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(WRITE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addParameter(instanceRawTypeName, "instance")
                .addParameter(TypeName.get(outputStreamTypeElement.asType()), "outputStream");
    }

    private MethodSpec.Builder newReadMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(READ_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .returns(instanceRawTypeName)
                .addParameter(TypeName.get(inputStreamTypeElement.asType()), "inputStream");
    }

    private MethodSpec.Builder newCloneMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(CLONE_METHOD_NAME)
                .returns(instanceRawTypeName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addParameter(instanceRawTypeName, "instance")
                .addParameter(TypeName.get(cloneUtilTypeElement.asType()), "util");
    }

    /**
     * 获取class对应的序列化工具类的类名
     */
    private String getSerializerClassName(TypeElement typeElement) {
        return getSerializerName(getClassName(typeElement));
    }

    /**
     * 暴露给应用层
     */
    public static String getSerializerName(String className) {
        return className + "Serializer";
    }
}
