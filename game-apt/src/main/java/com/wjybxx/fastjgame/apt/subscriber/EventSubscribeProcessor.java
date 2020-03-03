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

package com.wjybxx.fastjgame.apt.subscriber;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wjybxx.fastjgame.apt.core.MyAbstractProcessor;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
public class EventSubscribeProcessor extends MyAbstractProcessor {

    private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.eventbus.Subscribe";
    private static final String GENERIC_EVENT_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.eventbus.GenericEvent";

    private static final String HANDLER_REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.eventbus.EventHandlerRegistry";
    private static final String BUS_REGISTER_CANONICAL_NAME = "com.wjybxx.fastjgame.utils.eventbus.BusRegister";

    private static final String SUB_EVENTS_METHOD_NAME = "subEvents";
    private static final String ONLY_SUB_EVENTS_METHOD_NAME = "onlySubEvents";

    private TypeElement subscribeTypeElement;
    private DeclaredType subscribeDeclaredType;
    private DeclaredType genericEventDeclaredType;

    private TypeName busRegisterTypeName;
    private TypeName handlerRegistryTypeName;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(SUBSCRIBE_CANONICAL_NAME);
    }

    /**
     * 尝试初始化环境，也就是依赖的类都已经出现
     */
    @Override
    protected void ensureInited() {
        if (subscribeTypeElement != null) {
            // 已初始化
            return;
        }

        subscribeTypeElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
        subscribeDeclaredType = typeUtils.getDeclaredType(subscribeTypeElement);
        genericEventDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(GENERIC_EVENT_CANONICAL_NAME));

        busRegisterTypeName = ClassName.get(elementUtils.getTypeElement(BUS_REGISTER_CANONICAL_NAME));
        handlerRegistryTypeName = ClassName.get(elementUtils.getTypeElement(HANDLER_REGISTRY_CANONICAL_NAME));
    }

    @Override
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 只有方法可以带有该注解 METHOD只有普通方法，不包含构造方法， 按照外部类进行分类
        final Map<Element, ? extends List<? extends Element>> class2MethodsMap = roundEnv.getElementsAnnotatedWith(subscribeTypeElement).stream()
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2MethodsMap.forEach((element, object) -> {
            try {
                genProxyClass((TypeElement) element, object);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, AutoUtils.getStackTrace(e), element);
            }
        });
        return true;
    }

    private void genProxyClass(TypeElement typeElement, List<? extends Element> methodList) {
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addSuperinterface(busRegisterTypeName)
                .addMethod(genRegisterMethod(typeElement, methodList));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private static String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "BusRegister";
    }

    private MethodSpec genRegisterMethod(TypeElement typeElement, List<? extends Element> methodList) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(handlerRegistryTypeName, "registry")
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

            if (method.getParameters().size() != 1) {
                // 保证 1个参数
                messager.printMessage(Diagnostic.Kind.ERROR, "Subscribe method must have one and only one parameter!", method);
                continue;
            }

            final VariableElement eventParameter = method.getParameters().get(0);
            if (!isClassOrInterface(eventParameter.asType())) {
                // 事件参数必须是类或接口 (就不会是基本类型或泛型参数了，也排除了数组类型)
                messager.printMessage(Diagnostic.Kind.ERROR, "EventType must be class or interface!", method);
                continue;
            }

            if (isGenericEvent(eventParameter)) {
                registerGenericHandlers(builder, method);
            } else {
                registerNormalHandlers(builder, method);
            }
        }

        return builder.build();
    }

    /**
     * 判断是否是接口或类型
     */
    private static boolean isClassOrInterface(TypeMirror typeMirror) {
        return typeMirror.getKind() == TypeKind.DECLARED;
    }

    /**
     * 判断监听的是否是泛型事件类型
     */
    private boolean isGenericEvent(VariableElement eventParameter) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, eventParameter.asType(), genericEventDeclaredType);
    }

    /**
     * 注册泛型事件处理器
     */
    private void registerGenericHandlers(MethodSpec.Builder builder, ExecutableElement method) {
        final VariableElement eventParameter = method.getParameters().get(0);
        final TypeName parentEventRawTypeName = TypeName.get(erasure(eventParameter.asType()));
        final Set<TypeMirror> eventTypes = collectEventTypes(method, getGenericEventTypeArgument(eventParameter));

        for (TypeMirror typeMirror : eventTypes) {
            builder.addStatement("registry.register($T.class, $T.class, event -> instance.$L(event))",
                    parentEventRawTypeName,
                    TypeName.get(erasure(typeMirror)),
                    method.getSimpleName().toString());
        }
    }

    /**
     * 注册普通事件处理器
     */
    private void registerNormalHandlers(MethodSpec.Builder builder, ExecutableElement method) {
        final VariableElement eventParameter = method.getParameters().get(0);
        final TypeName parentEventRawTypeName = TypeName.get(erasure(eventParameter.asType()));
        final Set<TypeMirror> eventTypes = collectEventTypes(method, eventParameter.asType());

        for (TypeMirror typeMirror : eventTypes) {
            if (isSameTypeIgnoreTypeParameter(typeMirror, eventParameter.asType())) {
                // 声明类型
                builder.addStatement("registry.register($T.class, event -> instance.$L(event))",
                        parentEventRawTypeName,
                        method.getSimpleName().toString());
            } else {
                // 子类型需要显示转为超类型 - 否则可能导致重载问题
                builder.addStatement("registry.register($T.class, event -> instance.$L(($T) event))",
                        TypeName.get(erasure(typeMirror)),
                        method.getSimpleName().toString(),
                        parentEventRawTypeName);
            }
        }
    }

    /**
     * 查询是否只监听子类型参数
     */
    private Boolean isOnlySubEvents(AnnotationMirror annotationMirror) {
        return AutoUtils.getAnnotationValueValueWithDefaults(elementUtils, annotationMirror, ONLY_SUB_EVENTS_METHOD_NAME);
    }

    /**
     * 搜集types属性对应的事件类型
     * 注意查看{@link AnnotationValue}的类文档
     */
    private Set<TypeMirror> collectEventTypes(final ExecutableElement method, final TypeMirror parentTypeMirror) {
        final AnnotationMirror annotationMirror = AutoUtils.findAnnotation(typeUtils, method, subscribeDeclaredType)
                .orElseThrow();

        final Set<TypeMirror> result = new HashSet<>();
        if (!isOnlySubEvents(annotationMirror)) {
            result.add(parentTypeMirror);
        }

        final List<? extends AnnotationValue> subEventsList = AutoUtils.getAnnotationValueValue(annotationMirror, SUB_EVENTS_METHOD_NAME);
        if (null == subEventsList) {
            return result;
        }

        for (final AnnotationValue annotationValue : subEventsList) {
            final TypeMirror subEventTypeMirror = AutoUtils.getAnnotationValueTypeMirror(annotationValue);
            if (null == subEventTypeMirror) {
                // 无法获取参数
                messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported type " + annotationValue, method);
                continue;
            }

            if (!isSubTypeOfEventParent(subEventTypeMirror, parentTypeMirror)) {
                // 不是监听参数的子类型 - 带有泛型参数的 isSubType为false
                messager.printMessage(Diagnostic.Kind.ERROR, subEventTypeMirror.toString() + " is not " + parentTypeMirror.toString() + "'s subType", method);
                continue;
            }

            if (isResultContains(result, subEventTypeMirror)) {
                // 重复
                messager.printMessage(Diagnostic.Kind.ERROR, "Duplicate type " + subEventTypeMirror.toString(), method);
                continue;
            }

            result.add(subEventTypeMirror);
        }
        return result;
    }

    private boolean isResultContains(Set<TypeMirror> result, TypeMirror subEventTypeMirror) {
        return result.stream().anyMatch(typeMirror -> isSameTypeIgnoreTypeParameter(typeMirror, subEventTypeMirror));
    }

    private TypeMirror erasure(TypeMirror eventTypeMirror) {
        return typeUtils.erasure(eventTypeMirror);
    }

    private boolean isSubTypeOfEventParent(TypeMirror subType, TypeMirror parent) {
        return AutoUtils.isSubTypeIgnoreTypeParameter(typeUtils, subType, parent);
    }

    private boolean isSameTypeIgnoreTypeParameter(TypeMirror a, TypeMirror b) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, a, b);
    }

    /**
     * 获取泛型事件的泛型参数
     */
    private TypeMirror getGenericEventTypeArgument(final VariableElement genericEventVariableElement) {
        final TypeMirror result = AutoUtils.findFirstParameterActualType(genericEventVariableElement.asType());
        if (result == null || result.getKind() != TypeKind.DECLARED) {
            messager.printMessage(Diagnostic.Kind.ERROR, "GenericEvent has a bad type parameter!", genericEventVariableElement);
        }
        return result;
    }
}
