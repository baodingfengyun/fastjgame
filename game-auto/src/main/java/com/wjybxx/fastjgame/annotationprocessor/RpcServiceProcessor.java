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
import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcMethodProxy;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.annotation.RpcServiceProxy;
import com.wjybxx.fastjgame.misc.DefaultRpcBuilder;
import com.wjybxx.fastjgame.misc.RpcBuilder;
import com.wjybxx.fastjgame.misc.RpcFunctionRegistry;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.RpcResponseChannel;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.utils.AutoUtils;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.MathUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 为RpcService的注解生成工具类。
 * 我太难了...这套api根本都不熟悉，用着真难受。。。。。
 *
 * 由于类文件结构比较文件，API都是基于访问者模式的，完全不能直接获取数据，难受的一比。
 *
 * 注意：使用class对象有限制，只可以使用JDK自带的class 和 该jar包（该项目）中所有的class (包括依赖的jar包)
 *
 * 不在本包中的类，只能使用{@link Element}) 和 {@link javax.lang.model.type.TypeMirror}。
 * 因为需要使用的类可能还没编译，也就不存在对应的class文件，加载不到。
 * 这也是注解处理器必须要打成jar包的原因。。不然对应的注解类可能都还未被编译。。。。
 *
 * {@link RoundEnvironment} 运行环境，编译器环境。（他是一个独立的编译器，无法直接调试，需要远程调试）
 * {@link Types} 编译时的类型信息（非常类似 Class，但那是运行时的东西，注意现在是编译时）
 * {@link Filer} 文件读写 util (然而，Filer 有局限性，只有 create 相关的接口)
 * {@link Elements} 代码结构信息
 *
 * 遇见的坑，先记一笔：
 * 1. {@code types.isSameType(RpcResponseChannel<String>, RpcResponseChannel)  false} 带泛型和不带泛型的不是同一个类型。
 *    {@code types.isAssignable(RpcResponseChannel<String>, RpcResponseChannel)  true}
 *    {@code types.isSubType(RpcResponseChannel<String>, RpcResponseChannel)  true}
 *
 * 2. 在注解中引用另一个类时，这个类可能还未被编译，需要通过捕获异常获取到未编译的类的{@link TypeMirror}.
 *
 * 3. 我太难了，这Rpc代理得生成两个文件，一个生成在core包，一个生成在自己的包...
 *
 * 4. {@link Types#isSameType(TypeMirror, TypeMirror)}、{@link Types#isSubtype(TypeMirror, TypeMirror)} 、
 * {@link Types#isAssignable(TypeMirror, TypeMirror)} 必须是{@link DeclaredType}才可以。
 *
 * {@link DeclaredType}是变量、参数的声明类型。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class RpcServiceProcessor extends AbstractProcessor {

	private static final String registry = "registry";
	private static final String session = "session";
	private static final String methodParams = "methodParams";
	private static final String responseChannel = "responseChannel";
	private static final String result = "result";

	//rpc客户端代理包名
	private static final String proxy_package_name = "com.wjybxx.fastjgame.rpcproxy";

	// rpc服务器注册辅助类生成包名
	private static final String register_package_name = "com.wjybxx.fastjgame.rpcregister";

	// 工具类
	private Types types;
	private Elements elements;
	private Messager messager;
	/**
	 * {@link RpcBuilder}对应的类型
	 */
	private TypeElement builderElement;
	/**
	 * {@link Void}对应的类型
	 */
	private DeclaredType voidType;
	/**
	 * {@link RpcResponseChannel}对应的类型
	 */
	private DeclaredType responseChannelType;
	/**
	 * {@link com.wjybxx.fastjgame.net.Session}对应的类型
	 */
	private DeclaredType sessionType;
	/**
	 * {@link List}对应的类型
	 */
	private DeclaredType listType;

	/** 所有的serviceId集合，判断重复 */
	private final ShortSet serviceIdSet = new ShortOpenHashSet(128);
	/** 所有的methodKey集合，判断重复 */
	private final IntSet methodKeySet = new IntOpenHashSet(512);

	/** 生成信息 */
	private AnnotationSpec generatedAnnotation;
	/** unchecked注解 */
	private AnnotationSpec uncheckedAnnotation;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		types = processingEnv.getTypeUtils();
		elements = processingEnv.getElementUtils();
		messager = processingEnv.getMessager();

		builderElement = elements.getTypeElement(RpcBuilder.class.getCanonicalName());
		voidType = types.getDeclaredType(elements.getTypeElement(Void.class.getCanonicalName()));

		responseChannelType = types.getDeclaredType(elements.getTypeElement(RpcResponseChannel.class.getCanonicalName()));
		sessionType = types.getDeclaredType(elements.getTypeElement(Session.class.getCanonicalName()));
		listType = types.getDeclaredType(elements.getTypeElement(List.class.getCanonicalName()));

		generatedAnnotation = AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", RpcServiceProcessor.class.getCanonicalName())
				.build();

		uncheckedAnnotation = AnnotationSpec.builder(SuppressWarnings.class)
				.addMember("value", "$S", "unchecked")
				.build();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(RpcService.class.getCanonicalName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	/**
	 * 处理源自前一轮的类型元素的一组注解类型，并返回此处理器是否声明了这些注解类型。
	 * 如果返回true，则声明注释类型，并且不会要求后续处理器处理它们;
	 * 如果返回false，则注释类型无人认领，并且可能要求后续处理器处理它们。 处理器可以始终返回相同的布尔值，或者可以基于所选择的标准改变结果。
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// 该注解只有类才可以使用
		@SuppressWarnings("unchecked")
		Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(RpcService.class);
		for (TypeElement typeElement:typeElementSet) {
			genProxyClass(typeElement);
		}
		return true;
	}

	private void genProxyClass(TypeElement typeElement) {
		// 筛选rpc方法
		final List<ExecutableElement> proxyMethods = typeElement.getEnclosedElements().stream()
				.filter(element -> element.getKind() == ElementKind.METHOD)
				.map(element -> (ExecutableElement) element)
				.filter(element -> element.getAnnotation(RpcMethod.class) != null)
				.sorted(Comparator.comparingInt(e -> e.getAnnotation(RpcMethod.class).methodId()))
				.collect(Collectors.toList());
		// proxyMethods.size() == 0 也必须重新生成文件

		final short serviceId = typeElement.getAnnotation(RpcService.class).serviceId();
		if (!serviceIdSet.add(serviceId)) {
			// 打印重复serviceId
			messager.printMessage(Diagnostic.Kind.ERROR, " serviceId " + serviceId + " is duplicate!", typeElement);
		}

		List<MethodSpec> clientMethodProxyList = new ArrayList<>(proxyMethods.size());
		List<MethodSpec> serverMethodProxyList = new ArrayList<>(proxyMethods.size());

		// 生成代理方法
		for (ExecutableElement method:proxyMethods) {
			if (method.isVarArgs()) {
				messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod is not support varArgs!", method);
				continue;
			}
			if (!method.getModifiers().contains(Modifier.PUBLIC)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "RpcMethod must be public！", method);
				continue;
			}
			// 方法id
			final short methodId = method.getAnnotation(RpcMethod.class).methodId();
			// 方法的唯一键，乘以1W比位移有更好的可读性
			final int methodKey = MathUtils.safeMultiplyShort(serviceId, (short) 10000) + methodId;
			// 重复检测
			if (!methodKeySet.add(methodKey)) {
				messager.printMessage(Diagnostic.Kind.ERROR," methodKey " + methodKey + " is duplicate!", method);
			}
			// 生成双方的代理代码
			clientMethodProxyList.add(genClientMethodProxy(methodKey, method));
			serverMethodProxyList.add(genServerMethodProxy(methodKey, method));
		}

		// 保存serviceId
		AnnotationSpec proxyAnnotation = AnnotationSpec.builder(RpcServiceProxy.class)
				.addMember("serviceId", "$L", serviceId).build();

		final String className = typeElement.getSimpleName().toString();
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
					.builder(proxy_package_name, typeSpec)
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
					.builder(register_package_name, typeSpec)
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
	 * 		}
	 * }
	 * </pre>
	 *
	 *
	 */
	private MethodSpec genClientMethodProxy(int methodKey, ExecutableElement method) {
		// 工具方法 public static RpcBuilder<V>
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString());
		builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		// 拷贝泛型参数
		AutoUtils.copyTypeVariableNames(builder, method);

		// 解析方法参数 -- 提炼方法是为了减少当前方法的长度
		final ParseResult parseResult = parseParameters(method);
		final List<VariableElement> availableParameters = parseResult.availableParameters;
		final TypeMirror callReturnType = parseResult.callReturnType;
		final boolean allowCallback = parseResult.allowCallback;

		// 添加返回类型
		DeclaredType realReturnType = types.getDeclaredType(builderElement, callReturnType);
		builder.returns(ClassName.get(realReturnType));
		// 拷贝参数列表
		AutoUtils.copyParameters(builder, availableParameters);
		// 注明方法键值
		AnnotationSpec annotationSpec = AnnotationSpec.builder(RpcMethodProxy.class)
				.addMember("methodKey", "$L", methodKey)
				.build();
		builder.addAnnotation(annotationSpec);

		// 搜集参数代码块
		if (availableParameters.size() == 0) {
			builder.addStatement("return new $T<>($L, $T.emptyList(), $L)", DefaultRpcBuilder.class, methodKey, Collections.class, allowCallback);
		} else if (availableParameters.size() == 1) {
			final VariableElement firstVariableElement = availableParameters.get(0);
			final String firstParameterName = firstVariableElement.getSimpleName().toString();
			builder.addStatement("return new $T<>($L, $T.singletonList($L), $L)", DefaultRpcBuilder.class, methodKey, Collections.class, firstParameterName, allowCallback);
		} else {
			builder.addStatement("$T<Object> $L = new $T<>($L)", List.class, methodParams, ArrayList.class, availableParameters.size());
			for (VariableElement variableElement:availableParameters) {
				builder.addStatement("$L.add($L)", methodParams, variableElement.getSimpleName());
			}
			builder.addStatement("return new $T<>($L, $L, $L)", DefaultRpcBuilder.class, methodKey, "methodParams", allowCallback);
		}
		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private ParseResult parseParameters(ExecutableElement method) {
		// 原始参数列表
		final List<VariableElement> originParameters = (List<VariableElement>) method.getParameters();
		// 有效参数列表
		final List<VariableElement> availableParameters = new ArrayList<>(originParameters.size());
		// 返回值类型
		TypeMirror callReturenType = null;
		// 是否允许回调--是否是单向消息，返回类型是否是void/Void
		boolean allowCallback;

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
			availableParameters.add(variableElement);
		}
		if (null == callReturenType) {
			// 参数列表中不存在responseChannel
			callReturenType = method.getReturnType();
		} else {
			// 如果参数列表中存在responseChannel，那么返回值必须是void
			if (!isVoidType(method.getReturnType())){
				messager.printMessage(Diagnostic.Kind.ERROR, "callReturnType is not void, but parameters contains responseChannel!", method);
			}
		}
		if (isVoidType(callReturenType)) {
			allowCallback = false;
			// void转Void
			callReturenType = voidType;
		} else {
			allowCallback = true;
			// 基本类型转包装类型
			if (callReturenType.getKind().isPrimitive()) {
				callReturenType = types.boxedClass((PrimitiveType) callReturenType).asType();
			}
		}
		return new ParseResult(availableParameters, callReturenType, allowCallback);
	}

	/**
	 * 判断指定类型是否是void 或Void类型
	 */
	private boolean isVoidType(TypeMirror typeMirror) {
		return typeMirror.getKind() == TypeKind.VOID || isTargetType(typeMirror, declaredType -> types.isSameType(declaredType, voidType));
	}

	/**
	 * 是否是 {@link RpcResponseChannel} 类型。
	 */
	private boolean isResponseChannel(VariableElement variableElement) {
		return isTargetType(variableElement.asType(), declaredType -> types.isAssignable(declaredType, responseChannelType));
	}

	/**
	 * 是否是 {@link Session}类型
	 */
	private boolean isSession(VariableElement variableElement) {
		return isTargetType(variableElement.asType(), declaredType -> types.isSubtype(declaredType, sessionType));
	}

	/**
	 * 是否是 {@link List}类型
	 */
	private boolean isList(VariableElement variableElement) {
		return isTargetType(variableElement.asType(), declaredType -> types.isSubtype(declaredType, listType));
	}

	/**
	 * 不能访问{@link VariableElement}，会死循环，必须转换成typeMirror访问，否则无法访问到详细信息。
	 */
	private boolean isTargetType(final TypeMirror typeMirror, final Predicate<DeclaredType> matcher) {
		return typeMirror.accept(new SimpleTypeVisitor8<Boolean, Void>(){

			@Override
			public Boolean visitDeclared(DeclaredType t, Void aVoid) {
				// 访问声明的类型
				return matcher.test(t);
			}

			@Override
			protected Boolean defaultAction(TypeMirror e, Void aVoid) {
				return false;
			}

		}, null);
	}

	/**
	 * 获取RpcResponseChannel的泛型参数
	 */
	private TypeMirror getResponseChannelReturnType(final VariableElement variableElement) {
		return variableElement.asType().accept(new SimpleTypeVisitor8<TypeMirror, Void>(){
			@Override
			public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
				if (t.getTypeArguments().size() > 0) {
					// 第一个参数就是返回值类型
					return t.getTypeArguments().get(0);
				} else {
					// 声明类型木有泛型参数，返回Object类型，并打印一个警告
					// 2019年8月23日21:13:35 改为编译错误
					messager.printMessage(Diagnostic.Kind.ERROR, "RpcResponseChannel missing type parameter.", variableElement);
					return elements.getTypeElement(Object.class.getCanonicalName()).asType();
				}
			}
		}, null);
	}

	// --------------------------------------------- 为服务端生成代理方法 ---------------------------------------

	/**
	 * 生成注册方法
	 * {@code
	 * 		public static void register(RpcFunctionRegistry registry, T instance) {
	 * 		 	registerGetMethod1(registry, instance);
	 * 		 	registerGetMethod2(registry, instance);
	 * 		}
	 * }
	 * @param typeElement 类信息
	 * @param serverProxyMethodList 被代理的服务器方法
	 */
	private MethodSpec genRegisterMethod(TypeElement typeElement, List<MethodSpec> serverProxyMethodList) {
		final String className = typeElement.getSimpleName().toString();
		final String classParamName = AutoUtils.firstCharToLowerCase(className);
		MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(TypeName.VOID)
				.addParameter(RpcFunctionRegistry.class, registry)
				.addParameter(TypeName.get(typeElement.asType()), classParamName);

		// 添加调用
		for (MethodSpec method:serverProxyMethodList) {
			builder.addStatement("$L($L, $L)", method.name, registry, classParamName);
		}
		return builder.build();
	}

	/**
	 * 为某个具体方法生成注册方法
	 * {@code
	 * 		private static void registerGetMethod1(RpcFunctionRegistry registry, T instance) {
	 * 		    registry.register(10001, (session, methodParams, responseChannel) -> {
	 * 		       // code-
	 * 		       instance.method1(methodParams.get(0), methodParams.get(1), responseChannel);
	 * 		    });
	 * 		}
	 * }
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
				.addParameter(RpcFunctionRegistry.class, registry)
				.addParameter(TypeName.get(typeElement.asType()), classParamName);

		// 注明方法键值
		AnnotationSpec annotationSpec = AnnotationSpec.builder(RpcMethodProxy.class)
				.addMember("methodKey", "$L", methodKey)
				.build();
		builder.addAnnotation(annotationSpec);

		builder.addCode("$L.register($L, ($L, $L, $L) -> {\n", registry, methodKey,
				session, methodParams, responseChannel);

		if (!isVoidType(executableElement.getReturnType())) {
			// 同步返回结果
			builder.addCode("    try {\n");
			final InvokeStatement invokeStatement = genInvokeStatement(executableElement.getReturnType(), classParamName, executableElement);
			builder.addStatement("    " +invokeStatement.format, invokeStatement.params.toArray());
			builder.addStatement("        $L.writeSuccess($L)", responseChannel, result);
			builder.addCode("    } catch (Exception e) {\n");
			builder.addCode("        // prevent timeout\n");
			builder.addStatement("        $L.write($T.ERROR)", responseChannel, RpcResponse.class);
			builder.addStatement("        $T.rethrow(e)", ConcurrentUtils.class);
			builder.addCode("    }");
		} else  {
			// 异步返回结果 或 没有结果
			final InvokeStatement invokeStatement = genInvokeStatement(null, classParamName, executableElement);
			builder.addStatement(invokeStatement.format, invokeStatement.params.toArray());
		}
		builder.addStatement("})");
		return builder.build();
	}

	/**
	 * 生成方法调用代码，没有分号和换行符。
	 */
	private InvokeStatement genInvokeStatement(TypeMirror returnType,String classParamName, ExecutableElement executableElement) {
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

		boolean needDelimiter = false;
		int index = 0;
		for (VariableElement variableElement:executableElement.getParameters()) {
			if (needDelimiter) {
				format.append(", ");
			} else {
				needDelimiter = true;
			}

			if (isResponseChannel(variableElement)) {
				format.append(responseChannel);
			} else if (isSession(variableElement)){
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
		return new InvokeStatement(format.toString(), params);
	}

	private static class InvokeStatement {
		private final String format;
		private final List<Object> params;

		private InvokeStatement(String format, List<Object> params) {
			this.format = format;
			this.params = params;
		}
	}

	private static class ParseResult {
		// 除去特殊参数余下的参数
		private final List<VariableElement> availableParameters;
		// 远程调用的返回值类型
		private final TypeMirror callReturnType;
		// 是否允许回调
		private final boolean allowCallback;

		ParseResult(List<VariableElement> availableParameters, TypeMirror callReturnType, boolean allowCallback) {
			this.availableParameters = availableParameters;
			this.callReturnType = callReturnType;
			this.allowCallback = allowCallback;
		}
	}
}
