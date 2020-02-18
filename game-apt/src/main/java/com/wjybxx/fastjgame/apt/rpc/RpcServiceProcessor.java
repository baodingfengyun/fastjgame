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
import com.squareup.javapoet.*;
import com.wjybxx.fastjgame.apt.core.MyAbstractProcessor;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;
import com.wjybxx.fastjgame.apt.utils.BeanUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * 由于类文件结构比较稳定，API都是基于访问者模式的，完全不能直接获取数据，难受的一比。
 * <p>
 * 注意：使用class对象有限制，只可以使用JDK自带的class 和 该jar包（该项目）中所有的class (包括依赖的jar包)
 * <p>
 * 不在本包中的类，只能使用{@link Element}) 和 {@link javax.lang.model.type.TypeMirror}。
 * 因为需要使用的类可能还没编译，也就不存在对应的class文件，加载不到。
 * 这也是注解处理器必须要打成jar包的原因。不然注解处理器可能都还未被编译。。。。
 * <p>
 * {@link RoundEnvironment} 运行环境，编译器环境。（他是一个独立的编译器，无法直接调试，需要远程调试）
 * {@link Types} 类型工具类
 * {@link Filer} 文件读写util
 * {@link Elements} Element工具类
 * <p>
 * 遇见的坑，先记一笔：
 * 1. 在注解中使用另一个类时，由于引用的类可能尚未编译，因此安全的方式是使用
 * {@link AnnotationValue#accept(AnnotationValueVisitor, Object)}获取引用的类的{@link TypeMirror}。
 * 在不熟悉这个api时，谷歌/百度得到的基本都是通过捕获异常获取到未编译的类的{@link TypeMirror}。
 * <p>
 * 2. {@link Types#isSameType(TypeMirror, TypeMirror)}、{@link Types#isSubtype(TypeMirror, TypeMirror)} 、
 * {@link Types#isAssignable(TypeMirror, TypeMirror)} api用于判断类之间的关系。
 * <p>
 * 3. {@code typeUtils.isSameType(RpcResponseChannel<String>, RpcResponseChannel)  false}
 * {@code typeUtils.isAssignable(RpcResponseChannel<String>, RpcResponseChannel)  true}
 * {@code typeUtils.isAssignable(RpcResponseChannel<String>, RpcResponseChannel<Integer>)  false}
 * {@code typeUtils.isSubType(RpcResponseChannel<String>, RpcResponseChannel)  true}
 * {@code typeUtils.isSubType(RpcResponseChannel<String>, RpcResponseChannel<Integer>)  false}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class RpcServiceProcessor extends MyAbstractProcessor {

    private static final String METHOD_HANDLE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.misc.RpcMethodHandle";
    private static final String DEFAULT_METHOD_HANDLE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.misc.DefaultRpcMethodHandle";

    private static final String RPC_SERVICE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.RpcService";
    private static final String RPC_METHOD_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.RpcMethod";

    private static final String LAZY_SERIALIZABLE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.LazySerializable";
    private static final String PRE_DESERIALIZE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.PreDeserializable";

    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.net.misc.RpcFunctionRegistry";
    private static final String SESSION_CANONICAL_NAME = "com.wjybxx.fastjgame.net.session.Session";
    private static final String CHANNEL_CANONICAL_NAME = "com.wjybxx.fastjgame.net.common.RpcResponseChannel";

    private static final String EXCEPTION_UTILS_CANONICAL_NAME = "org.apache.commons.lang3.exception.ExceptionUtils";

    private static final String SERVICE_ID_METHOD_NAME = "serviceId";
    private static final String METHOD_ID_METHOD_NAME = "methodId";

    private static final String session = "session";
    private static final String methodParams = "methodParams";
    private static final String responseChannel = "responseChannel";

    private static final String registry = "registry";
    private static final String instance = "instance";

    /**
     * 所有的serviceId集合，判断重复。
     * 编译是分模块编译的，每一个模块都是一个新的processor，因此只能检测当前模块的重复。
     * 能在编译期间发现错误就在编译期间发现错误。
     */
    private final Set<Short> serviceIdSet = new HashSet<>(64);

    private WildcardType wildcardType;

    private DeclaredType responseChannelDeclaredType;
    private DeclaredType sessionDeclaredType;

    private TypeElement methodHandleElement;
    private TypeName defaultMethodHandleRawTypeName;

    private DeclaredType lazySerializableDeclaredType;
    private DeclaredType preDeserializeDeclaredType;

    private TypeElement rpcServiceElement;
    private DeclaredType rpcServiceDeclaredType;
    private DeclaredType rpcMethodDeclaredType;

    private ClassName registryTypeName;
    private ClassName exceptionUtilsTypeName;

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

        registryTypeName = ClassName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME));
        sessionDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SESSION_CANONICAL_NAME));
        responseChannelDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(CHANNEL_CANONICAL_NAME));

        exceptionUtilsTypeName = ClassName.get(elementUtils.getTypeElement(EXCEPTION_UTILS_CANONICAL_NAME));

        methodHandleElement = elementUtils.getTypeElement(METHOD_HANDLE_CANONICAL_NAME);
        defaultMethodHandleRawTypeName = TypeName.get(typeUtils.getDeclaredType(elementUtils.getTypeElement(DEFAULT_METHOD_HANDLE_CANONICAL_NAME)));

        lazySerializableDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(LAZY_SERIALIZABLE_CANONICAL_NAME));
        preDeserializeDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PRE_DESERIALIZE_CANONICAL_NAME));
    }


    @Override
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 该注解只有类才可以使用
        @SuppressWarnings("unchecked")
        Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(rpcServiceElement);
        for (TypeElement typeElement : typeElementSet) {
            try {
                genProxyClass(typeElement);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), typeElement);
            }
        }
        return true;
    }

    private void genProxyClass(TypeElement typeElement) {
        final Optional<? extends AnnotationMirror> serviceAnnotationOption = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, typeElement, rpcServiceDeclaredType);
        assert serviceAnnotationOption.isPresent();
        // 基本类型会被包装，Object不能直接转short
        final Short serviceId = AutoUtils.getAnnotationValueValueNotDefault(serviceAnnotationOption.get(), SERVICE_ID_METHOD_NAME);
        assert null != serviceId;
        if (serviceId <= 0) {
            // serviceId非法
            messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " must greater than 0!", typeElement);
            return;
        }

        if (!serviceIdSet.add(serviceId)) {
            // serviceId重复
            messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " is duplicate!", typeElement);
            return;
        }

        // rpcMethods.size() == 0 也必须重新生成文件
        final List<ExecutableElement> rpcMethods = collectRpcMethods(typeElement);

        // 客户端代理
        genClientProxy(typeElement, serviceId, rpcMethods);

        // 服务器代理
        genServerProxy(typeElement, serviceId, rpcMethods);
    }

    /**
     * 搜集rpc方法
     *
     * @param typeElement rpcService类
     * @return 所有合法rpc方法
     */
    private List<ExecutableElement> collectRpcMethods(TypeElement typeElement) {
        final List<ExecutableElement> result = new ArrayList<>();
        final Set<Short> methodIdSet = new HashSet<>();

        for (final Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }

            final ExecutableElement method = (ExecutableElement) element;
            final Optional<? extends AnnotationMirror> rpcMethodAnnotation = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, rpcMethodDeclaredType);
            if (!rpcMethodAnnotation.isPresent()) {
                // 不是rpc方法，跳过
                continue;
            }

            if (method.isVarArgs()) {
                // 不支持变长参数
                messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod not support varArgs!", method);
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
            final Short methodId = AutoUtils.getAnnotationValueValueNotDefault(rpcMethodAnnotation.get(), METHOD_ID_METHOD_NAME);
            assert null != methodId;
            if (methodId < 0 || methodId > 9999) {
                // 方法id非法
                messager.printMessage(Diagnostic.Kind.ERROR, " methodId " + methodId + " must between [0,9999]!", method);
                continue;
            }

            if (!methodIdSet.add(methodId)) {
                // 同一个类中的方法id不可以重复 - 它保证了本模块中方法id不会重复
                messager.printMessage(Diagnostic.Kind.ERROR, " methodId " + methodId + " is duplicate!", method);
                continue;
            }

            result.add(method);
        }

        return result;
    }

    private Short getMethodId(ExecutableElement method) {
        final Optional<? extends AnnotationMirror> annotationMirror = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, rpcMethodDeclaredType);
        assert annotationMirror.isPresent();
        // 方法id，基本类型会被封装为包装类型，Object并不能直接转换到基本类型
        return AutoUtils.getAnnotationValueValueNotDefault(annotationMirror.get(), METHOD_ID_METHOD_NAME);
    }

    /**
     * 计算方法的唯一键
     *
     * @param serviceId 服务id
     * @param methodId  方法id
     * @return methodKey
     */
    private static int getMethodKey(int serviceId, Short methodId) {
        // 乘10000有更好的可读性
        return serviceId * 10000 + methodId;
    }

    // ----------------------------------------- 为客户端生成代理方法 -------------------------------------------

    /**
     * 为客户端生成代理文件
     * XXXRpcProxy
     */
    private void genClientProxy(final TypeElement typeElement, final Short serviceId, final List<ExecutableElement> rpcMethods) {
        // 代理类不可以继承
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getClientProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(processorInfoAnnotation);

        // 生成代理方法
        for (final ExecutableElement method : rpcMethods) {
            final int methodKey = getMethodKey(serviceId, getMethodId(method));
            typeBuilder.addMethod(genClientMethodProxy(methodKey, method));
        }

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private String getClientProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "RpcProxy";
    }

    /**
     * 为客户端生成代理方法
     * <pre>{@code
     * 		public static RpcBuilder<String> method1(int id, String param) {
     * 			List<Object> methodParams = new ArrayList<>(2);
     * 			methodParams.add(id);
     * 			methodParams.add(param);
     * 			return new RpcBuilder<>(1, methodParams, true);
     *        }
     * }
     * </pre>
     */
    private MethodSpec genClientMethodProxy(int methodKey, ExecutableElement method) {
        // 工具方法 public static RpcBuilder<V>
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        // 拷贝泛型参数
        AutoUtils.copyTypeVariables(builder, method);

        // 解析方法参数 -- 提炼方法是为了减少当前方法的长度
        final ParseResult parseResult = parseParameters(method);
        final List<ParameterSpec> realParameters = parseResult.realParameters;

        // 添加返回类型-带泛型
        final DeclaredType realReturnType = typeUtils.getDeclaredType(methodHandleElement, parseResult.callReturnType);
        builder.returns(ClassName.get(realReturnType));

        // 拷贝参数列表
        builder.addParameters(realParameters);

        // 搜集参数代码块（一个参数时不能使用singleTonList了，因为可能要修改内容）
        if (realParameters.size() == 0) {
            // 无参时，使用 Collections.emptyList();
            builder.addStatement("return new $T<>($L, $T.emptyList(), $L, $L)", defaultMethodHandleRawTypeName, methodKey, Collections.class,
                    parseResult.lazyIndexes, parseResult.preIndexes);
        } else {
            builder.addStatement("$T<Object> $L = new $T<>($L)", ArrayList.class, methodParams, ArrayList.class, realParameters.size());
            for (ParameterSpec parameterSpec : realParameters) {
                builder.addStatement("$L.add($L)", methodParams, parameterSpec.name);
            }
            builder.addStatement("return new $T<>($L, $L, $L, $L)", defaultMethodHandleRawTypeName, methodKey, methodParams,
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

        // 返回值类型
        TypeMirror callReturnType = null;

        // 需要延迟初始化的参数所在的位置
        int lazyIndexes = 0;
        // 需要提前反序列化的参数所在的位置
        int preIndexes = 0;

        // 筛选参数
        for (VariableElement variableElement : originParameters) {
            // rpcResponseChannel需要从参数列表删除，并捕获泛型类型
            if (callReturnType == null && isResponseChannel(variableElement)) {
                callReturnType = getResponseChannelTypeArgument(variableElement);
                continue;
            }
            // session需要从参数列表删除
            if (isSession(variableElement)) {
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
        if (null == callReturnType) {
            // 参数列表中不存在responseChannel
            callReturnType = method.getReturnType();
        } else {
            // 如果参数列表中存在responseChannel，那么返回值必须是void
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType is not void, but parameters contains responseChannel!", method);
            }
        }
        if (callReturnType.getKind() == TypeKind.VOID) {
            // 返回值类型为void，rpcBuilder泛型参数为泛型通配符
            callReturnType = wildcardType;
        } else {
            // 基本类型转包装类型
            if (callReturnType.getKind().isPrimitive()) {
                callReturnType = typeUtils.boxedClass((PrimitiveType) callReturnType).asType();
            }
        }
        return new ParseResult(realParameters, lazyIndexes, preIndexes, callReturnType);
    }

    private static ParameterSpec lazySerializableParameterProxy(VariableElement variableElement) {
        return ParameterSpec.builder(Object.class, variableElement.getSimpleName().toString()).build();
    }

    private static ParameterSpec preDeserializeParameterProxy(VariableElement variableElement) {
        return ParameterSpec.builder(byte[].class, variableElement.getSimpleName().toString()).build();
    }

    private boolean isResponseChannel(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), responseChannelDeclaredType);
    }

    private boolean isSession(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), sessionDeclaredType);
    }

    /**
     * 是否可延迟序列化的参数？
     * 1. 必须带有{@link #LAZY_SERIALIZABLE_CANONICAL_NAME}注解
     * 2. 必须是字节数组
     */
    private boolean isLazySerializeParameter(VariableElement variableElement) {
        if (!AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, variableElement, lazySerializableDeclaredType).isPresent()) {
            return false;
        }
        if (AutoUtils.isTargetPrimitiveArrayType(variableElement, TypeKind.BYTE)) {
            return true;
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation LazySerializable only support byte[]", variableElement);
            return false;
        }
    }

    /**
     * 是否是需要提前反序列化的参数？
     * 1. 必须带有{@link #PRE_DESERIALIZE_CANONICAL_NAME}注解
     * 2. 不能是字节数组
     */
    private boolean isPreDeserializeParameter(VariableElement variableElement) {
        if (!AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, variableElement, preDeserializeDeclaredType).isPresent()) {
            return false;
        }
        if (AutoUtils.isTargetPrimitiveArrayType(variableElement, TypeKind.BYTE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation PreDeserializable doesn't support byte[]", variableElement);
            return false;
        }
        return true;
    }

    private TypeMirror getResponseChannelTypeArgument(final VariableElement variableElement) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<TypeMirror, Void>() {
            @Override
            public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
                if (t.getTypeArguments().size() > 0) {
                    // 第一个参数就是返回值类型，这里的泛型也可能是'?'
                    return t.getTypeArguments().get(0);
                } else {
                    // 声明类型木有泛型参数，返回Object类型，并打印一个错误
                    // 2019年8月23日21:13:35 改为编译错误
                    messager.printMessage(Diagnostic.Kind.ERROR, "RpcResponseChannel missing type parameter!", variableElement);
                    return elementUtils.getTypeElement(Object.class.getCanonicalName()).asType();
                }
            }
        }, null);
    }

    // --------------------------------------------- 为服务端生成代理方法 ---------------------------------------

    /**
     * 为服务器生成代理文件
     * XXXRpcRegister
     */
    private void genServerProxy(TypeElement typeElement, Short serviceId, List<ExecutableElement> rpcMethods) {
        final List<MethodSpec> serverMethodProxyList = new ArrayList<>(rpcMethods.size());
        // 生成代理方法
        for (final ExecutableElement method : rpcMethods) {
            final int methodKey = getMethodKey(serviceId, getMethodId(method));
            serverMethodProxyList.add(genServerMethodProxy(typeElement, methodKey, method));
        }

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getServerProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation);

        typeBuilder.addMethods(serverMethodProxyList);

        // 生成注册方法
        typeBuilder.addMethod(genRegisterMethod(typeElement, serverMethodProxyList));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private String getServerProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "RpcRegister";
    }

    /**
     * 生成注册方法
     * {@code
     * public static void register(RpcFunctionRegistry registry, T instance) {
     * registerGetMethod1(registry, instance);
     * registerGetMethod2(registry, instance);
     * }
     * }
     *
     * @param typeElement           类信息
     * @param serverProxyMethodList 被代理的服务器方法
     */
    private MethodSpec genRegisterMethod(TypeElement typeElement, List<MethodSpec> serverProxyMethodList) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(registryTypeName, registry)
                .addParameter(TypeName.get(typeElement.asType()), instance);

        // 添加调用
        for (MethodSpec method : serverProxyMethodList) {
            builder.addStatement("$L($L, $L)", method.name, registry, instance);
        }

        return builder.build();
    }

    /**
     * 为某个具体方法生成注册方法，方法分为两类
     * 1. 异步方法 -- 异步方法是指需要responseChannel的方法，由应用层代码告知远程执行结果。
     * 2. 同步方法 -- 同步方法是指不需要responseChannel的方法，由生成的代码告知远程执行结果。
     * <p>
     * 1. 异步方法，那么返回结果的职责就完全交给应用层
     * <pre>
     * {@code
     * 		private static void registerGetMethod1(RpcFunctionRegistry registry, T instance) {
     * 		    registry.register(10001, (session, methodParams, responseChannel) -> {
     * 		       instance.method1(methodParams.get(0), methodParams.get(1), responseChannel);
     *            });
     *        }
     * }
     * </pre>
     * 2. 同步方法，同步方法又分为：有结果和无结果类型，有结果的。
     * 2.1 有结果的，返回直接结果
     * <pre>
     * {@code
     * 		private static void registerGetMethod2(RpcFunctionRegistry registry, T instance) {
     * 		    registry.register(10002, (session, methodParams, responseChannel) -> {
     * 		       try {
     * 		     		V result = instance.method2(methodParams.get(0), methodParams.get(1));
     * 		     	    responseChannel.writeSuccess(result);
     *               } catch(Throwable cause){
     *                  responseChannel.writeFailure(cause)
     *                  ConcurrentUtils.rethrow(cause);
     *               }
     *            });
     *        }
     * }
     * </pre>
     * 2.2 无结果的，返回null，表示执行成功
     * <pre>
     * {@code
     * 		private static void registerGetMethod2(RpcFunctionRegistry registry, T instance) {
     * 		    registry.register(10002, (session, methodParams, responseChannel) -> {
     * 		       try {
     * 		       		instance.method1(methodParams.get(0), methodParams.get(1), responseChannel);
     * 		       	    responseChannel.writeSuccess(null);
     *               } catch(Throwable cause) {
     *                   responseChannel.writeFailure(cause)
     *                   ConcurrentUtils.rethrow(cause);
     *               }
     *            });
     *        }
     * }
     * </pre>
     */
    private MethodSpec genServerMethodProxy(TypeElement typeElement, int methodKey, ExecutableElement method) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(getServerProxyMethodName(methodKey, method))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(registryTypeName, registry)
                .addParameter(TypeName.get(typeElement.asType()), instance);

        // 双方都必须拷贝泛型变量
        AutoUtils.copyTypeVariables(builder, method);

        builder.addCode("$L.register($L, ($L, $L, $L) -> {\n", registry, methodKey,
                session, methodParams, responseChannel);

        final InvokeStatement invokeStatement = genInvokeStatement(method);
        if (invokeStatement.hasResponseChannel) {
            // 异步返回值，交给应用层返回结果
            builder.addStatement(invokeStatement.format, invokeStatement.params.toArray());
        } else {
            // 同步返回结果 - 底层返回结果
            builder.addCode("    try {\n");
            builder.addStatement("    " + invokeStatement.format, invokeStatement.params.toArray());
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                builder.addStatement("        $L.writeSuccess(null)", responseChannel);
            } else {
                builder.addStatement("        $L.writeSuccess(result)", responseChannel);
            }
            builder.addCode("    } catch (Throwable cause) {\n");
            builder.addStatement("        $L.writeFailure(cause)", responseChannel);
            builder.addStatement("        $T.rethrow(cause)", exceptionUtilsTypeName);

            builder.addCode("    }\n");
        }
        builder.addStatement("})");
        return builder.build();
    }

    /**
     * 加上methodKey防止重复
     *
     * @param methodKey rpc方法唯一键
     * @param method    rpc方法
     * @return 注册该rpc方法的
     */
    private String getServerProxyMethodName(int methodKey, ExecutableElement method) {
        return "_register" + BeanUtils.firstCharToUpperCase(method.getSimpleName().toString()) + "_" + methodKey;
    }

    /**
     * 生成方法调用代码，没有分号和换行符。
     * {@code Object result = instance.rpcMethod(a, b, c)}
     */
    private InvokeStatement genInvokeStatement(ExecutableElement method) {
        // 缩进
        final StringBuilder format = new StringBuilder("    ");
        final List<Object> params = new ArrayList<>(10);

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            // 声明返回值
            final TypeName returnTypeName = ParameterizedTypeName.get(method.getReturnType());
            format.append("$T result = ");
            params.add(returnTypeName);
        }

        // 调用方法
        format.append("$L.$L(");
        params.add(instance);
        params.add(method.getSimpleName().toString());

        boolean hasResponseChannel = false;
        boolean needDelimiter = false;
        int index = 0;
        for (VariableElement variableElement : method.getParameters()) {
            if (needDelimiter) {
                format.append(", ");
            } else {
                needDelimiter = true;
            }

            if (isSession(variableElement)) {
                format.append(session);
            } else if (isResponseChannel(variableElement)) {
                format.append(responseChannel);
                hasResponseChannel = true;
            } else {
                final TypeName parameterTypeName = ParameterizedTypeName.get(variableElement.asType());
                if (parameterTypeName.isPrimitive()) {
                    // 基本类型需要两次转换，否则可能导致重载问题
                    // (int)((Integer)methodParams.get(index))
                    // eg:
                    // getName(int age);
                    // getName(Integer age);
                    format.append("($T)(($T)$L.get($L))");
                    params.add(parameterTypeName);
                    params.add(parameterTypeName.box());
                } else {
                    format.append("($T)$L.get($L)");
                    params.add(parameterTypeName);
                }
                params.add(methodParams);
                params.add(index);
                index++;
            }
        }
        format.append(")");
        return new InvokeStatement(format.toString(), params, hasResponseChannel);
    }

    private static class InvokeStatement {

        private final String format;
        private final List<Object> params;
        private final boolean hasResponseChannel;

        private InvokeStatement(String format, List<Object> params, boolean hasResponseChannel) {
            this.format = format;
            this.params = params;
            this.hasResponseChannel = hasResponseChannel;
        }
    }

    private static class ParseResult {
        // 除去特殊参数余下的参数
        private final List<ParameterSpec> realParameters;
        // 需要延迟初始化的位置
        private final int lazyIndexes;
        // 需要提前反序列化的位置
        private final int preIndexes;
        // 远程调用的返回值类型
        private final TypeMirror callReturnType;

        ParseResult(List<ParameterSpec> realParameters, int lazyIndexes, int preIndexes, TypeMirror callReturnType) {
            this.realParameters = realParameters;
            this.lazyIndexes = lazyIndexes;
            this.preIndexes = preIndexes;
            this.callReturnType = callReturnType;
        }
    }
}
