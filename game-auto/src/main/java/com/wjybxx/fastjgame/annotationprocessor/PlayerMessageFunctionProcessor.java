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
import com.google.protobuf.AbstractMessage;
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
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
public class PlayerMessageFunctionProcessor extends AbstractProcessor {

    private static final String SUBSCRIBE_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.PlayerMessageSubscribe";
    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.PlayerMessageFunctionRegistry";
    private static final String FUNCTION_CANONICAL_NAME = "com.wjybxx.fastjgame.misc.PlayerMessageFunction";
    private static final String PLAYER_CANONICAL_NAME = "com.wjybxx.fastjgame.gameobject.Player";

    /** 输出编译信息 */
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    /** 处理器信息 */
    private AnnotationSpec generatedAnnotation;

    private TypeElement registryType;
    private TypeElement functionType;
    private DeclaredType playerType;
    private DeclaredType messageType;

    private TypeName registryTypeName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();

        generatedAnnotation = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", PlayerMessageFunctionProcessor.class.getCanonicalName())
                .build();
        // 不能在这里初始化别的数据，因为可能不存在
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(SUBSCRIBE_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private void init() {
        if (null != registryType) {
            // 已初始化
            return;
        }

        registryType = elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME);
        functionType = elementUtils.getTypeElement(FUNCTION_CANONICAL_NAME);
        playerType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PLAYER_CANONICAL_NAME));
        messageType = typeUtils.getDeclaredType(elementUtils.getTypeElement(AbstractMessage.class.getCanonicalName()));

        registryTypeName = TypeName.get(registryType.asType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement annotationElement = elementUtils.getTypeElement(SUBSCRIBE_CANONICAL_NAME);
        if (null == annotationElement) {
            // 不在scene模块
            return false;
        }
        // 初始化
        init();

        Map<Element, ? extends List<? extends Element>> elementListMap = roundEnv.getElementsAnnotatedWith(annotationElement)
                .stream()
                .filter(element -> ((Element) element).getEnclosingElement().getKind() == ElementKind.CLASS)
                .collect(Collectors.groupingBy(element -> ((Element) element).getEnclosingElement()));

        elementListMap.forEach((type, methods) -> {
            genProxyClass((TypeElement)type, (List<ExecutableElement>) methods);
        });
        return false;
    }

    private void genProxyClass(TypeElement typeElement, List<ExecutableElement> subscribeMethods) {
        final String proxyClassName = typeElement.getSimpleName().toString() + "MsgFunRegister";
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(proxyClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(generatedAnnotation);

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(registryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (Element element:subscribeMethods) {
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
            // 保证必须是两个参数
            if (method.getParameters().size() != 2) {
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method must have two and only two parameter!", method);
                continue;
            }
            // 第一个参数必须是Player类型
            final VariableElement firstVariableElement = method.getParameters().get(0);
            if (!isTargetType(firstVariableElement, declaredType -> typeUtils.isSameType(declaredType, playerType))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method first parameter must be player!", method);
                continue;
            }
            // 第二个参数必须是具体的消息类型
            final VariableElement secondVariableElement = method.getParameters().get(1);
            if (!isTargetType(secondVariableElement, declaredType -> typeUtils.isSubtype(declaredType, messageType))){
                messager.printMessage(Diagnostic.Kind.ERROR, "subscribe method second parameter must be subtype of AbstractMessage!", method);
                continue;
            }
            // 第二个参数类型是要注册的消息类型
            TypeName typeName = ParameterizedTypeName.get(secondVariableElement.asType());
            methodBuilder.addStatement("registry.register($T.class, (player, message) -> instance.$L(player, message))",
                    typeName, method.getSimpleName().toString());
        }

        typeBuilder.addMethod(methodBuilder.build());

        TypeSpec typeSpec = typeBuilder.build();
        JavaFile javaFile = JavaFile
                .builder("com.wjybxx.fastjgame.msgfunregister", typeSpec)
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
     * 判定声明的类型是否是指定类型
     */
    private boolean isTargetType(VariableElement variableElement, Predicate<DeclaredType> matcher) {
        return variableElement.asType().accept(new SimpleTypeVisitor8<Boolean, Void>(){

            @Override
            public Boolean visitDeclared(DeclaredType t, Void aVoid) {
                // 访问声明的类型 eg: String str
                return matcher.test(t);
            }

            @Override
            public Boolean visitTypeVariable(TypeVariable t, Void aVoid) {
                // 泛型变量 eg: E element
                return false;
            }

            @Override
            protected Boolean defaultAction(TypeMirror e, Void aVoid) {
                return false;
            }

        }, null);
    }
}
