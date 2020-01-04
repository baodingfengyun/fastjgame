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
import javax.lang.model.type.TypeKind;
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

    // 工具类
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

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
        final Map<Element, ? extends List<? extends Element>> class2MethodsMap = roundEnv.getElementsAnnotatedWith(subscribeTypeElement).stream()
                .filter(element -> element.getEnclosingElement().getKind() == ElementKind.CLASS)
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2MethodsMap.forEach((element, object) -> {
            genProxyClass((TypeElement) element, object);
        });
        return true;
    }

    private void genProxyClass(TypeElement typeElement, List<? extends Element> methodList) {
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addMethod(genRegisterMethod(typeElement, methodList));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "BusRegister";
    }

    private MethodSpec genRegisterMethod(TypeElement typeElement, List<? extends Element> methodList) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(registryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (Element element : methodList) {
            final ExecutableElement method = (ExecutableElement) element;

            if (method.getModifiers().contains(Modifier.STATIC)) {
                // 不可以是静态方法
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscribe method can't be static！", method);
                continue;
            }
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                // 访问权限不可以是private - 由于生成的类和该类属于同一个包，因此不必public，只要不是private即可
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscribe method can't be private！", method);
                continue;
            }

            if (method.getParameters().size() != 1 && method.getParameters().size() != 2) {
                // 保证 1 - 2 个参数
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscribe method must have one or two parameter!", method);
                continue;
            }

            // 最后一个参数是事件参数
            final VariableElement eventParameter = method.getParameters().get(method.getParameters().size() - 1);
            if (!isClassOrInterface(eventParameter)) {
                // 事件参数必须是类或接口 (就不会是基本类型或泛型参数了，也排除了数组类型)
                messager.printMessage(Diagnostic.Kind.ERROR, "EventType must be class or interface!", method);
                continue;
            }

            final Set<TypeMirror> eventTypes = collectEventTypes(method, eventParameter);
            // 如果有两个参数，则第一个参数是上下文参数
            final VariableElement contextParameter = method.getParameters().size() == 2 ? method.getParameters().get(0) : null;

            for (TypeMirror typeMirror : eventTypes) {
                final StringBuilder formatBuilder = new StringBuilder("registry.register($T.class, (context, event) -> instance.$L(");
                final List<Object> params = new ArrayList<>(8);
                params.add(TypeName.get(typeUtils.erasure(typeMirror)));
                params.add(method.getSimpleName().toString());

                if (contextParameter != null) {
                    appendContextParam(formatBuilder, params, contextParameter);
                    formatBuilder.append(", ");
                }

                appendEventParam(formatBuilder, params, eventParameter, typeMirror);

                formatBuilder.append("))");

                builder.addStatement(formatBuilder.toString(), params.toArray());
            }
        }
        return builder.build();
    }

    /**
     * 判断是否是接口或类型
     */
    private boolean isClassOrInterface(VariableElement variableElement) {
        return variableElement.asType().getKind() == TypeKind.DECLARED;
    }

    private void appendContextParam(final StringBuilder formatBuilder, final List<Object> params, final VariableElement contextParameter) {
        final TypeName contextTypeName = ParameterizedTypeName.get(contextParameter.asType());
        if (contextTypeName.isPrimitive()) {
            // 基本类型显式拆箱 - 否则会导致重载问题
            formatBuilder.append("($T)(($T)context)");
            params.add(contextTypeName);
            params.add(contextTypeName.box());
        } else {
            formatBuilder.append("($T)context");
            params.add(contextTypeName);
        }
    }

    private void appendEventParam(final StringBuilder formatBuilder, final List<Object> params,
                                  final VariableElement parentEventType, final TypeMirror subEventType) {
        if (isSameTypeIgnoreTypeParameter(parentEventType.asType(), subEventType)) {
            formatBuilder.append("event");
        } else {
            // 子类型需要显示转为超类型 - 否则可能导致重载问题
            final TypeName eventRawTypeName = TypeName.get(getEventRawType(parentEventType));
            formatBuilder.append("($T)event");
            params.add(eventRawTypeName);
        }
    }

    /**
     * 查询是否只监听子类型参数
     */
    private Boolean isOnlySubEvents(AnnotationMirror annotationMirror) {
        return AutoUtils.getAnnotationValue(elementUtils, annotationMirror, ONLY_SUB_EVENTS_METHOD_NAME);
    }

    /**
     * 搜集types属性对应的事件类型
     * 注意查看{@link AnnotationValue}的类文档
     */
    private Set<TypeMirror> collectEventTypes(final ExecutableElement method, final VariableElement eventParameter) {
        final AnnotationMirror annotationMirror = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, subscribeDeclaredType).orElse(null);
        assert annotationMirror != null;

        final Set<TypeMirror> result = new HashSet<>();
        if (!isOnlySubEvents(annotationMirror)) {
            result.add(getEventRawType(eventParameter));
        }

        final List<? extends AnnotationValue> subEventsList = AutoUtils.getAnnotationValueNotDefault(annotationMirror, SUB_EVENTS_METHOD_NAME);
        if (null == subEventsList) {
            return result;
        }

        for (final AnnotationValue annotationValue : subEventsList) {
            final TypeMirror subEventTypeMirror = getSubEventTypeMirror(annotationValue);
            if (null == subEventTypeMirror) {
                // 无法获取参数
                messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported type " + annotationValue, method);
                continue;
            }

            if (!isSubTypeIgnoreTypeParameter(subEventTypeMirror, eventParameter.asType())) {
                // 不是监听参数的子类型 - 带有泛型参数的 isSubType为false
                messager.printMessage(Diagnostic.Kind.ERROR, subEventTypeMirror.toString() + " is not " + getEventRawType(eventParameter).toString() + "'s subType", method);
                continue;
            }

            if (result.stream().anyMatch(typeMirror -> isSameTypeIgnoreTypeParameter(typeMirror, subEventTypeMirror))) {
                // 重复
                messager.printMessage(Diagnostic.Kind.ERROR, "Duplicate type " + subEventTypeMirror.toString(), method);
                continue;
            }

            result.add(typeUtils.erasure(subEventTypeMirror));
        }
        return result;
    }

    private TypeMirror getEventRawType(VariableElement eventParameter) {
        return typeUtils.erasure(eventParameter.asType());
    }

    private static TypeMirror getSubEventTypeMirror(AnnotationValue annotationValue) {
        return annotationValue.accept(new SimpleAnnotationValueVisitor8<TypeMirror, Object>() {
            @Override
            public TypeMirror visitType(TypeMirror t, Object o) {
                return t;
            }

        }, null);
    }

    private boolean isSubTypeIgnoreTypeParameter(TypeMirror a, TypeMirror b) {
        return typeUtils.isSubtype(typeUtils.erasure(a), typeUtils.erasure(b));
    }

    private boolean isSameTypeIgnoreTypeParameter(TypeMirror a, TypeMirror b) {
        return typeUtils.isSameType(typeUtils.erasure(a), typeUtils.erasure(b));
    }

}
