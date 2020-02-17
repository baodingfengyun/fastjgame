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

package com.wjybxx.fastjgame.apt.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;
import com.wjybxx.fastjgame.apt.utils.BeanUtils;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 1. 对于普通类：必须包含无参构造方法，且field注解的number必须在 0-65535之间
 * 2. 对于枚举：必须实现 NumericalEnum 接口，且提供非private的forNumber方法
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class SerializableClassProcessor extends MyAbstractProcessor {

    // 使用这种方式可以脱离对utils，net包的依赖
    private static final String SERIALIZABLE_CLASS_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.SerializableClass";
    private static final String SERIALIZABLE_FIELD_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.SerializableField";

    private static final String NUMBER_ENUM_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.entity.NumericalEntity";
    private static final String FOR_NUMBER_METHOD_NAME = "forNumber";
    private static final String GET_NUMBER_METHOD_NAME = "getNumber";

    private static final String INDEXABLE_ENTITY_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.entity.IndexableEntity";
    private static final String FOR_INDEX_METHOD_NAME = "forIndex";
    private static final String GET_INDEX_METHOD_NAME = "getIndex";

    private static final String WIRETYPE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.binary.WireType";
    private static final String WIRE_TYPE_INT = "INT";
    private static final String FINDTYPE_METHOD_NAME = "findType";

    private static final String SERIALIZER_CANONICAL_NAME = "com.wjybxx.fastjgame.net.binary.EntitySerializer";
    private static final String OUTPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.net.binary.EntityOutputStream";
    private static final String INPUT_STREAM_CANONICAL_NAME = "com.wjybxx.fastjgame.net.binary.EntityInputStream";

    private static final String WRITE_OBJECT_METHOD_NAME = "writeObject";
    private static final String READ_OBJECT_METHOD_NAME = "readObject";
    private static final String WRITE_FIELD_METHOD_NAME = "writeField";
    private static final String READ_FIELD_METHOD_NAME = "readField";

    private TypeMirror mapTypeMirror;
    private TypeMirror collectionTypeMirror;

    private TypeElement serializableClassElement;
    private DeclaredType serializableFieldDeclaredType;

    private TypeElement dbEntityTypeElement;
    private DeclaredType dbFieldDeclaredType;
    private DeclaredType impDeclaredType;

    private DeclaredType numericalEnumDeclaredType;
    private DeclaredType indexableEntityDeclaredType;

    private TypeName wireTypeTypeName;
    private TypeElement serializerTypeElement;
    private TypeElement outputStreamTypeElement;
    private TypeElement inputStreamTypeElement;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(SERIALIZABLE_CLASS_CANONICAL_NAME, DBEntityProcessor.DB_ENTITY_CANONICAL_NAME);
    }

    @Override
    protected void ensureInited() {
        if (serializableClassElement != null) {
            return;
        }

        mapTypeMirror = elementUtils.getTypeElement(Map.class.getCanonicalName()).asType();
        collectionTypeMirror = elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType();

        serializableClassElement = elementUtils.getTypeElement(SERIALIZABLE_CLASS_CANONICAL_NAME);
        serializableFieldDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SERIALIZABLE_FIELD_CANONICAL_NAME));

        dbEntityTypeElement = elementUtils.getTypeElement(DBEntityProcessor.DB_ENTITY_CANONICAL_NAME);
        dbFieldDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(DBEntityProcessor.DB_FIELD_CANONICAL_NAME));
        impDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(DBEntityProcessor.IMPL_CANONICAL_NAME));

        numericalEnumDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(NUMBER_ENUM_CANONICAL_NAME));
        indexableEntityDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(INDEXABLE_ENTITY_CANONICAL_NAME));

        wireTypeTypeName = TypeName.get(elementUtils.getTypeElement(WIRETYPE_CANONICAL_NAME).asType());
        serializerTypeElement = elementUtils.getTypeElement(SERIALIZER_CANONICAL_NAME);
        outputStreamTypeElement = elementUtils.getTypeElement(OUTPUT_STREAM_CANONICAL_NAME);
        inputStreamTypeElement = elementUtils.getTypeElement(INPUT_STREAM_CANONICAL_NAME);
    }

    @Override
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 该注解只有类可以使用
        @SuppressWarnings("unchecked") final Set<TypeElement> allTypeElements = (Set<TypeElement>) roundEnv.getElementsAnnotatedWithAny(serializableClassElement, dbEntityTypeElement);
        final Set<TypeElement> sourceFileTypeElementSet = AutoUtils.selectSourceFile(allTypeElements, elementUtils);

        for (TypeElement typeElement : sourceFileTypeElementSet) {
            try {
                checkBase(typeElement);
                generateSerializer(typeElement);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), typeElement);
            }
        }
        return true;
    }

    /**
     * 基础信息检查
     */
    private void checkBase(TypeElement typeElement) {
        if (typeElement.getKind() == ElementKind.ENUM) {
            checkEnum(typeElement);
        } else {
            if (isNumericalEnum(typeElement)) {
                checkNumericalEnum(typeElement);
            } else if (isIndexableEntity(typeElement)) {
                checkIndexableEntity(typeElement);
            } else if (typeElement.getKind() == ElementKind.CLASS) {
                // 检查普通类
                checkClass(typeElement);
            } else {
                // 其它类型抛出编译错误
                messager.printMessage(Diagnostic.Kind.ERROR, "unsupported class", typeElement);
            }
        }
    }

    /**
     * 检查枚举 - 要序列化的枚举，必须实现 NumericalEnum 接口，否则无法序列化，或自己手写serializer。
     */
    private void checkEnum(TypeElement typeElement) {
        if (!isNumericalEnum(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "serializable enum must implement " + numericalEnumDeclaredType.asElement().getSimpleName(),
                    typeElement);
        }
        checkNumericalEnum(typeElement);
    }

    private boolean isNumericalEnum(TypeElement typeElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, typeElement.asType(), numericalEnumDeclaredType);
    }

    /**
     * 检查 NumericalEnum 的子类是否有forNumber方法
     */
    private void checkNumericalEnum(TypeElement typeElement) {
        if (!isContainStaticNotPrivateForNumberMethod(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    String.format("%s must contains a not private 'static %s forNumber(int)' method!", typeElement.getSimpleName(), typeElement.getSimpleName()),
                    typeElement);
        }
    }

    /**
     * 是否包含静态的非private的forNumber方法 - static T forNumber(int)
     * (一定有getNumber方法)
     */
    private boolean isContainStaticNotPrivateForNumberMethod(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !method.getModifiers().contains(Modifier.PRIVATE))
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> method.getSimpleName().toString().equals(FOR_NUMBER_METHOD_NAME))
                .filter(method -> method.getParameters().size() == 1)
                .anyMatch(method -> method.getParameters().get(0).asType().getKind() == TypeKind.INT);
    }

    private boolean isIndexableEntity(TypeElement typeElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, typeElement.asType(), indexableEntityDeclaredType);
    }

    /**
     * 检查可索引的实体，检查是否有forIndex方法
     */
    private void checkIndexableEntity(TypeElement typeElement) {
        final TypeMirror indexTypeMirror = getIndexTypeMirror(typeElement);
        if (!isContainsStaticForIndexMethod(typeElement, indexTypeMirror)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    String.format("%s must contains a not private 'static %s forIndex(%s)' method!",
                            typeElement.getSimpleName(), typeElement.getSimpleName(), indexTypeMirror.toString()),
                    typeElement);
        }
    }

    /**
     * 获取索引类型
     */
    private TypeMirror getIndexTypeMirror(TypeElement typeElement) {
        final ExecutableElement getIndexMethod = typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(e -> ((Element) e).getSimpleName().toString().equals(GET_INDEX_METHOD_NAME))
                .filter(e -> e.getParameters().isEmpty())
                .findFirst()
                .orElse(null);
        if (getIndexMethod == null) {
            throw new IllegalArgumentException("can't find getIndex method");
        }
        return getIndexMethod.getReturnType();
    }

    /**
     * @param typeElement     方法的返回值类型
     * @param indexTypeMirror 方法的参数类型
     */
    private boolean isContainsStaticForIndexMethod(final TypeElement typeElement, final TypeMirror indexTypeMirror) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .filter(method -> !method.getModifiers().contains(Modifier.PRIVATE))
                .filter(method -> AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, method.getReturnType(), typeElement.asType()))
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> method.getSimpleName().toString().equals(FOR_INDEX_METHOD_NAME))
                .filter(method -> method.getParameters().size() == 1)
                .anyMatch(method -> typeUtils.isSameType(method.getParameters().get(0).asType(), indexTypeMirror));
    }

    private void checkClass(TypeElement typeElement) {
        // 父类可能是不序列化的，但是有字段要序列化
        final List<? extends Element> allFieldsAndMethodWithInherit = getAllFieldsAndMethodsWithInherit(typeElement);
        for (Element element : allFieldsAndMethodWithInherit) {
            // 非成员属性
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }

            final VariableElement variableElement = (VariableElement) element;
            // 不需要序列化
            if (!isSerializableField(variableElement)) {
                continue;
            }

            // 不能是static
            if (variableElement.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "serializable field can't be static", variableElement);
                continue;
            }

            // 必须包含非private的getter方法
            if (!containerNotPrivateGetterMethod(variableElement, allFieldsAndMethodWithInherit)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "serializable field must contains a not private getter", variableElement);
                continue;
            }

            // map和集合类型
            if (isMapOrCollection(variableElement)) {
                checkMapAndCollectionField(variableElement);
            }
        }

        // 无参构造方法检测
        if (!BeanUtils.containsNoArgsConstructor(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "SerializableClass " + typeElement.getSimpleName() + " must contains no-args constructor, private is ok!",
                    typeElement);
        }
    }

    /**
     * 用于 {@link DBEntityProcessor#DB_FIELD_CANONICAL_NAME}注解和{@link #SERIALIZABLE_FIELD_CANONICAL_NAME}注解的字段是可以序列化的
     */
    private boolean isSerializableField(VariableElement variableElement) {
        final Optional<? extends AnnotationMirror> a = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, variableElement, serializableFieldDeclaredType);
        if (a.isPresent()) {
            return true;
        }
        final Optional<? extends AnnotationMirror> b = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, variableElement, dbFieldDeclaredType);
        return b.isPresent();
    }

    /**
     * {@link Elements#getAllMembers(TypeElement)}只包含父类的公共属性，不包含私有的东西。
     */
    private List<Element> getAllFieldsAndMethodsWithInherit(TypeElement typeElement) {
        final List<TypeElement> flatInherit = AutoUtils.flatInherit(typeElement);
        return flatInherit.stream()
                .flatMap(e -> e.getEnclosedElements().stream())
                .filter(e -> e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }


    /**
     * 是否包含非private的getter方法
     *
     * @param allFieldsAndMethodWithInherit 所有的字段和方法，可能在父类中
     */
    private boolean containerNotPrivateGetterMethod(final VariableElement variableElement, final List<? extends Element> allFieldsAndMethodWithInherit) {
        final String fieldName = variableElement.getSimpleName().toString();
        final boolean isBoolean = BeanUtils.isBoolean(typeUtils, variableElement.asType());
        final String getterMethodName = BeanUtils.getterMethodName(fieldName, isBoolean);

        // 非private的
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

    private void checkMapAndCollectionField(VariableElement variableElement) {
        final DeclaredType declaredType = AutoUtils.getDeclaredType(variableElement.asType());
        if (!declaredType.asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            // 声明类型是具体类型
            return;
        }

        final TypeMirror implTypeMirror = getFieldImplType(variableElement);
        if (implTypeMirror == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Abstract MapOrCollection must contains impl annotation " + DBEntityProcessor.IMPL_CANONICAL_NAME,
                    variableElement);
            return;
        }

        if (!AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, implTypeMirror, variableElement.asType())) {
            messager.printMessage(Diagnostic.Kind.ERROR, "MapOrCollectionImpl must be field sub type", variableElement);
            return;
        }

        // 子类型一定是个class，一定是DeclaredType
        final DeclaredType impDeclaredType = (DeclaredType) implTypeMirror;
        if (impDeclaredType.asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "MapOrCollectionImpl must can't be abstract class", variableElement);
            return;
        }

        final ExecutableElement noArgsConstructor = BeanUtils.getNoArgsConstructor((TypeElement) impDeclaredType.asElement());
        if (noArgsConstructor == null || !noArgsConstructor.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "MapOrCollectionImpl must contains public no args constructor", variableElement);
        }
    }

    private boolean isMapOrCollection(VariableElement variableElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), mapTypeMirror)
                || AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), collectionTypeMirror);
    }

    private TypeMirror getFieldImplType(VariableElement variableElement) {
        final AnnotationMirror impAnnotationMirror = AutoUtils
                .findFirstAnnotationWithoutInheritance(typeUtils, variableElement, impDeclaredType)
                .orElse(null);

        if (impAnnotationMirror == null) {
            return null;
        }

        final AnnotationValue annotationValue = AutoUtils.getAnnotationValueNotDefault(impAnnotationMirror, "value");
        assert null != annotationValue;
        return AutoUtils.getAnnotationValueTypeMirror(annotationValue);
    }

    // ----------------------------------------------- 辅助类生成 -------------------------------------------

    private void generateSerializer(TypeElement typeElement) {
        if (isNumericalEnum(typeElement)) {
            genNumericalEnumSerializer(typeElement);
        } else if (isIndexableEntity(typeElement)) {
            genIndexableEntitySerializer(typeElement);
        } else {
            genBeanSerializer(typeElement);
        }
    }

    // ---------------------------------------------------------- javaBean代码生成 ---------------------------------------------------

    private void genBeanSerializer(TypeElement typeElement) {
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getSerializerClassName(typeElement));
        final CodeBlock.Builder wireTypeStaticCodeBlock = CodeBlock.builder();

        final TypeName instanceRawTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));
        final MethodSpec.Builder writeMethodBuilder = newWriteMethodBuilder(instanceRawTypeName);

        final MethodSpec.Builder readMethodBuilder = newReadMethodBuilder(instanceRawTypeName);
        readMethodBuilder.addStatement("$T instance = new $T()", instanceRawTypeName, instanceRawTypeName);

        // 必须包含超类字段
        final List<? extends Element> allFieldsAndMethodWithInherit = getAllFieldsAndMethodsWithInherit(typeElement);
        for (Element element : allFieldsAndMethodWithInherit) {
            // 非成员属性
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            // 该注解只有Field可以使用
            final VariableElement variableElement = (VariableElement) element;
            // 查找该字段上的注解
            if (!isSerializableField(variableElement)) {
                continue;
            }

            TypeName fieldRawTypeName = ParameterizedTypeName.get(typeUtils.erasure(variableElement.asType()));
            if (fieldRawTypeName.isPrimitive()) {
                fieldRawTypeName = fieldRawTypeName.box();
            }

            final String wireTypeFieldName = "wireType_" + variableElement.getSimpleName().toString();

            typeBuilder.addField(byte.class, wireTypeFieldName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
            wireTypeStaticCodeBlock.addStatement("$L = $T.$L($T.class)", wireTypeFieldName, wireTypeTypeName, FINDTYPE_METHOD_NAME, fieldRawTypeName);

            final String getterName = BeanUtils.getterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));
            final String setterName = BeanUtils.setterMethodName(variableElement.getSimpleName().toString(), BeanUtils.isBoolean(fieldRawTypeName));

            writeMethodBuilder.addStatement("outputStream.$L($L, instance.$L())", WRITE_FIELD_METHOD_NAME, wireTypeFieldName, getterName);
            readMethodBuilder.addStatement("instance.$L(inputStream.$L($L))", setterName, READ_FIELD_METHOD_NAME, wireTypeFieldName);
        }

        readMethodBuilder.addStatement("return instance");

        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addSuperinterface(TypeName.get(typeUtils.getDeclaredType(serializerTypeElement, typeUtils.erasure(typeElement.asType()))))
                .addStaticBlock(wireTypeStaticCodeBlock.build())
                .addMethod(writeMethodBuilder.build())
                .addMethod(readMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private MethodSpec.Builder newWriteMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(WRITE_OBJECT_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .addParameter(instanceRawTypeName, "instance")
                .addParameter(TypeName.get(outputStreamTypeElement.asType()), "outputStream");
    }

    private MethodSpec.Builder newReadMethodBuilder(TypeName instanceRawTypeName) {
        return MethodSpec.methodBuilder(READ_OBJECT_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addException(IOException.class)
                .returns(instanceRawTypeName)
                .addParameter(TypeName.get(inputStreamTypeElement.asType()), "inputStream");
    }

    /**
     * 获取class对应的序列化工具类的类名
     */
    private String getSerializerClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "Serializer";
    }

    // ---------------------------------------------------------- 枚举序列化生成 ---------------------------------------------------

    private void genNumericalEnumSerializer(TypeElement typeElement) {
        final TypeName instanceRawTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));

        // 写入number即可 outputStream.writeObject(WireType.INT, instance.getNumber())
        final MethodSpec.Builder writeMethodBuilder = newWriteMethodBuilder(instanceRawTypeName);
        writeMethodBuilder.addStatement("outputStream.$L($T.$L, instance.$L())",
                WRITE_FIELD_METHOD_NAME, wireTypeTypeName, WIRE_TYPE_INT, GET_NUMBER_METHOD_NAME);

        // 读取number即可 return A.forNumber(inputStream.readObject(WireType.INT))
        final MethodSpec.Builder readMethodBuilder = newReadMethodBuilder(instanceRawTypeName);
        readMethodBuilder.addStatement("return $T.$L(inputStream.$L($T.$L))",
                instanceRawTypeName, FOR_NUMBER_METHOD_NAME, READ_FIELD_METHOD_NAME, wireTypeTypeName, WIRE_TYPE_INT);

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getSerializerClassName(typeElement));
        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addSuperinterface(TypeName.get(typeUtils.getDeclaredType(serializerTypeElement, typeUtils.erasure(typeElement.asType()))))
                .addMethod(writeMethodBuilder.build())
                .addMethod(readMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }


    private void genIndexableEntitySerializer(TypeElement typeElement) {

    }
}
