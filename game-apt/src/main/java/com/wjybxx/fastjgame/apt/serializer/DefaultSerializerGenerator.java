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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 */
class DefaultSerializerGenerator extends AbstractGenerator<SerializableClassProcessor> {

    private static final String READ_STRING_METHOD_NAME = "readString";
    private static final String READ_BYTES_METHOD_NAME = "readBytes";
    private static final String READ_COLLECTION_METHOD_NAME = "readCollection";
    private static final String READ_MAP_METHOD_NAME = "readMap";
    private static final String READ_ARRAY_METHOD_NAME = "readArray";
    private static final String READ_OBJECT_METHOD_NAME = "readObject";

    private static final String WRITE_STRING_METHOD_NAME = "writeString";
    private static final String WRITE_BYTES_METHOD_NAME = "writeBytes";
    private static final String WRITE_COLLECTION_METHOD_NAME = "writeCollection";
    private static final String WRITE_MAP_METHOD_NAME = "writeMap";
    private static final String WRITE_ARRAY_METHOD_NAME = "writeArray";
    private static final String WRITE_OBJECT_METHOD_NAME = "writeObject";

    private static final String CONSTRUCTOR_FIELD_NAME = "r_constructor";

    private TypeName instanceRawTypeName;
    private DeclaredType superDeclaredType;

    private TypeSpec.Builder typeBuilder;
    private CodeBlock.Builder staticCodeBlockBuilder;

    private MethodSpec getEntityMethod;
    private MethodSpec.Builder newInstanceMethodBuilder;
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
        superDeclaredType = typeUtils.getDeclaredType(processor.abstractSerializerTypeElement, typeUtils.erasure(typeElement.asType()));

        typeBuilder = TypeSpec.classBuilder(SerializableClassProcessor.getSerializerClassName(typeElement));
        staticCodeBlockBuilder = CodeBlock.builder();

        getEntityMethod = newGetEntityMethodBuilder();
        newInstanceMethodBuilder = newInstanceMethodBuilder();
        readFieldsMethodBuilder = newReadFieldsMethodBuilder();
        writeObjectMethodBuilder = newWriteObjectMethodBuilder();

        // 必须包含超类字段
        allFieldsAndMethodWithInherit = BeanUtils.getAllFieldsAndMethodsWithInherit(typeElement);
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

            addWriteStatement(variableElement);

