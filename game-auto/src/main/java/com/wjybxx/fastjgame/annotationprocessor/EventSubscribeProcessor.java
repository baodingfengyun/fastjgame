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
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
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

    private static final String HANDLER_REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.EventHandlerRegistry";
    private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.Subscribe";
    private static final String SUB_EVENTS_METHOD_NAME = "subEvents";

    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;

    /**
     * 生成信息
     */
    private AnnotationSpec generatedAnnotation;

    /**
     * {@code Subscribe对应的注解元素}
     */
    private TypeElement subscribeElement;
    private TypeName eventRegistryTypeName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();

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
        if (subscribeElement != null) {
            // 已初始化
            return;
        }

        subscribeElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
        eventRegistryTypeName = TypeName.get(elementUtils.getTypeElement(HANDLER_REGISTRY_CANONICAL_NAME).asType());
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
                .addParameter(eventRegistryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (Element element : methodList) {
            ExecutableElement method = (ExecutableElement) element;
            // 访问权限不可以是private - 因为生成的类和该类属于同一个包，不必public，只要不是private即可
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method can't be private！", method);
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
            // 注册参数关注的事件类型
            final TypeName parametersTypeName = ParameterizedTypeName.get(variableElement.asType());
            // registry.register(EventA.class, event -> instance.method(event));
            methodBuilder.addStatement("registry.register($T.class, event -> instance.$L(event))", parametersTypeName, method.getSimpleName().toString());

            final Set<TypeMirror> subscribeSubTypes = collectTypes(method, variableElement);
            for (TypeMirror subType : subscribeSubTypes) {
                // 注意：这里需要转换为函数参数对应的事件类型（超类型），否则可能会找不到对应的方法，或关联到其它方法
                final TypeName typeName = TypeName.get(subType);
                methodBuilder.addStatement("registry.register($T.class, event -> instance.$L(($T)event))", typeName, method.getSimpleName().toString(), parametersTypeName);
            }
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
     * 搜集types属性对应的事件类型
     */
    private Set<TypeMirror> collectTypes(final ExecutableElement method, final VariableElement variableElement) {
        final List<?> typeArray = (List<?>) AutoUtils.getAnnotationValueNotDefault(method.getAnnotationMirrors().get(0), SUB_EVENTS_METHOD_NAME);
        if (null == typeArray) {
            return Collections.emptySet();
        }

        final Set<TypeMirror> result = new HashSet<>();
        for (Object typeBean : typeArray) {
            final String typeName = typeBean.toString().replace(".class", "");
            final TypeElement typeElement = elementUtils.getTypeElement(typeName);
            if (null == typeElement) {
                // 基本类型为null
                messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported type " + typeName, method);
                continue;
            }

            if (!typeUtils.isSubtype(typeElement.asType(), variableElement.asType())) {
                // 不是监听参数的子类型
                messager.printMessage(Diagnostic.Kind.ERROR, SUB_EVENTS_METHOD_NAME + "'s element must be " + variableElement.asType().toString() + "'s subType", method);
                continue;
            }
            result.add(typeElement.asType());
        }
        return result;
    }

    /**
     * 判断一个变量的声明类型是否包含泛型
     */
    private boolean containsTypeVariable(final VariableElement variableElement) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>() {

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
        }, null);
    }
}
