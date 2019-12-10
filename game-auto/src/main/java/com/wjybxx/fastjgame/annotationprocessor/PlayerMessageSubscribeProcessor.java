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
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PlayerMessageSubscribe注解处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/25
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class PlayerMessageSubscribeProcessor extends AbstractProcessor {

    private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.PlayerMessageSubscribe";
    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.PlayerMessageFunctionRegistry";
    private static final String PLAYER_CANONICAL_NAME = "com.wjybxx.fastjgame.gameobject.Player";

    /**
     * 输出编译信息
     */
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    private AnnotationSpec processorInfoAnnotation;

    private TypeElement subscribeElement;
    private DeclaredType subscribeDeclaredType;

    private DeclaredType playerType;

    private TypeName registryTypeName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
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

    private void ensureInited() {
        // 已初始化
        if (null != subscribeElement) {
            return;
        }

        subscribeElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
        subscribeDeclaredType = typeUtils.getDeclaredType(subscribeElement);

        playerType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PLAYER_CANONICAL_NAME));

        registryTypeName = TypeName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME).asType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();

        Map<Element, ? extends List<? extends Element>> class2methodsMap = roundEnv.getElementsAnnotatedWith(subscribeElement).stream()
                .filter(element -> element.getEnclosingElement().getKind() == ElementKind.CLASS)
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2methodsMap.forEach((type, methods) -> {
            genProxyClass((TypeElement) type, (List<ExecutableElement>) methods);
        });
        return false;
    }

    private void genProxyClass(TypeElement typeElement, List<ExecutableElement> subscribeMethods) {
        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(processorInfoAnnotation)
                .addMethod(genRegisterMethod(typeElement, subscribeMethods));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private MethodSpec genRegisterMethod(TypeElement typeElement, List<ExecutableElement> subscribeMethods) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(registryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (Element element : subscribeMethods) {
            final ExecutableElement method = (ExecutableElement) element;

            if (method.getModifiers().contains(Modifier.STATIC)) {
                // 不可以是静态方法
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method can't be static！", method);
                continue;
            }
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                // 访问权限不可以是private
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method can't be private！", method);
                continue;
            }

            if (method.getParameters().size() != 2) {
                // 保证必须是两个参数
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method must have two and only two parameter!", method);
                continue;
            }

            final VariableElement firstVariableElement = method.getParameters().get(0);
            if (!AutoUtils.isTargetDeclaredType(firstVariableElement, declaredType -> typeUtils.isSameType(declaredType, playerType))) {
                // 第一个参数必须是Player类型
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method first parameter type must be player!", method);
                continue;
            }

            // 第二个参数是要注册的所有子消息类型的超类型
            final VariableElement secondVariableElement = method.getParameters().get(1);
            if (secondVariableElement.asType().getKind().isPrimitive()) {
                // 消息类型不可以是基本类型
                messager.printMessage(Diagnostic.Kind.ERROR, "PrimitiveType is not allowed here!", method);
                continue;
            }
            if (AutoUtils.containsTypeVariable(secondVariableElement.asType())) {
                // 消息类型不可以包含泛型
                messager.printMessage(Diagnostic.Kind.ERROR, "TypeParameter is not allowed here!", method);
                continue;
            }

            final AnnotationMirror annotationMirror = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, subscribeDeclaredType).get();
            final Boolean onlySubEvents = EventSubscribeProcessor.isOnlySubEvents(elementUtils, annotationMirror);

            final TypeName messageTypeName = ParameterizedTypeName.get(secondVariableElement.asType());
            if (!onlySubEvents) {
                builder.addStatement("registry.register($T.class, (player, message) -> instance.$L(player, message))",
                        messageTypeName, method.getSimpleName().toString());
            }

            // 搜集注册的子事件
            final Set<TypeMirror> subscribeSubTypes = EventSubscribeProcessor.collectSubEventTypes(typeUtils, messager, method,
                    secondVariableElement.asType(), annotationMirror);

            for (TypeMirror subType : subscribeSubTypes) {
                if (!onlySubEvents && typeUtils.isSameType(subType, secondVariableElement.asType())) {
                    // 去除重复
                    continue;
                }

                // 注意：这里需要转换为函数参数对应的事件类型（超类型），否则可能会找不到对应的方法，或关联到其它方法
                // registry.register(Child.class, event -> instance.method(player, (Parent)event));
                builder.addStatement("registry.register($T.class, (player, message) -> instance.$L(player, ($T)message))",
                        TypeName.get(subType), method.getSimpleName().toString(), messageTypeName);
            }

        }
        return builder.build();
    }

    private String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "PlayerMsgRegister";
    }

}
