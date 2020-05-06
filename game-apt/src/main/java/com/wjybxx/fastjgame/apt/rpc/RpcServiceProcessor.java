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

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.wjybxx.fastjgame.apt.core.MyAbstractProcessor;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 由于类文件结构比较稳定，API都是基于访问者模式的，完全不能直接获取数据，难受的一比。
 * <p>
 * 注意：使用class对象有限制，只可以使用JDK自带的class 和 该jar包（该项目）中所有的class (包括依赖的jar包)
 * <p>
 * 不在本包中的类，只能使用{@link Element}) 和 {@link javax.lang.model.type.TypeMirror}。
 * 因为需要使用的类可能还没编译，也就不存在对应的class文件，加载不到。
 * 这也是注解处理器必须要打成jar包的原因。不然注解处理器可能都还未被编译。。。。
 * <p>
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class RpcServiceProcessor extends MyAbstractProcessor {

    private static final String RPC_SERVICE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.RpcService";
    private static final String RPC_METHOD_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.RpcMethod";

    static final String LAZY_SERIALIZABLE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.LazySerializable";
    static final String PRE_DESERIALIZE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.PreDeserializable";

    private static final String METHOD_SPEC_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.RpcMethodSpec";
    private static final String DEFAULT_METHOD_SPEC_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.DefaultRpcMethodSpec";

    private static final String METHOD_REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.RpcMethodProxyRegistry";

    private static final String CONTEXT_CANONICAL_NAME = "com.wjybxx.fastjgame.net.rpc.RpcProcessContext";
    private static final String FUTURE_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.concurrent.ListenableFuture";
    private static final String FUTURE_UTILS_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.concurrent.FutureUtils";

    private static final String SERVICE_ID_METHOD_NAME = "serviceId";
    private static final String METHOD_ID_METHOD_NAME = "methodId";

    WildcardType wildcardType;

    private DeclaredType contextDeclaredType;
    private DeclaredType futureDeclaredType;
    TypeName futureUtilsTypeName;

    TypeElement methodSpecElement;
    TypeName defaultMethodSpecRawTypeName;

    DeclaredType lazySerializableDeclaredType;
    DeclaredType preDeserializeDeclaredType;

    private TypeElement rpcServiceElement;
    private DeclaredType rpcServiceDeclaredType;
    private DeclaredType rpcMethodDeclaredType;

    ClassName methodRegistryTypeName;

    private TypeMirror mapTypeMirror;
    private TypeMirror linkedHashMapTypeMirror;

    private TypeMirror collectionTypeMirror;
    private TypeMirror arrayListTypeMirror;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RPC_SERVICE_CANONICAL_NAME);
    }

    @Override
    protected void ensureInited() {
        if (rpcServiceElement != null) {
            // 已初始化
            return;
        }
        wildcardType = typeUtils.getWildcardType(null, null);

        rpcServiceElement = elementUtils.getTypeElement(RPC_SERVICE_CANONICAL_NAME);
        rpcServiceDeclaredType = typeUtils.getDeclaredType(rpcServiceElement);
        rpcMethodDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(RPC_METHOD_CANONICAL_NAME));

        methodRegistryTypeName = ClassName.get(elementUtils.getTypeElement(METHOD_REGISTRY_CANONICAL_NAME));

        contextDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(CONTEXT_CANONICAL_NAME));
        futureDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(FUTURE_CANONICAL_NAME));
        futureUtilsTypeName = TypeName.get(elementUtils.getTypeElement(FUTURE_UTILS_CANONICAL_NAME).asType());

        methodSpecElement = elementUtils.getTypeElement(METHOD_SPEC_CANONICAL_NAME);
        defaultMethodSpecRawTypeName = TypeName.get(typeUtils.getDeclaredType(elementUtils.getTypeElement(DEFAULT_METHOD_SPEC_CANONICAL_NAME)));

        lazySerializableDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(LAZY_SERIALIZABLE_CANONICAL_NAME));
        preDeserializeDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PRE_DESERIALIZE_CANONICAL_NAME));

        mapTypeMirror = elementUtils.getTypeElement(Map.class.getCanonicalName()).asType();
        linkedHashMapTypeMirror = elementUtils.getTypeElement(LinkedHashMap.class.getCanonicalName()).asType();

        collectionTypeMirror = elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType();
        arrayListTypeMirror = elementUtils.getTypeElement(ArrayList.class.getCanonicalName()).asType();
    }

    @Override
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 该注解只有类才可以使用
        @SuppressWarnings("unchecked")
        Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(rpcServiceElement);
        for (TypeElement typeElement : typeElementSet) {
            try {
                checkBase(typeElement);

                genProxyClass(typeElement);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, AutoUtils.getStackTrace(e), typeElement);
            }
        }
        return true;
    }

    private void checkBase(TypeElement typeElement) {
        final Optional<? extends AnnotationMirror> serviceAnnotationOption = AutoUtils.findAnnotation(typeUtils, typeElement, rpcServiceDeclaredType);
        assert serviceAnnotationOption.isPresent();
        // 基本类型会被包装，Object不能直接转short
        final Short serviceId = AutoUtils.getAnnotationValueValue(serviceAnnotationOption.get(), SERVICE_ID_METHOD_NAME);
        assert null != serviceId;
        if (serviceId <= 0) {
            // serviceId非法
            messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " must greater than 0!", typeElement);
            return;
        }

        checkMethods(typeElement);
    }

    private void checkMethods(TypeElement typeElement) {
        final Set<Short> methodIdSet = new HashSet<>();
        for (final Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }

            final ExecutableElement method = (ExecutableElement) element;
            final Optional<? extends AnnotationMirror> rpcMethodAnnotation = AutoUtils.findAnnotation(typeUtils, method, rpcMethodDeclaredType);
            if (rpcMethodAnnotation.isEmpty()) {
                // 不是rpc方法，跳过
                continue;
            }

            if (method.getModifiers().contains(Modifier.STATIC)) {
                // 不可以是静态的
                messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod method can't be static！", method);
                continue;
            }

            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                // 访问权限不可以是private - 由于生成的类和该类属于同一个包，因此不必public，只要不是private即可
                messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod method can't be private！", method);
                continue;
            }

            // 方法id，基本类型会被封装为包装类型，Object并不能直接转换到基本类型
            final Short methodId = AutoUtils.getAnnotationValueValue(rpcMethodAnnotation.get(), METHOD_ID_METHOD_NAME);
            assert null != methodId;
            if (methodId < 0 || methodId > 9999) {
                // 方法id非法
                messager.printMessage(Diagnostic.Kind.ERROR, " methodId " + methodId + " must between [0,9999]!", method);
                continue;
            }

            if (!methodIdSet.add(methodId)) {
                // 同一个类中的方法id不可以重复 - 它保证了本模块中方法id不会重复
                messager.printMessage(Diagnostic.Kind.ERROR, " methodId " + methodId + " is duplicate!", method);
            }

            checkParameters(method);
        }
    }

    private void checkParameters(ExecutableElement method) {
        for (VariableElement variableElement : method.getParameters()) {
            if (isMap(variableElement)) {
                checkMap(variableElement);
                continue;
            }

            if (isCollection(variableElement)) {
                checkCollection(variableElement);
            }
        }
    }

    private void checkMap(VariableElement variableElement) {
        if (!isAssignableFromLinkedHashMap(variableElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Unsupported map type, Map parameter only support LinkedHashMap's parent, " +
                            "\nrefer to the annotation '@Impl' for more help",
                    variableElement);
        }
    }

    private void checkCollection(VariableElement variableElement) {
        if (!isAssignableFormArrayList(variableElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Unsupported collection type, Collection parameter only support ArrayList's parent, " +
                            "\nrefer to the annotation '@Impl' for more help",
                    variableElement);
        }
    }

    private void genProxyClass(TypeElement typeElement) {
        final Short serviceId = getServiceId(typeElement);

        // rpcMethods.size() == 0 也必须重新生成文件(必须刷新文件)
        final List<ExecutableElement> rpcMethods = collectRpcMethods(typeElement);

        // 客户端代理
        genClientProxy(typeElement, serviceId, rpcMethods);

        // 服务器代理
        genServerProxy(typeElement, serviceId, rpcMethods);
    }

    private Short getServiceId(TypeElement typeElement) {
        // 基本类型会被包装，Object不能直接转short
        return (Short) AutoUtils.findAnnotation(typeUtils, typeElement, rpcServiceDeclaredType)
                .map(annotationMirror -> AutoUtils.getAnnotationValueValue(annotationMirror, SERVICE_ID_METHOD_NAME))
                .get();
    }

    private List<ExecutableElement> collectRpcMethods(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(method -> AutoUtils.findAnnotation(typeUtils, method, rpcMethodDeclaredType).isPresent())
                .map(e -> (ExecutableElement) e)
                .collect(Collectors.toList());
    }

    Short getMethodId(ExecutableElement method) {
        // 基本类型会被包装，Object不能直接转short
        return (Short) AutoUtils.findAnnotation(typeUtils, method, rpcMethodDeclaredType)
                .map(annotationMirror -> AutoUtils.getAnnotationValueValue(annotationMirror, METHOD_ID_METHOD_NAME))
                .get();
    }

    /**
     * 为客户端生成代理文件
     * XXXRpcProxy
     */
    private void genClientProxy(final TypeElement typeElement, final Short serviceId, final List<ExecutableElement> rpcMethods) {
        new RpcProxyGenerator(this, typeElement, serviceId, rpcMethods)
                .execute();
    }

    /**
     * 为服务器生成代理文件
     * XXXRpcRegister
     */
    private void genServerProxy(TypeElement typeElement, Short serviceId, List<ExecutableElement> rpcMethods) {
        new RpcRegisterGenerator(this, typeElement, serviceId, rpcMethods)
                .execute();
    }

    private boolean isMap(VariableElement variableElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), mapTypeMirror);
    }

    private boolean isAssignableFromLinkedHashMap(VariableElement variableElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, linkedHashMapTypeMirror, variableElement.asType());
    }

    private boolean isCollection(VariableElement variableElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), collectionTypeMirror);
    }

    private boolean isAssignableFormArrayList(VariableElement variableElement) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, arrayListTypeMirror, variableElement.asType());
    }

    boolean isContext(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), contextDeclaredType);
    }

    boolean isFuture(TypeMirror typeMirror) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, typeMirror, futureDeclaredType);
    }

}
