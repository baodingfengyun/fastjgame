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

package com.wjybxx.fastjgame.apt.serializer;

import com.squareup.javapoet.*;
import com.wjybxx.fastjgame.apt.core.AbstractGenerator;
import com.wjybxx.fastjgame.apt.utils.AptReflectUtils;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;
import com.wjybxx.fastjgame.apt.utils.BeanUtils;

import javax.lang.model.element.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static com.wjybxx.fastjgame.apt.serializer.SerializableClassProcessor.*;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 */
class DefaultSerializerGenerator extends AbstractGenerator<SerializableClassProcessor> {

    private static final String FACTORY_METHOD_NAME = "newInstance";
    private static final String READ_FIELDS_METHOD_NAME = "readFields";

    private TypeName instanceRawTypeName;

    private TypeSpec.Builder typeBuilder;
    private CodeBlock.Builder staticCodeBlockBuilder;

    private MethodSpec getEntityMethod;
    private MethodSpec.Builder factoryMethodBuilder;
    private MethodSpec.Builder readFieldsMethodBuilder;
    private MethodSpec.Builder writeObjectMethodBuilder;

    private List<? extends Element> allFieldsAndMethodWithInherit;

    DefaultSerializerGenerator(SerializableClassProcessor processor, TypeElement typeElement) {
        super(processor, typeElement);
    }

    @Override
    public void execute() {
        init();

        gen();
    }