            addReadStatement(variableElement);
        }

        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .superclass(TypeName.get(superDeclaredType))
                .addStaticBlock(staticCodeBlockBuilder.build())
                .addMethod(getEntityMethod)
                .addMethod(writeObjectMethodBuilder.build())
                .addMethod(newInstanceMethodBuilder.build())
                .addMethod(readFieldsMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private void genFactoryMethod() {
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            // 抽象类或接口
            newInstanceMethodBuilder.addStatement("throw new $T()", UnsupportedOperationException.class);
            return;
        }

        final ExecutableElement noArgsConstructor = BeanUtils.getNoArgsConstructor(typeElement);
        assert null != noArgsConstructor;
        if (!noArgsConstructor.getModifiers().contains(Modifier.PRIVATE)) {
            // 非private，直接new
            newInstanceMethodBuilder.addStatement("return new $T()", instanceRawTypeName);
        } else {
            // 创建构造函数反射字段
            final FieldSpec constructorFieldSpec = getConstructorFieldSpec();

            typeBuilder.addField(constructorFieldSpec);
            staticCodeBlockBuilder.addStatement("$L = $T.getNoArgsConstructor($T.class)", CONSTRUCTOR_FIELD_NAME, AptReflectUtils.class, instanceRawTypeName);
            // 反射创建对象
            newInstanceMethodBuilder.addStatement("return $L.newInstance($T.EMPTY_OBJECT_ARRAY)", CONSTRUCTOR_FIELD_NAME, AptReflectUtils.class);
        }
    }

    private FieldSpec getConstructorFieldSpec() {
        final ClassName className = ClassName.get(Constructor.class);
        final ParameterizedTypeName fieldTypeName = ParameterizedTypeName.get(className, instanceRawTypeName);
        return FieldSpec.builder(fieldTypeName, CONSTRUCTOR_FIELD_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
    }

    /**
     * 写对象用的getter方法
     * 且要求了一定有getter方法
     */
    private void addWriteStatement(VariableElement variableElement) {
        final String getterName = getGetterName(variableElement);
        final String writeMethodName = getWriteMethodName(variableElement);
        writeObjectMethodBuilder.addStatement("outputStream.$L(instance.$L())", writeMethodName, getterName);
    }

    private String getGetterName(VariableElement variableElement) {
        return BeanUtils.getterMethodName(variableElement.getSimpleName().toString(),
                BeanUtils.isPrimitiveBoolean(variableElement.asType()));
    }

    private String getWriteMethodName(VariableElement variableElement) {
        if (isPrimitiveType(variableElement)) {
            return getWritePrimitiveTypeMethodName(variableElement);
        }
        if (processor.isString(variableElement)) {
            return WRITE_STRING_METHOD_NAME;
        }
        if (isByteArray(variableElement)) {
            return WRITE_BYTES_METHOD_NAME;
        }
        if (processor.isCollection(variableElement)) {
            return WRITE_COLLECTION_METHOD_NAME;
        }
        if (processor.isMap(variableElement)) {
            return WRITE_MAP_METHOD_NAME;
        }
        if (AutoUtils.isArrayType(variableElement.asType())) {
            return WRITE_ARRAY_METHOD_NAME;
        }
        return WRITE_OBJECT_METHOD_NAME;
    }

    private static boolean isPrimitiveType(VariableElement variableElement) {
        return variableElement.asType().getKind().isPrimitive();
    }

    private static String getWritePrimitiveTypeMethodName(VariableElement variableElement) {
        return "write" + primitiveTypeName(variableElement);
    }

    private static String primitiveTypeName(VariableElement variableElement) {
        return BeanUtils.firstCharToUpperCase(variableElement.asType().getKind().name().toLowerCase());
    }

    private boolean isByteArray(VariableElement variableElement) {
        return AutoUtils.isTargetPrimitiveArrayType(variableElement, TypeKind.BYTE);
    }

    /**
     * 读对象用的setter
     */
    private void addReadStatement(VariableElement variableElement) {
        if (isContainerNotPrivateSetterMethod(variableElement)) {
            readBySetter(variableElement);
        } else {
            readByReflect(variableElement);
        }
    }

    private boolean isContainerNotPrivateSetterMethod(final VariableElement variableElement) {
        return BeanUtils.isContainerNotPrivateSetterMethod(typeUtils, variableElement, allFieldsAndMethodWithInherit);
    }

    private void readBySetter(VariableElement variableElement) {
        // 包含非private的setter方法
        final String setterName = getSetterName(variableElement);
        List<Object> params = new ArrayList<>(8);
        StringBuilder readFormat = new StringBuilder("instance.$L(");
        params.add(setterName);

        appendReadStatement(variableElement, readFormat, params);
        readFormat.append(")");

        readFieldsMethodBuilder.addStatement(readFormat.toString(), params.toArray());
    }

    private String getSetterName(VariableElement variableElement) {
        return BeanUtils.setterMethodName(variableElement.getSimpleName().toString(),
                BeanUtils.isPrimitiveBoolean(variableElement.asType()));
    }

    private void appendReadStatement(VariableElement variableElement, StringBuilder readFormat, List<Object> params) {
        if (processor.isCollection(variableElement)) {
            appendReadCollectionStatement(variableElement, readFormat, params);
        } else if (processor.isMap(variableElement)) {
            appendReadMapStatement(variableElement, readFormat, params);
        } else if (AutoUtils.isArrayType(variableElement.asType())) {
            appendReadArrayStatement(variableElement, readFormat, params);
        } else {
            final String readMethodName = getReadMethodName(variableElement);
            readFormat.append("inputStream.$L()");
            params.add(readMethodName);
        }
    }

    private void appendReadCollectionStatement(VariableElement variableElement, StringBuilder readFormat, List<Object> params) {
        final TypeMirror impTypeMirror = getMapOrCollectionImp(variableElement);
        if (processor.isEnumSet(impTypeMirror)) {
            final TypeMirror elementTypeMirror = AutoUtils.findFirstParameterActualType(variableElement.asType());
            if (null == elementTypeMirror) {
                messager.printMessage(Diagnostic.Kind.ERROR, "raw types is not allowed here (EnumSet)", variableElement);
                return;
            }

            final TypeName elementRawTypeName = TypeName.get(typeUtils.erasure(elementTypeMirror));

            final TypeName enumSetRawTypeName = ClassName.get(processor.enumSetRawTypeMirror);

            readFormat.append("inputStream.$L(size -> $T.noneOf($T.class))");
            params.add(READ_COLLECTION_METHOD_NAME);
            params.add(enumSetRawTypeName);
            params.add(elementRawTypeName);
        } else {
            final TypeName impTypeName = TypeName.get(typeUtils.erasure(impTypeMirror));

            readFormat.append("inputStream.$L($T::new)");
            params.add(READ_COLLECTION_METHOD_NAME);
            params.add(impTypeName);
        }
    }

    /**
     * 获取参数的具体类型
     */
    private TypeMirror getMapOrCollectionImp(VariableElement variableElement) {
        final DeclaredType declaredType = AutoUtils.getDeclaredType(variableElement.asType());
        if (!declaredType.asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            // 声明类型是具体类型
            return declaredType;
        }

        if (processor.isEnumMap(declaredType) || processor.isEnumSet(declaredType)) {
            // 声明类型是EnumMap 或 EnumSet
            return declaredType;
        }

        return processor.getFieldImplAnnotationValue(variableElement);
    }

    private void appendReadMapStatement(VariableElement variableElement, StringBuilder readFormat, List<Object> params) {
        final TypeMirror impTypeMirror = getMapOrCollectionImp(variableElement);
        if (processor.isEnumMap(impTypeMirror)) {
            final TypeMirror keyTypeMirror = AutoUtils.findFirstParameterActualType(variableElement.asType());
            if (null == keyTypeMirror) {
                messager.printMessage(Diagnostic.Kind.ERROR, "raw types is not allowed here (EnumMap)", variableElement);
                return;
            }

            final TypeName keyRawTypeName = TypeName.get(typeUtils.erasure(keyTypeMirror));

            final TypeName enumMapRawTypeName = ClassName.get(processor.enumMapRawTypeMirror);

            readFormat.append("inputStream.$L(size -> new $T<>($T.class))");
            params.add(READ_MAP_METHOD_NAME);
            params.add(enumMapRawTypeName);
            params.add(keyRawTypeName);
        } else {
            final TypeName impTypeName = TypeName.get(typeUtils.erasure(impTypeMirror));

            readFormat.append("inputStream.$L($T::new)");
            params.add(READ_MAP_METHOD_NAME);
            params.add(impTypeName);
        }
    }

    private void appendReadArrayStatement(VariableElement variableElement, StringBuilder readFormat, List<Object> params) {
        final TypeName componentTypeName = TypeName.get(AutoUtils.getComponentType(variableElement.asType()));

        readFormat.append("inputStream.$L($T.class)");
        params.add(READ_ARRAY_METHOD_NAME);
        params.add(componentTypeName);
    }

    private String getReadMethodName(VariableElement variableElement) {
        if (isPrimitiveType(variableElement)) {
            return getReadPrimitiveMethodName(variableElement);
        }
        if (processor.isString(variableElement)) {
            return READ_STRING_METHOD_NAME;
        }
        if (isByteArray(variableElement)) {
            return READ_BYTES_METHOD_NAME;
        }
        return READ_OBJECT_METHOD_NAME;
    }

    private void readByReflect(VariableElement variableElement) {
        // 需要定义反射构造方法字段
        final String fieldName = variableElement.getSimpleName().toString();
        final String reflectFieldName = "r_field_" + fieldName;
        typeBuilder.addField(Field.class, reflectFieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        // 这里需要使用getEnclosingElement获取真正定义该字段的类
        final TypeName fieldDeclaredClassTypeName = TypeName.get(typeUtils.erasure(variableElement.getEnclosingElement().asType()));
        staticCodeBlockBuilder.addStatement("$L = $T.getDeclaredField($T.class ,$S)", reflectFieldName, AptReflectUtils.class,
                fieldDeclaredClassTypeName, fieldName);

        // 反射赋值
        StringBuilder readFormat = new StringBuilder("$L.set(instance, ");
        List<Object> params = new ArrayList<>(8);
        params.add(reflectFieldName);

        appendReadStatement(variableElement, readFormat, params);
        readFormat.append(")");
        readFieldsMethodBuilder.addStatement(readFormat.toString(), params.toArray());
    }

    private static String getReadPrimitiveMethodName(VariableElement variableElement) {
        return "read" + primitiveTypeName(variableElement);
    }

    private MethodSpec newGetEntityMethodBuilder() {
        return processor.newGetEntityMethod(superDeclaredType);
    }

    private MethodSpec.Builder newInstanceMethodBuilder() {
        return MethodSpec.overriding(processor.newInstanceMethod, superDeclaredType, typeUtils);
    }

    private MethodSpec.Builder newWriteObjectMethodBuilder() {
        return processor.newWriteMethodBuilder(superDeclaredType);
    }

    private MethodSpec.Builder newReadFieldsMethodBuilder() {
        return MethodSpec.overriding(processor.readFieldsMethod, superDeclaredType, typeUtils);
    }

}
