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

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
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

    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.EventHandlerRegistry";
    private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.eventbus.Subscribe";

    private static final String SUB_EVENTS_METHOD_NAME = "subEvents";
    private static final String ONLY_SUB_EVENTS_METHOD_NAME = "onlySubEvents";

    private static final String REGISTER_METHOD_NAME = "register";

    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    /**
     * 生成信息
     */
    private AnnotationSpec processorInfoAnnotation;

    private TypeElement subscribeTypeElement;
    private DeclaredType subscribeDeclaredType;

    private TypeName registryTypeName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();

        processorInfoAnnotation = AutoUtils.newProcessorInfoAnnotation(getClass());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(SUBSCRIBE_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return AutoUtils.SOURCE_VERSION;
    }

    /**
     * 尝试初始化环境，也就是依赖的类都已经出现
     */
    private void ensureInited() {
        if (subscribeTypeElement != null) {
            // 已初始化
            return;
        }

        subscribeTypeElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
        subscribeDeclaredType = typeUtils.getDeclaredType(subscribeTypeElement);
        registryTypeName = TypeName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME).asType());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();

        // 只有方法可以带有该注解 METHOD只有普通方法，不包含构造方法， 按照外部类进行分类
        final Map<Element, ? extends List<? extends Element>> class2MethodMap = roundEnv.getElementsAnnotatedWith(subscribeTypeElement).stream()
                .filter(element -> element.getEnclosingElement().getKind() == ElementKind.CLASS)
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2MethodMap.forEach((element, object) -> {
            genProxyClass((TypeElement) element, object);
        });
        return true;
    }

    private void genProxyClass(TypeElement typeElement, List<? extends Element> methodList) {
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(processorInfoAnnotation)
                .addMethod(genRegisterMethod(typeElement, methodList));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, typeUtils, elementUtils, messager, filer);
    }

    private String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "BusRegister";
    }

    private MethodSpec genRegisterMethod(TypeElement typeElement, List<? extends Element> methodList) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(REGISTER_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(registryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (Element element : methodList) {
            final ExecutableElement method = (ExecutableElement) element;
            if (method.getModifiers().contains(Modifier.STATIC)) {
                // 不可以是静态方法
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method can't be static！", method);
                continue;
            }

            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                // 访问权限不可以是private - 因为生成的类和该类属于同一个包，不必public，只要不是private即可
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method can't be private！", method);
                continue;
            }

            if (method.getParameters().size() != 1) {
                // 保证有且仅有一个参数
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method must have one and only one parameter!", method);
                continue;
            }

            final VariableElement variableElement = method.getParameters().get(0);
            if (variableElement.asType().getKind().isPrimitive()) {
                // 参数不可以是基本类型
                messager.printMessage(Diagnostic.Kind.ERROR, "PrimitiveType is not allowed here!", method);
                continue;
            }

            if (AutoUtils.containsTypeVariable(variableElement.asType())) {
                // 参数不可以包含泛型
                messager.printMessage(Diagnostic.Kind.ERROR, "TypeParameter is not allowed here!", method);
                continue;
            }

            // 参数类型是要注册的所有事件类型的超类型
            final TypeName parametersTypeName = ParameterizedTypeName.get(variableElement.asType());
            final Optional<? extends AnnotationMirror> annotationMirror = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, subscribeDeclaredType);
            assert annotationMirror.isPresent();

            final Boolean onlySubEvents = (Boolean) AutoUtils.getAnnotationValue(elementUtils, annotationMirror.get(), ONLY_SUB_EVENTS_METHOD_NAME);
            if (!onlySubEvents) {
                // 注册参数关注的事件类型
                // registry.register(EventA.class, event -> instance.method(event));
                builder.addStatement("registry.register($T.class, event -> instance.$L(event))", parametersTypeName, method.getSimpleName().toString());
            }

            final Set<TypeMirror> subscribeSubTypes = collectTypes(method, variableElement, annotationMirror.get());
            for (TypeMirror subType : subscribeSubTypes) {
                // 注意：这里需要转换为函数参数对应的事件类型（超类型），否则可能会找不到对应的方法，或关联到其它方法
                final TypeName typeName = TypeName.get(subType);
                builder.addStatement("registry.register($T.class, event -> instance.$L(($T)event))", typeName, method.getSimpleName().toString(), parametersTypeName);
            }
        }
        return builder.build();
    }

    /**
     * 搜集types属性对应的事件类型
     * 注意查看{@link AnnotationValue}的类文档
     */
    @SuppressWarnings("unchecked")
    private Set<TypeMirror> collectTypes(final ExecutableElement method, final VariableElement variableElement, AnnotationMirror annotationMirror) {
        final List<? extends AnnotationValue> subEventsList = (List<? extends AnnotationValue>) AutoUtils.getAnnotationValueNotDefault(annotationMirror, SUB_EVENTS_METHOD_NAME);
        if (null == subEventsList) {
            return Collections.emptySet();
        }

        final Set<TypeMirror> result = new HashSet<>();
        for (final AnnotationValue annotationValue : subEventsList) {
            final TypeMirror subEventTypeMirror = getSubEventTypeMirror(annotationValue);

            if (null == subEventTypeMirror) {
                // 无法获取参数
                messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported type " + annotationValue, method);
                continue;
            }

            if (subEventTypeMirror.getKind().isPrimitive()) {
                // 不可以是基本类型
                messager.printMessage(Diagnostic.Kind.ERROR, "PrimitiveType is not allowed here!", method);
                continue;
            }

            if (!typeUtils.isSubtype(subEventTypeMirror, variableElement.asType())) {
                // 不是监听参数的子类型
                messager.printMessage(Diagnostic.Kind.ERROR, SUB_EVENTS_METHOD_NAME + "'s element must be " + variableElement.asType().toString() + "'s subType", method);
                continue;
            }

            result.add(subEventTypeMirror);
        }
        return result;
    }

    private TypeMirror getSubEventTypeMirror(AnnotationValue annotationValue) {
        return annotationValue.accept(new SimpleAnnotationValueVisitor8<TypeMirror, Object>() {
            @Override
            public TypeMirror visitType(TypeMirror t, Object o) {
                return t;
            }
        }, null);
    }

}
