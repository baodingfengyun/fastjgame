/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.annotationprocessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.wjybxx.fastjgame.utils.AutoUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2019年8月26日15:12:45 第二版<br>
 * Q: 为什么不直接使用注解的Class写代码了？<br>
 * A: 不再直接依赖net模块。这样，net模块也可以使用该注解了。当然要支持这样的特性，编码就更加间接了，因此可读性可能降低不少，但是是值得的。
 * <p>
 * 为RpcService的注解生成工具类。
 * 我太难了...这套api根本都不熟悉，用着真难受。。。。。
 * <p>
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
 * 1. {@code typeUtils.isSameType(RpcResponseChannel<String>, RpcResponseChannel)  false} 带泛型和不带泛型的不是同一个类型。
 * {@code typeUtils.isAssignable(RpcResponseChannel<String>, RpcResponseChannel)  true}
 * {@code typeUtils.isSubType(RpcResponseChannel<String>, RpcResponseChannel)  true}
 * <p>
 * 2. 在注解中引用另一个类时，这个类可能还未被编译，需要通过捕获异常获取到未编译的类的{@link TypeMirror}.
 * <p>
 * 3. {@link Types#isSameType(TypeMirror, TypeMirror)}、{@link Types#isSubtype(TypeMirror, TypeMirror)} 、
 * {@link Types#isAssignable(TypeMirror, TypeMirror)} ，一般使用{@link DeclaredType}，{@link DeclaredType}代表一个类或接口。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class RpcServiceProcessor extends AbstractProcessor {

    private static final String BUILDER_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.RpcBuilder";
    private static final String DEFAULT_BUILDER_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.DefaultRpcBuilder";
    private static final String SESSION_CANONICAL_NAME = "com.wjybxx.fastjgame.net.session.Session";

    private static final String RPC_SERVICE_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.RpcService";
    private static final String RPC_METHOD_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.RpcMethod";
    private static final String RPC_SERVICE_PROXY_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.RpcServiceProxy";
    private static final String RPC_METHOD_PROXY_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.RpcMethodProxy";
    private static final String LAZY_SERIALIZABLE_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.LazySerializable";
    private static final String PRE_DESERIALIZE_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.PreDeserializable";

    private static final String CHANNEL_CANONICAL_NAME = "com.wjybxx.fastjgame.net.common.RpcResponseChannel";
    private static final String RPC_RESPONSE_CANONICAL_NAME = "com.wjybxx.fastjgame.net.common.RpcResponse";

    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.RpcFunctionRegistry";

    private static final String SERVICE_ID_METHOD_NAME = "serviceId";
    private static final String METHOD_ID_METHOD_NAME = "methodId";

    private static final String registry = "registry";
    private static final String session = "session";
    private static final String methodParams = "methodParams";
    private static final String responseChannel = "responseChannel";
    private static final String result = "result";

    // 工具类
    private Types typeUtils;
    private Elements elementUtils;
    private Messager messager;

    /**
     * 生成信息
     */
    private AnnotationSpec generatedAnnotation;
    /**
     * unchecked注解
     */
    private AnnotationSpec uncheckedAnnotation;
    /**
     * 所有的serviceId集合，判断重复
     */
    private final ShortSet serviceIdSet = new ShortOpenHashSet(64);
    /**
     * 所有的methodKey集合，判断重复，只能检测当前模块的重复。 编译是分模块编译的，每一个模块都是一个新的processor
     */
    private final IntSet methodKeySet = new IntOpenHashSet(256);
    /**
     * 无界泛型通配符
     */
    private WildcardType wildcardType;

    /**
     * {@code RpcBuilder}对应的类型
     */
    private TypeElement builderElement;
    /**
     * {@code RpcResponseChannel}对应的类型
     */
    private DeclaredType responseChannelType;
    /**
     * {@code Session}对应的类型
     */
    private DeclaredType sessionType;
    /**
     * {@code LazySerializable}对应的类型
     */
    private DeclaredType lazySerializeType;
    private DeclaredType preDeserializeType;

    private TypeElement rpcServiceElement;
    private DeclaredType rpcServiceDeclaredType;
    private DeclaredType rpcMethodDeclaredType;

    private ClassName rpcServiceProxyTypeName;
    private ClassName rpcMethodProxyTypeName;
    private ClassName rpcResponseTypeName;

    private ClassName registryTypeName;
    private TypeName defaultBuilderRawTypeName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();

        generatedAnnotation = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", RpcServiceProcessor.class.getCanonicalName())
                .build();

        uncheckedAnnotation = AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .build();

        wildcardType = typeUtils.getWildcardType(null, null);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(RPC_SERVICE_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private void ensureInited() {
        if (rpcServiceElement != null) {
            // 已初始化
            return;
        }
        rpcServiceElement = elementUtils.getTypeElement(RPC_SERVICE_CANONICAL_NAME);
        rpcServiceDeclaredType = typeUtils.getDeclaredType(rpcServiceElement);
        rpcMethodDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(RPC_METHOD_CANONICAL_NAME));

        rpcServiceProxyTypeName = ClassName.get(elementUtils.getTypeElement(RPC_SERVICE_PROXY_CANONICAL_NAME));
        rpcMethodProxyTypeName = ClassName.get(elementUtils.getTypeElement(RPC_METHOD_PROXY_CANONICAL_NAME));

        registryTypeName = ClassName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME));
        rpcResponseTypeName = ClassName.get(elementUtils.getTypeElement(RPC_RESPONSE_CANONICAL_NAME));

        builderElement = elementUtils.getTypeElement(BUILDER_CANONICAL_NAME);

        responseChannelType = typeUtils.getDeclaredType(elementUtils.getTypeElement(CHANNEL_CANONICAL_NAME));
        sessionType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SESSION_CANONICAL_NAME));

        lazySerializeType = typeUtils.getDeclaredType(elementUtils.getTypeElement(LAZY_SERIALIZABLE_CANONICAL_NAME));
        preDeserializeType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PRE_DESERIALIZE_CANONICAL_NAME));

        defaultBuilderRawTypeName = TypeName.get(typeUtils.getDeclaredType(elementUtils.getTypeElement(DEFAULT_BUILDER_CANONICAL_NAME)));
    }


    /**
     * 只有代码中出现指定的注解时才会走到该方法，而{@link #init(ProcessingEnvironment)}每次编译都会执行，不论是否出现指定注解。
     * <p>
     * 处理源自前一轮的类型元素的一组注解类型，并返回此处理器是否声明了这些注解类型。
     * 如果返回true，则声明注释类型，并且不会要求后续处理器处理它们;
     * 如果返回false，则注释类型无人认领，并且可能要求后续处理器处理它们。 处理器可以始终返回相同的布尔值，或者可以基于所选择的标准改变结果。
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();
        // 该注解只有类才可以使用
        @SuppressWarnings("unchecked")
        Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(rpcServiceElement);
        for (TypeElement typeElement : typeElementSet) {
            genProxyClass(typeElement);
        }
        return true;
    }

    private void genProxyClass(TypeElement typeElement) {
        final Optional<? extends AnnotationMirror> serviceAnnotationOption = AutoUtils.findFirstAnnotationNotInheritance(typeUtils, typeElement, rpcServiceDeclaredType);
        assert serviceAnnotationOption.isPresent();
        // 基本类型会被包装，Object不能直接转short
        final short serviceId = (Short) AutoUtils.getAnnotationValueNotDefault(serviceAnnotationOption.get(), SERVICE_ID_METHOD_NAME);

        if (serviceId <= 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " must greater than 0!", typeElement);
            return;
        }

        if (!serviceIdSet.add(serviceId)) {
            // 打印重复serviceId
            messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " is duplicate!", typeElement);
            return;
        }

        // 筛选rpc方法
        final List<ExecutableElement> allMethods = typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());

        // allMethods.size() == 0 也必须重新生成文件
        List<MethodSpec> clientMethodProxyList = new ArrayList<>(allMethods.size());
        List<MethodSpec> serverMethodProxyList = new ArrayList<>(allMethods.size());

        // 生成代理方法
        for (ExecutableElement method : allMethods) {
            final Optional<? extends AnnotationMirror> rpcMethodAnnotation = AutoUtils.findFirstAnnotationNotInheritance(typeUtils, method, rpcMethodDeclaredType);
            if (!rpcMethodAnnotation.isPresent()) {
                // 不是rpc方法，跳过
                continue;
            }
            if (method.isVarArgs()) {
                messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod is not support varArgs!", method);
                continue;
            }
            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod must be public!", method);
                continue;
            }
            // 方法id，基本类型会被封装为包装类型，Object并不能直接转换到基本类型
            final short methodId = (Short) AutoUtils.getAnnotationValueNotDefault(rpcMethodAnnotation.get(), METHOD_ID_METHOD_NAME);
            if (methodId < 0 || methodId > 9999) {
                messager.printMessage(Diagnostic.Kind.ERROR, " methodId " + methodId + " must between [0,9999]!", method);
                continue;
            }
            // 方法的唯一键，乘以1W比位移有更好的可读性
            final int methodKey = (int) serviceId * 10000 + methodId;
            // 重复检测
            if (!methodKeySet.add(methodKey)) {
                messager.printMessage(Diagnostic.Kind.ERROR, " methodKey " + methodKey + " is duplicate!", method);
            }
            // 生成双方的代理代码
            clientMethodProxyList.add(genClientMethodProxy(methodKey, method));
            serverMethodProxyList.add(genServerMethodProxy(methodKey, method));
        }

        // 保存serviceId
        final AnnotationSpec proxyAnnotation = AnnotationSpec.builder(rpcServiceProxyTypeName)
                .addMember("serviceId", "$L", serviceId).build();

        final String className = typeElement.getSimpleName().toString();
        final String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        // 客户端代理，生成在core包下
        {
            // 代理类不可以继承
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className + "RpcProxy")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(generatedAnnotation)
                    .addAnnotation(proxyAnnotation);

            // 添加客户端代理方法，在最上面
            typeBuilder.addMethods(clientMethodProxyList);

            TypeSpec typeSpec = typeBuilder.build();
            JavaFile javaFile = JavaFile
                    .builder(packageName, typeSpec)
                    // 不用导入java.lang包
                    .skipJavaLangImports(true)
                    // 4空格缩进
                    .indent("    ")
                    .build();

            try {
                // 输出到注解处理器配置的路径下，这样才可以在下一轮检测到并进行编译 输出到processingEnv.getFiler()会立即参与编译
                // 如果自己指定路径，可以生成源码到指定路径，但是可能无法被编译器检测到，本轮无法参与编译，需要再进行一次编译
                javaFile.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "writeToFile caught exception!", typeElement);
            }
        }

        // 服务器代理，生成在自己模块
        {
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className + "RpcRegister")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(generatedAnnotation)
                    .addAnnotation(proxyAnnotation);
            // register类涉及大量的类型转换，全部取消警告
            typeBuilder.addAnnotation(uncheckedAnnotation);

            typeBuilder.addMethod(genRegisterMethod(typeElement, serverMethodProxyList));
            typeBuilder.addMethods(serverMethodProxyList);

            TypeSpec typeSpec = typeBuilder.build();
            JavaFile javaFile = JavaFile
                    .builder(packageName, typeSpec)
                    // 不用导入java.lang包
                    .skipJavaLangImports(true)
                    // 4空格缩进
                    .indent("    ")
                    .build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "writeToFile caught exception!", typeElement);
            }
        }
    }

    // ----------------------------------------- 为客户端生成代理方法 -------------------------------------------

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
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString());
        builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        // 拷贝泛型参数
        AutoUtils.copyTypeVariableNames(builder, method);

        // 解析方法参数 -- 提炼方法是为了减少当前方法的长度
        final ParseResult parseResult = parseParameters(method);
        final List<ParameterSpec> realParameters = parseResult.realParameters;

        // 添加返回类型-带泛型
        DeclaredType realReturnType = typeUtils.getDeclaredType(builderElement, parseResult.callReturnType);
        builder.returns(ClassName.get(realReturnType));
        // 拷贝参数列表
        builder.addParameters(realParameters);
        // 注明方法键值
        AnnotationSpec annotationSpec = AnnotationSpec.builder(rpcMethodProxyTypeName)
                .addMember("methodKey", "$L", methodKey)
                .build();
        builder.addAnnotation(annotationSpec);

        // 搜集参数代码块（一个参数时不能使用singleTonList了，因为可能要修改内容）
        if (realParameters.size() == 0) {
            // 无参时，使用 Collections.emptyList();
            builder.addStatement("return new $T<>($L, $T.emptyList(), $L, $L)", defaultBuilderRawTypeName, methodKey, Collections.class,
                    parseResult.lazyIndexes, parseResult.preIndexes);
        } else {
            builder.addStatement("$T<Object> $L = new $T<>($L)", ArrayList.class, methodParams, ArrayList.class, realParameters.size());
            for (ParameterSpec parameterSpec : realParameters) {
                builder.addStatement("$L.add($L)", methodParams, parameterSpec.name);
            }
            builder.addStatement("return new $T<>($L, $L, $L, $L)", defaultBuilderRawTypeName, methodKey, methodParams,
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
        TypeMirror callReturenType = null;

        // 需要延迟初始化的参数所在的位置
        int lazyIndexes = 0;
        // 需要提前反序列化的参数所在的位置
        int preIndexes = 0;

        // 筛选参数
        for (VariableElement variableElement : originParameters) {
            // rpcResponseChannel需要从参数列表删除，并捕获泛型类型
            if (callReturenType == null && isResponseChannel(variableElement)) {
                callReturenType = getResponseChannelReturnType(variableElement);
                continue;
            }
            // session需要从参数列表删除
            if (isSession(variableElement)) {
                continue;
            }
            if (isLazySerializeParameter(variableElement)) {
                // 延迟初始化的参数 - 要替换为Object
                lazyIndexes |= 1 << realParameters.size();
                final ParameterSpec parameterSpec = ParameterSpec.builder(Object.class, variableElement.getSimpleName().toString()).build();
                realParameters.add(parameterSpec);
            } else if (isPreDeserializeParameter(variableElement)) {
                // 查看是否需要提前反序列化 - 要替换为byte[]
                preIndexes |= 1 << realParameters.size();
                final ParameterSpec parameterSpec = ParameterSpec.builder(byte[].class, variableElement.getSimpleName().toString()).build();
                realParameters.add(parameterSpec);
            } else {
                // 普通参数
                realParameters.add(ParameterSpec.get(variableElement));
            }
        }
        if (null == callReturenType) {
            // 参数列表中不存在responseChannel
            callReturenType = method.getReturnType();
        } else {
            // 如果参数列表中存在responseChannel，那么返回值必须是void
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType is not void, but parameters contains responseChannel!", method);
            }
        }
        if (callReturenType.getKind() == TypeKind.VOID) {
            // 返回值类型为void，rpcBuilder泛型参数为泛型通配符
            callReturenType = wildcardType;
        } else {
            // 基本类型转包装类型
            if (callReturenType.getKind().isPrimitive()) {
                callReturenType = typeUtils.boxedClass((PrimitiveType) callReturenType).asType();
            }
        }
        return new ParseResult(realParameters, lazyIndexes, preIndexes, callReturenType);
    }

    /**
     * 是否是 {@code RpcResponseChannel} 类型。
     * (带泛型的和不带泛型的 isSameType返回false)
     */
    private boolean isResponseChannel(VariableElement variableElement) {
        return AutoUtils.isTargetDeclaredType(variableElement, declaredType -> typeUtils.isAssignable(declaredType, responseChannelType));
    }

    /**
     * 是否是 {@code Session}类型
     */
    private boolean isSession(VariableElement variableElement) {
        return AutoUtils.isTargetDeclaredType(variableElement, declaredType -> typeUtils.isSubtype(declaredType, sessionType));
    }

    /**
     * 是否可延迟序列化的参数？
     * 1. 必须带有{@link #LAZY_SERIALIZABLE_CANONICAL_NAME}注解
     * 2. 必须是字节数组
     */
    private boolean isLazySerializeParameter(VariableElement variableElement) {
        if (!AutoUtils.findFirstAnnotationNotInheritance(typeUtils, variableElement, lazySerializeType).isPresent()) {
            return false;
        }
        if (AutoUtils.isTargetArrayType(variableElement, TypeKind.BYTE)) {
            return true;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation LazySerializable only support byte[]", variableElement);
        return false;
    }

    /**
     * 是否是需要提前反序列化的参数？
     * 1. 必须带有{@link #PRE_DESERIALIZE_CANONICAL_NAME}注解
     * 2. 不能是字节数组
     */
    private boolean isPreDeserializeParameter(VariableElement variableElement) {
        if (!AutoUtils.findFirstAnnotationNotInheritance(typeUtils, variableElement, preDeserializeType).isPresent()) {
            return false;
        }
        if (AutoUtils.isTargetArrayType(variableElement, TypeKind.BYTE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation PreDeserializable doesn't support byte[]", variableElement);
            return false;
        }
        return true;
    }

    /**
     * 获取RpcResponseChannel的泛型参数
     */
    private TypeMirror getResponseChannelReturnType(final VariableElement variableElement) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<TypeMirror, Void>() {
            @Override
            public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
                if (t.getTypeArguments().size() > 0) {
                    // 第一个参数就是返回值类型，这里的泛型也可能是'?'
                    return t.getTypeArguments().get(0);
                } else {
                    // 声明类型木有泛型参数，返回Object类型，并打印一个警告
                    // 2019年8月23日21:13:35 改为编译错误
                    messager.printMessage(Diagnostic.Kind.ERROR, "RpcResponseChannel missing type parameter!", variableElement);
                    return elementUtils.getTypeElement(Object.class.getCanonicalName()).asType();
                }
            }
        }, null);
    }

    // --------------------------------------------- 为服务端生成代理方法 ---------------------------------------

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
        final String className = typeElement.getSimpleName().toString();
        final String classParamName = AutoUtils.firstCharToLowerCase(className);
        MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(registryTypeName, registry)
                .addParameter(TypeName.get(typeElement.asType()), classParamName);

        // 添加调用
        for (MethodSpec method : serverProxyMethodList) {
            builder.addStatement("$L($L, $L)", method.name, registry, classParamName);
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
     * 		       RpcResponse response = RpcResponse.ERROR;
     * 		       try {
     * 		     		V result = instance.method2(methodParams.get(0), methodParams.get(1));
     * 					response = RpcResponse.newSucceedResponse(result);
     *               } finally {
     * 		           responseChannel.write(response);
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
     * 		       RpcResponse response = RpcResponse.ERROR;
     * 		       try {
     * 		       		instance.method1(methodParams.get(0), methodParams.get(1), responseChannel);
     * 					response = RpcResponse.SUCCESS;
     *               } finally {
     * 		           responseChannel.write(response);
     *               }
     *            });
     *        }
     * }
     * </pre>
     *
     * @param executableElement rpcMethod
     * @return methodName
     */
    private MethodSpec genServerMethodProxy(int methodKey, ExecutableElement executableElement) {
        final TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
        final String classParamName = AutoUtils.firstCharToLowerCase(typeElement.getSimpleName().toString());

        // 加上methodKey防止签名重复
        final String methodName = "_register" + AutoUtils.firstCharToUpperCase(executableElement.getSimpleName().toString()) + "_" + methodKey;
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(registryTypeName, registry)
                .addParameter(TypeName.get(typeElement.asType()), classParamName);

        // 注明方法键值
        AnnotationSpec annotationSpec = AnnotationSpec.builder(rpcMethodProxyTypeName)
                .addMember("methodKey", "$L", methodKey)
                .build();
        builder.addAnnotation(annotationSpec);

        builder.addCode("$L.register($L, ($L, $L, $L) -> {\n", registry, methodKey,
                session, methodParams, responseChannel);

        if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
            builder.addStatement("    $T response = $T.ERROR", rpcResponseTypeName, rpcResponseTypeName);
            // 同步返回结果
            builder.addCode("    try {\n");
            final InvokeStatement invokeStatement = genInvokeStatement(executableElement.getReturnType(), classParamName, executableElement);
            builder.addStatement("    " + invokeStatement.format, invokeStatement.params.toArray());
            builder.addStatement("        response = $T.newSucceedResponse($L)", rpcResponseTypeName, result);
            builder.addCode("    } finally {\n");
            builder.addStatement("        $L.write(response)", responseChannel);
            builder.addCode("    }\n");
        } else {
            // 方法声明的返回值类型为void，可能为异步方法，也可能是真没有返回值，但是没有返回值也需要告知对方是否成功了，如果对方发的是rpc调用。
            final InvokeStatement invokeStatement = genInvokeStatement(null, classParamName, executableElement);
            if (invokeStatement.asyncMethod) {
                // 有异步返回值，交给应用层返回结果
                builder.addStatement(invokeStatement.format, invokeStatement.params.toArray());
            } else {
                // 确实没有结果，返回null给远程
                builder.addStatement("    $T response = $T.ERROR", rpcResponseTypeName, rpcResponseTypeName);
                // 同步返回结果
                builder.addCode("    try {\n");
                builder.addStatement("    " + invokeStatement.format, invokeStatement.params.toArray());
                builder.addStatement("        response = $T.SUCCESS", rpcResponseTypeName);
                builder.addCode("    } finally {\n");
                builder.addStatement("        $L.write(response)", responseChannel);
                builder.addCode("    }\n");
            }
        }
        builder.addStatement("})");
        return builder.build();
    }

    /**
     * 生成方法调用代码，没有分号和换行符。
     */
    private InvokeStatement genInvokeStatement(TypeMirror returnType, String classParamName, ExecutableElement executableElement) {
        // 缩进
        StringBuilder format = new StringBuilder("    ");
        List<Object> params = new ArrayList<>(10);

        if (null != returnType) {
            format.append("$T $L = ");
            // 返回值是什么类型就是什么类型，不需要包装
            TypeName typeName = ParameterizedTypeName.get(returnType);
            params.add(typeName);
            params.add(result);
        }

        format.append("$L.$L(");
        params.add(classParamName);
        params.add(executableElement.getSimpleName().toString());

        boolean asyncMethod = false;
        boolean needDelimiter = false;
        int index = 0;
        for (VariableElement variableElement : executableElement.getParameters()) {
            if (needDelimiter) {
                format.append(", ");
            } else {
                needDelimiter = true;
            }

            if (isResponseChannel(variableElement)) {
                format.append(responseChannel);
                asyncMethod = true;
            } else if (isSession(variableElement)) {
                format.append(session);
            } else {
                TypeName typeName = ParameterizedTypeName.get(variableElement.asType());
                if (typeName.isPrimitive()) {
                    // 基本类型需要两次转换，否则可能导致重载问题
                    // (int)((Integer)methodParams.get(index))
                    // eg:
                    // getName(int age);
                    // getName(Integer age);
                    format.append("($T)(($T)$L.get($L))");
                    params.add(typeName);
                    params.add(typeName.box());
                } else {
                    format.append("($T)$L.get($L)");
                    params.add(typeName);

                }
                params.add(methodParams);
                params.add(index);
                index++;
            }
        }
        format.append(")");
        return new InvokeStatement(format.toString(), params, asyncMethod);
    }

    private static class InvokeStatement {

        private final String format;
        private final List<Object> params;
        private final boolean asyncMethod;

        private InvokeStatement(String format, List<Object> params, boolean asyncMethod) {
            this.format = format;
            this.params = params;
            this.asyncMethod = asyncMethod;
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
