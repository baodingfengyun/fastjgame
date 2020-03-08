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

package com.wjybxx.fastjgame.apt.rpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.wjybxx.fastjgame.apt.core.AbstractGenerator;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 为客户端生成代理文件 XXXRpcProxy
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/22
 */
class RpcProxyGenerator extends AbstractGenerator<RpcServiceProcessor> {

    private final short serviceId;
    private final List<ExecutableElement> rpcMethods;

    RpcProxyGenerator(RpcServiceProcessor processor, TypeElement typeElement, short serviceId, List<ExecutableElement> rpcMethods) {
        super(processor, typeElement);
        this.serviceId = serviceId;
        this.rpcMethods = rpcMethods;
    }

    @Override
    public void execute() {
        // 代理类不可以继承
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getClientProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(processorInfoAnnotation);

        // 生成代理方法
        for (final ExecutableElement method : rpcMethods) {
            typeBuilder.addMethod(genClientMethodProxy(method));
        }

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private static String getClientProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "RpcProxy";
    }

    /**
     * 为客户端生成代理方法
     * <pre>{@code
     * 		public static RpcBuilder<String> method1(int id, String param) {
     * 			List<Object> methodParams = new ArrayList<>(2);
     * 			methodParams.add(id);
     * 			methodParams.add(param);
     * 			return new DefaultRpcMethodSpec<>(1, 2, methodParams, 0, 0);
     *        }
     * }
     * </pre>
     */
    private MethodSpec genClientMethodProxy(ExecutableElement method) {
        // 工具方法 public static RpcBuilder<V>
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        // 拷贝泛型参数
        AutoUtils.copyTypeVariables(builder, method);

        // 解析方法参数 -- 提炼方法是为了减少当前方法的长度
        final ParseResult parseResult = parseParameters(method);
        final List<ParameterSpec> realParameters = parseResult.realParameters;

        // 添加返回类型 - 带泛型
        final TypeMirror returnType = getNonPrimitiveReturnType(method);
        final DeclaredType realReturnType = typeUtils.getDeclaredType(processor.methodSpecElement, returnType);
        builder.returns(ClassName.get(realReturnType));

        // 拷贝参数列表
        builder.addParameters(realParameters);
        // 是否是变长参数类型
        builder.varargs(method.isVarArgs());

        if (realParameters.size() == 0) {
            // 无参时，使用 Collections.emptyList();
            builder.addStatement("return new $T<>((short)$L, (short)$L, $T.emptyList(), $L, $L)",
                    processor.defaultMethodSpecRawTypeName,
                    serviceId, processor.getMethodId(method),
                    Collections.class,
                    parseResult.lazyIndexes, parseResult.preIndexes);
        } else {
            builder.addStatement("$T<Object> methodParams = new $T<>($L)", ArrayList.class,
                    ArrayList.class, realParameters.size());

            for (ParameterSpec parameterSpec : realParameters) {
                builder.addStatement("methodParams.add($L)", parameterSpec.name);
            }
            builder.addStatement("return new $T<>((short)$L, (short)$L, methodParams, $L, $L)",
                    processor.defaultMethodSpecRawTypeName,
                    serviceId, processor.getMethodId(method),
                    parseResult.lazyIndexes, parseResult.preIndexes);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private ParseResult parseParameters(ExecutableElement method) {
        // 原始参数列表
        final List<VariableElement> originParameters = (List<VariableElement>) method.getParameters();
        // 真实参数列表
        final List<ParameterSpec> realParameters = new ArrayList<>(originParameters.size());

        // 需要延迟初始化的参数所在的位置
        int lazyIndexes = 0;
        // 需要提前反序列化的参数所在的位置
        int preIndexes = 0;

        // 筛选参数
        for (VariableElement variableElement : originParameters) {
            // session和promise需要从参数列表删除
            if (processor.isPromise(variableElement) || processor.isSession(variableElement)) {
                continue;
            }

            if (isLazySerializeParameter(variableElement)) {
                // 延迟序列化的参数(对方可以发送任意数据) - 要替换为Object
                lazyIndexes |= 1 << realParameters.size();
                realParameters.add(lazySerializableParameterProxy(variableElement));
            } else if (isPreDeserializeParameter(variableElement)) {
                // 查看是否需要提前反序列化(要求对方发来的是byte[]) - 要替换为byte[]
                preIndexes |= 1 << realParameters.size();
                realParameters.add(preDeserializeParameterProxy(variableElement));
            } else {
                // 普通参数
                realParameters.add(ParameterSpec.get(variableElement));
            }
        }
        return new ParseResult(realParameters, lazyIndexes, preIndexes);
    }

    private static ParameterSpec lazySerializableParameterProxy(VariableElement variableElement) {
        return ParameterSpec.builder(Object.class, variableElement.getSimpleName().toString()).build();
    }

    private static ParameterSpec preDeserializeParameterProxy(VariableElement variableElement) {
        return ParameterSpec.builder(byte[].class, variableElement.getSimpleName().toString()).build();
    }

    /**
     * 是否可延迟序列化的参数？（无论对方发送什么，我接收为一个byte[]）
     * 1. 必须带有{@link RpcServiceProcessor#LAZY_SERIALIZABLE_CANONICAL_NAME}注解
     * 2. 必须是字节数组
     */
    private boolean isLazySerializeParameter(VariableElement variableElement) {
        if (!isAnnotationPresent(variableElement, processor.lazySerializableDeclaredType)) {
            return false;
        }
        if (isByteArrayVariable(variableElement.asType())) {
            return true;
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "Annotation LazySerializable only support byte[]", variableElement);
            return false;
        }
    }

    private boolean isAnnotationPresent(Element element, DeclaredType annationMirror) {
        return AutoUtils.isAnnotationPresent(typeUtils, element, annationMirror);
    }

    private boolean isByteArrayVariable(TypeMirror typeMirror) {
        return AutoUtils.isTargetPrimitiveArrayType(typeMirror, TypeKind.BYTE);
    }

    /**
     * 是否是需要提前反序列化的参数？（无论对方发送什么，我接收为一个非byte[]对象）
     * 1. 必须带有{@link RpcServiceProcessor#PRE_DESERIALIZE_CANONICAL_NAME}注解
     * 2. 不能是字节数组
     */
    private boolean isPreDeserializeParameter(VariableElement variableElement) {
        if (!isAnnotationPresent(variableElement, processor.preDeserializeDeclaredType)) {
            return false;
        }
        if (isByteArrayVariable(variableElement.asType())) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Annotation PreDeserializable doesn't support byte[]", variableElement);
            return false;
        }
        return true;
    }

