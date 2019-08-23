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
import com.wjybxx.fastjgame.eventbus.EventBus;
import com.wjybxx.fastjgame.eventbus.Subscribe;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为带有{@link Subscribe}注解的方法生成代理。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/23
 */
public class EventBusProcessor extends AbstractProcessor {

	// 工具类
	private Messager messager;
	private Types types;
	private Elements elements;
	/** unchecked注解 */
	private AnnotationSpec uncheckedAnnotation;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		types = processingEnv.getTypeUtils();
		elements = processingEnv.getElementUtils();

		uncheckedAnnotation = AnnotationSpec.builder(SuppressWarnings.class)
				.addMember("value", "$S", "unchecked")
				.build();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(Subscribe.class.getCanonicalName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// 只有方法可以带有该注解 METHOD只有普通方法，不包含构造方法， 按照外部类进行分类
		final Map<Element, ? extends List<? extends Element>> collect = roundEnv.getElementsAnnotatedWith(Subscribe.class).stream()
				.filter(element -> ((Element) element).getKind() == ElementKind.METHOD)
				.filter(element -> ((Element) element).getEnclosingElement().getKind() == ElementKind.CLASS)
				.collect(Collectors.groupingBy(element -> ((Element) element).getEnclosingElement()));

		collect.forEach((element, object) -> {
			genProxyClass((TypeElement) element, object);
		});
		return false;
	}

	private void genProxyClass(TypeElement typeElement, List<? extends Element> methodList) {
		final String proxyClassName = typeElement.getSimpleName().toString() + "EventRegister";
		TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(proxyClassName);

		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("register")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(EventBus.class, "bus")
				.addParameter(TypeName.get(typeElement.asType()), "instance");

		for (Element element:methodList) {
			ExecutableElement method = (ExecutableElement) element;
			if (method.getParameters().size() != 1) {
				messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method must have one and only one parameter!", method);
			}
			final VariableElement variableElement = method.getParameters().get(0);
			TypeName typeName = ParameterizedTypeName.get(variableElement.asType());
			if (typeName.isPrimitive()) {
				// 基本类型需要转换为包装类型
				typeName = typeName.box();
			}

			// bus.register(EventA.class, event -> instance.method(event));
			methodBuilder.addStatement("bus.register($T.class, event -> instance.$L(event))", typeName, method.getSimpleName().toString());
		}

		typeBuilder.addMethod(methodBuilder.build());

		TypeSpec typeSpec = typeBuilder.build();
		JavaFile javaFile = JavaFile
				.builder("com.wjybxx.fastjgame.rpcregister", typeSpec)
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
}
