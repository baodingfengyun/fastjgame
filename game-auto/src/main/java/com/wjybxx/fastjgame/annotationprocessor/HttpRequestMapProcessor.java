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

import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * http请求注解处理器。
 * 写习惯了，越来越容易了。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/27
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class HttpRequestMapProcessor extends AbstractProcessor {

	private static final String HTTP_REQUEST_MAPPING_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.HttpRequestMapping";
	private static final String PARAM_CANONICAL_NAME = "com.wjybxx.fastjgame.net.HttpRequestParam";
	private static final String SESSION_CANONICAL_NAME = "com.wjybxx.fastjgame.net.HttpSession";
	private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.net.HttpRequestHandlerRegistry";

	private static final String PATH_METHOD_NAME = "path";
	private static final String INHERIT_METHOD_NAME = "inherit";

	private static final char PATH_PREFIX = '/';

	private Types typeUtils;
	private Elements elementUtils;
	private Messager messager;

	private TypeElement httpRequestMappingElement;
	private DeclaredType httpRequestMappingDeclaredType;

	private DeclaredType sessionDeclaredType;
	private DeclaredType stringDeclaredType;
	private DeclaredType paramDeclaredType;

	private TypeName registryTypeName;

	/** 注解处理器信息 */
	private AnnotationSpec generatedAnnotation;

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(HTTP_REQUEST_MAPPING_CANONICAL_NAME);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		messager = processingEnv.getMessager();

		generatedAnnotation = AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", HttpRequestMapProcessor.class.getCanonicalName())
				.build();
	}

	private void ensureInited() {
		if (null != httpRequestMappingElement) {
			return;
		}
		httpRequestMappingElement = elementUtils.getTypeElement(HTTP_REQUEST_MAPPING_CANONICAL_NAME);
		httpRequestMappingDeclaredType = typeUtils.getDeclaredType(httpRequestMappingElement);

		sessionDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SESSION_CANONICAL_NAME));
		stringDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(String.class.getCanonicalName()));
		paramDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PARAM_CANONICAL_NAME));

		registryTypeName = TypeName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME).asType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ensureInited();

		// 该注解可以用在类和方法上，需要注意
		final Map<Element, ? extends List<? extends Element>> typeElement2MethodsMap = roundEnv.getElementsAnnotatedWith(httpRequestMappingElement).stream()
				.filter(e -> ((Element) e).getKind() == ElementKind.METHOD)
				.collect(Collectors.groupingBy((Function<Element, Element>) Element::getEnclosingElement));

		typeElement2MethodsMap.forEach((element, object) -> {
			genProxyClass((TypeElement) element, (List<ExecutableElement>) object);
		});
		return false;
	}

	private void genProxyClass(TypeElement typeElement, List<ExecutableElement> methodList) {
		final Optional<? extends AnnotationMirror> typeAnnotation = AutoUtils.findFirstAnnotationNotInheritance(typeUtils, typeElement, httpRequestMappingDeclaredType);
		final String parentPath = typeAnnotation
				.map(annotationMirror -> (String) AutoUtils.getAnnotationValueNotDefault(annotationMirror, PATH_METHOD_NAME))
				.orElse(null);
		// 父路径存在时需要校验
		if (parentPath != null && !checkPath(typeElement, parentPath)) {
			return;
		}

		final String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
		final String proxyClassName = typeElement.getSimpleName().toString() + "HttpRegister";

		TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(proxyClassName)
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addAnnotation(generatedAnnotation);

		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("register")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(registryTypeName, "registry")
				.addParameter(TypeName.get(typeElement.asType()), "instance");

		for (ExecutableElement method : methodList){
			final Optional<? extends AnnotationMirror> methodAnnotation = AutoUtils.findFirstAnnotationNotInheritance(typeUtils, method, httpRequestMappingDeclaredType);
			assert methodAnnotation.isPresent();
			final String childPath = (String) AutoUtils.getAnnotationValueNotDefault(methodAnnotation.get(), PATH_METHOD_NAME);
			if (!checkPath(method, childPath)) {
				continue;
			}
			// 访问权限必须是public
			if (!method.getModifiers().contains(Modifier.PUBLIC)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method must be public！", method);
				continue;
			}
			// 返回值类型必须是void
			if (method.getReturnType().getKind() != TypeKind.VOID) {
				messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType must be void!", method);
				continue;
			}
			// 必须是3个参数
			if (method.getParameters().size() != 3) {
				messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method must have three and only three parameter!", method);
				continue;
			}
			final VariableElement firstVariableElement = method.getParameters().get(0);
			// 第一个参数必须是HttpSession
			if (!AutoUtils.isTargetDeclaredType(firstVariableElement, declaredType -> typeUtils.isSameType(declaredType, sessionDeclaredType))) {
				messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method first parameter type must be HttpSession!", method);
				continue;
			}
			final VariableElement secondVariableElement = method.getParameters().get(1);
			// 第二个参数必须是String
			if (!AutoUtils.isTargetDeclaredType(secondVariableElement, declaredType -> typeUtils.isSameType(declaredType, stringDeclaredType))) {
				messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method second parameter type must be String!", method);
				continue;
			}
			final VariableElement thirdVariableElement = method.getParameters().get(2);
			// 第三个参数必须是httpRequestParam
			if (!AutoUtils.isTargetDeclaredType(thirdVariableElement, declaredType -> typeUtils.isSameType(declaredType, paramDeclaredType))) {
				messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method third parameter type must be HttpRequestParam!", method);
				continue;
			}
			// 是否继承父节点路径，如果继承，则使用组合路径，否则使用方法指定的路径
			final boolean inherit = (Boolean) AutoUtils.getAnnotationValue(elementUtils, methodAnnotation.get(), INHERIT_METHOD_NAME);
			String finalPath = inherit ? makePath(parentPath, childPath) : childPath;

			// 生成lambda表达式
			methodBuilder.addStatement("registry.register($S, (httpSession, path, params) -> instance.$L(httpSession, path, params))",
					finalPath,
					method.getSimpleName().toString());
		}

		typeBuilder.addMethod(methodBuilder.build());

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
		} catch (Exception e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "writeToFile caught exception!", typeElement);
		}
	}

	private static String makePath(@Nullable String parentPath, @Nonnull String childPath) {
		// 格式都是 /parent/child
		return parentPath == null ? childPath: parentPath + childPath;
	}

	/**
	 * 检查路径是否合法
	 * @param element 用于编译器定位
	 * @param path http路径
	 */
	private boolean checkPath(Element element, String path) {
		if (path.length() == 0) {
			messager.printMessage(Diagnostic.Kind.ERROR, "path is not allowed empty!", element);
			return false;
		}
		if (path.charAt(0) != PATH_PREFIX) {
			messager.printMessage(Diagnostic.Kind.ERROR, "path must start with '/' !", element);
			return false;
		}
		return true;
	}
}