    private void init() {
        instanceRawTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));

        typeBuilder = TypeSpec.classBuilder(SerializableClassProcessor.getSerializerClassName(typeElement));
        staticCodeBlockBuilder = CodeBlock.builder();

        getEntityMethod = newGetEntityMethodBuilder(instanceRawTypeName);
        factoryMethodBuilder = newFactoryMethodBuilder(instanceRawTypeName);
        readFieldsMethodBuilder = newReadFieldsMethodBuilder(instanceRawTypeName);
        writeObjectMethodBuilder = newWriteObjectMethodBuilder(instanceRawTypeName);

        // 必须包含超类字段
        allFieldsAndMethodWithInherit = processor.getAllFieldsAndMethodsWithInherit(typeElement);
    }

    private void gen() {
        genFactoryMethod();

        for (Element element : allFieldsAndMethodWithInherit) {
            // 非成员属性
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            // 该注解只有Field可以使用
            final VariableElement variableElement = (VariableElement) element;
            // 查找该字段上的注解
            if (!processor.isSerializableField(variableElement)) {
                continue;
            }

            final TypeName fieldRawTypeName = ParameterizedTypeName.get(typeUtils.erasure(variableElement.asType()));
            final String filedWireTypeFieldName = "wireType_" + variableElement.getSimpleName().toString();

            typeBuilder.addField(byte.class, filedWireTypeFieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
            staticCodeBlockBuilder.addStatement("$L = $T.$L($T.class)", filedWireTypeFieldName, processor.wireTypeTypeName, FINDTYPE_METHOD_NAME, fieldRawTypeName);

            addReadStatement(variableElement, fieldRawTypeName, filedWireTypeFieldName);

            addWriteStatement(variableElement, fieldRawTypeName, filedWireTypeFieldName);
        }

        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .superclass(TypeName.get(typeUtils.getDeclaredType(processor.abstractSerializerElement, typeUtils.erasure(typeElement.asType()))))
                .addStaticBlock(staticCodeBlockBuilder.build())
                .addMethod(getEntityMethod)
                .addMethod(factoryMethodBuilder.build())
                .addMethod(writeObjectMethodBuilder.build())
                .addMethod(readFieldsMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private void genFactoryMethod() {
        final ExecutableElement noArgsConstructor = BeanUtils.getNoArgsConstructor(typeElement);
        assert null != noArgsConstructor;
        if (!noArgsConstructor.getModifiers().contains(Modifier.PRIVATE)) {
            // 非private，直接new
            factoryMethodBuilder.addStatement("return new $T()", instanceRawTypeName);
        } else {
            // 创建构造函数反射字段
            final String constructorFieldName = "r_constructor";
            final FieldSpec constructorFieldSpec = getConstructorFieldSpec(constructorFieldName);

            typeBuilder.addField(constructorFieldSpec);
            staticCodeBlockBuilder.addStatement("$L = $T.getNoArgsConstructor($T.class)", constructorFieldName, AptReflectUtils.class, instanceRawTypeName);
            // 反射创建对象
            factoryMethodBuilder.addStatement("return $L.newInstance()", constructorFieldName);
        }
    }

    private FieldSpec getConstructorFieldSpec(String constructorFieldName) {
        final ClassName className = ClassName.get(Constructor.class);
        final ParameterizedTypeName fieldTypeName = ParameterizedTypeName.get(className, instanceRawTypeName);
        return FieldSpec.builder(fieldTypeName, constructorFieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
    }

    /**
     * 写对象用的getter方法(一定有get方法)
     */
    private void addWriteStatement(VariableElement variableElement, TypeName fieldRawTypeName, String filedWireTypeFieldName) {
        final String getterName = BeanUtils.getterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));
        writeObjectMethodBuilder.addStatement("outputStream.$L($L, instance.$L())", WRITE_FIELD_METHOD_NAME, filedWireTypeFieldName, getterName);
    }

    /**
     * 读对象用的setter
     */
    private void addReadStatement(VariableElement variableElement, TypeName fieldRawTypeName, final String filedWireTypeFieldName) {
        final String fieldName = variableElement.getSimpleName().toString();
        if (isContainerNotPrivateSetterMethod(variableElement)) {
            // 包含非private的setter方法
            final String setterName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));
            readFieldsMethodBuilder.addStatement("instance.$L(inputStream.$L($L))", setterName, READ_FIELD_METHOD_NAME, filedWireTypeFieldName);
        } else {
            // 需要定义反射构造方法
            final String reflectFieldName = "r_field_" + fieldName;
            typeBuilder.addField(Field.class, reflectFieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

            // 这里需要使用getEnclosingElement获取真正定义该字段的类
            final TypeName fieldDeclaredClassTypeName = TypeName.get(typeUtils.erasure(variableElement.getEnclosingElement().asType()));
            staticCodeBlockBuilder.addStatement("$L = $T.getDeclaredField($T.class ,$S)", reflectFieldName, AptReflectUtils.class,
                    fieldDeclaredClassTypeName, fieldName);

            // 反射赋值
            readFieldsMethodBuilder.addStatement("$L.set(instance, inputStream.$L($L))", reflectFieldName, READ_FIELD_METHOD_NAME, filedWireTypeFieldName);
        }
    }

    private boolean isContainerNotPrivateSetterMethod(final VariableElement variableElement) {
        final String fieldName = variableElement.getSimpleName().toString();
        final boolean isBoolean = BeanUtils.isBoolean(typeUtils, variableElement.asType());
        final String setterMethodName = BeanUtils.setterMethodName(fieldName, isBoolean);

        // 非private的
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

    private MethodSpec.Builder newFactoryMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(FACTORY_METHOD_NAME)
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(Override.class)
                .addException(Exception.class)
                .returns(instanceRawTypeName);
    }

    private MethodSpec newGetEntityMethodBuilder(TypeName instanceRawTypeName) {
        return processor.newGetEntityMethod(instanceRawTypeName);
    }

    private MethodSpec.Builder newWriteObjectMethodBuilder(TypeName instanceRawTypeName) {
        return processor.newWriteMethodBuilder(instanceRawTypeName);
    }

    private MethodSpec.Builder newReadFieldsMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(READ_FIELDS_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(Exception.class)
                .addParameter(instanceRawTypeName, "instance")
                .addParameter(TypeName.get(processor.inputStreamTypeElement.asType()), "inputStream");
    }

}
