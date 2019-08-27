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

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为带有{@code Subscribe}注解的方法生成代理。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class EventSubscribeProcessor extends AbstractProcessor {

	private static final String EVENT_BUS_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.EventBus";
	private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.Subscribe";

	// 工具类
	private Messager messager;
	private Elements elementUtils;

	/** 生成信息 */
	private AnnotationSpec generatedAnnotation;

	/** {@code Subscribe对应的注解元素} */
	private TypeElement subscribeElement;
	private TypeName eventBusTypeName;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		elementUtils = processingEnv.getElementUtils();

		generatedAnnotation = AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", EventSubscribeProcessor.class.getCanonicalName())
				.build();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(SUBSCRIBE_CANONICAL_NAME);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	/**
	 * 尝试初始化环境，也就是依赖的类都已经出现
	 */
	private void ensureInited() {
		if (subscribeElement != null){
			// 已初始化
			return;
		}

		subscribeElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
		eventBusTypeName = TypeName.get(elementUtils.getTypeElement(EVENT_BUS_CANONICAL_NAME).asType());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ensureInited();

		// 只有方法可以带有该注解 METHOD只有普通方法，不包含构造方法， 按照外部类进行分类
		final Map<Element, ? extends List<? extends Element>> collect = roundEnv.getElementsAnnotatedWith(subscribeElement).stream()
				.filter(element -> ((Element) element).getEnclosingElement().getKind() == ElementKind.CLASS)
				.collect(Collectors.groupingBy(element -> ((Element) element).getEnclosingElement()));

		collect.forEach((element, object) -> {
			genProxyClass((TypeElement) element, object);
		});
		return true;
	}

	private void genProxyClass(TypeElement typeElement, List<? extends Element> methodList) {
		final String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
		final String proxyClassName = typeElement.getSimpleName().toString() + "BusRegister";
		TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(proxyClassName)
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addAnnotation(generatedAnnotation);

		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("register")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(eventBusTypeName, "bus")
				.addParameter(TypeName.get(typeElement.asType()), "instance");

		for (Element element:methodList) {
			ExecutableElement method = (ExecutableElement) element;
			// 访问权限必须是public
			if (!method.getModifiers().contains(Modifier.PUBLIC)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be public！", method);
				continue;
			}
			// 返回值类型必须是void
			if (method.getReturnType().getKind() != TypeKind.VOID) {
				messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType must be void!", method);
				continue;
			}
			// 保证有且仅有一个参数
			if (method.getParameters().size() != 1) {
				messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method must have one and only one parameter!", method);
				continue;
			}
			final VariableElement variableElement = method.getParameters().get(0);
			// 参数不可以是基本类型
			if (variableElement.asType().getKind().isPrimitive()) {
				messager.printMessage(Diagnostic.Kind.ERROR, "PrimitiveType is not allowed here!", method);
				continue;
			}
			// 参数不可以包含泛型
			if (containsTypeVariable(variableElement)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "TypeParameter is not allowed here!", method);
				continue;
			}
			TypeName typeName = ParameterizedTypeName.get(variableElement.asType());
			// bus.register(EventA.class, event -> instance.method(event));
			methodBuilder.addStatement("bus.register($T.class, event -> instance.$L(event))", typeName, method.getSimpleName().toString());
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

	/**
	 * 判断一个变量的声明类型是否包含泛型
	 */
	private boolean containsTypeVariable(final VariableElement variableElement) {
		return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>(){

			@Override
			public Boolean visitDeclared(DeclaredType t, Void aVoid) {
				// eg:
				// method(Set<String> value)
				return t.getTypeArguments().size() > 0;
			}

			@Override
			public Boolean visitTypeVariable(TypeVariable t, Void aVoid) {
				// eg:
				// method(T value)
				return true;
			}

			@Override
			protected Boolean defaultAction(TypeMirror e, Void aVoid) {
				return false;
			}
		},  null);
	}
}
