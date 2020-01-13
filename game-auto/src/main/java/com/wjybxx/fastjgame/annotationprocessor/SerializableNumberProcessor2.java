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

    private static final String SERIALIZER_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.BeanSerializer";
    private static final String OUTPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.GameOutputStream";
    private static final String INPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.GameInputStream";
    private static final String WIRETYPE_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.WireType";

    private static final String WRITE_METHOD_NAME = "write";
    private static final String READ_METHOD_NAME = "read";
    private static final String FINDTYPE_METHOD_NAME = "findType";


    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    private AnnotationSpec processorInfoAnnotation;

    private TypeElement serializerTypeElement;
    private TypeElement outputStreamTypeElement;
    private TypeElement inputStreamTypeElement;

    private DeclaredType serializerDeclaredType;
    private ExecutableElement superWriteMethod;
    private ExecutableElement superReadMethod;
    private TypeName wireTypeTypeName;

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
        if (serializableClassElement != null) {
            return;
        }
        serializerTypeElement = elementUtils.getTypeElement(SERIALIZER_CANONICAL_NAME);
        outputStreamTypeElement = elementUtils.getTypeElement(OUTPUT_STREAM_CANONICAL_NAME);
        inputStreamTypeElement = elementUtils.getTypeElement(INPUT_STREAM_CANONICAL_NAME);

        serializerDeclaredType = typeUtils.getDeclaredType(serializerTypeElement);
        superWriteMethod = AutoUtils.findMethodByName(serializerTypeElement, WRITE_METHOD_NAME);
        superReadMethod = AutoUtils.findMethodByName(serializerTypeElement, READ_METHOD_NAME);
        wireTypeTypeName = TypeName.get(elementUtils.getTypeElement(WIRETYPE_CANONICAL_NAME).asType());

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

            } else {
                genClassSerializer(typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.ENUM) {

        }
    }

    private boolean isNumericalEnum(TypeElement typeElement) {
        return typeUtils.isSubtype(typeUtils.getDeclaredType(typeElement), numericalEnumDeclaredType);
    }

    private void genEnumSerializer(TypeElement typeElement) {

    }

    private void genClassSerializer(final TypeElement typeElement) {
        if (!isAvailable(typeElement)) {
            return;
        }

        final TypeName instanceTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));
        final MethodSpec.Builder writeMethodBuilder = getWriteMethodBuilder(instanceTypeName);

        final MethodSpec.Builder readMethodBuilder = getReadMethodBuilder(instanceTypeName);
        readMethodBuilder.addStatement("$T instance = new $T()", instanceTypeName, instanceTypeName);

        final CodeBlock.Builder typeIndexesStaticCodeBlock = CodeBlock.builder();
        typeIndexesStaticCodeBlock.add("typeIndexes = new byte[] {");

        int index = 0;
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

            TypeName fieldTypeName = ParameterizedTypeName.get(variableElement.asType());
            if (fieldTypeName.isPrimitive()) {
                fieldTypeName = fieldTypeName.box();
            }

            final String getterName = BeanUtils.getterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldTypeName));
            final String setterName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString());

            typeIndexesStaticCodeBlock.add("$T.$L($T.class),\n", wireTypeTypeName, FINDTYPE_METHOD_NAME, fieldTypeName);
            writeMethodBuilder.addStatement("outputStream.writeObject(typeIndexes[$L], instance.$L())", index, getterName);
            readMethodBuilder.addStatement("instance.$L(inputStream.readObject(typeIndexes[$L]))", setterName, index);

            index++;
        }

        readMethodBuilder.addStatement("return instance");

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getSerializerClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                // 类型索引及静态代码块
                .addField(byte[].class, "typeIndexes", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addStaticBlock(typeIndexesStaticCodeBlock.add("};\n").build())
                // 读写方法
                .addMethod(writeMethodBuilder.build())
                .addSuperinterface(TypeName.get(typeUtils.getDeclaredType(serializerTypeElement, typeUtils.erasure(typeElement.asType()))))
                .addMethod(readMethodBuilder.build());

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

            final String setterMethodName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString());
            final ExecutableElement setterMethod = AutoUtils.findMethodByName(typeElement, setterMethodName);
            if (null == setterMethod || setterMethod.getModifiers().contains(Modifier.PRIVATE)) {
                return false;
            }
        }
        return true;
    }

    private MethodSpec.Builder getWriteMethodBuilder(TypeName instanceTypeName) {
        return MethodSpec.methodBuilder(WRITE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addParameter(instanceTypeName, "instance")
                .addParameter(TypeName.get(outputStreamTypeElement.asType()), "outputStream");
    }

    private MethodSpec.Builder getReadMethodBuilder(TypeName instanceTypeName) {
        return MethodSpec.methodBuilder(READ_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .returns(instanceTypeName)
                .addParameter(TypeName.get(inputStreamTypeElement.asType()), "inputStream");
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