    private static class ParseResult {
        // 除去特殊参数余下的参数
        private final List<ParameterSpec> realParameters;
        // 需要延迟初始化的位置
        private final int lazyIndexes;
        // 需要提前反序列化的位置

        private final int preIndexes;

        ParseResult(List<ParameterSpec> realParameters, int lazyIndexes, int preIndexes) {
            this.realParameters = realParameters;
            this.lazyIndexes = lazyIndexes;
            this.preIndexes = preIndexes;
        }

    }

    private TypeMirror getNonPrimitiveReturnType(ExecutableElement method) {
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            // 方法有返回值
            if (method.getReturnType().getKind().isPrimitive()) {
                // 可能是基础类型
                return typeUtils.boxedClass((PrimitiveType) method.getReturnType()).asType();
            } else {
                return method.getReturnType();
            }
        }

        // 判断是否有Promise，如果没有则返回通配符（其实Void也行）
        return method.getParameters().stream()
                .filter(processor::isPromise)
                .findFirst()
                .map(this::getPromiseTypeArgument)
                .orElse(processor.wildcardType);
    }

    private TypeMirror getPromiseTypeArgument(final VariableElement variableElement) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<TypeMirror, Void>() {
            @Override
            public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
                if (t.getTypeArguments().size() > 0) {
                    // 第一个参数就是返回值类型，这里的泛型也可能是'?'
                    return t.getTypeArguments().get(0);
                } else {
                    // 声明类型木有泛型参数，返回Object类型，并打印一个错误
                    // 2019年8月23日21:13:35 改为编译错误
                    messager.printMessage(Diagnostic.Kind.ERROR, "Promise missing type parameter!", variableElement);
                    return elementUtils.getTypeElement(Object.class.getCanonicalName()).asType();
                }
            }
        }, null);
    }
}
