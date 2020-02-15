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

package com.wjybxx.fastjgame.apt.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
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

    private static final String HTTP_REQUEST_MAPPING_CANONICAL_NAME = "com.wjybxx.fastjgame.net.annotation.HttpRequestMapping";
    private static final String PARAM_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpRequestParam";
    private static final String SESSION_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpSession";
    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpRequestHandlerRegistry";

    private static final String PATH_METHOD_NAME = "path";
    private static final String INHERIT_METHOD_NAME = "inherit";

    private static final char PATH_PREFIX = '/';

    private Types typeUtils;
    private Elements elementUtils;
    private Messager messager;
    private Filer filer;

    private TypeElement httpRequestMappingElement;
    private DeclaredType httpRequestMappingDeclaredType;

    private DeclaredType sessionDeclaredType;
    private DeclaredType pathDeclaredType;
    private DeclaredType requestParamDeclaredType;

    private TypeName registryTypeName;

    private AnnotationSpec processorInfoAnnotation;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(HTTP_REQUEST_MAPPING_CANONICAL_NAME);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return AutoUtils.SOURCE_VERSION;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();

        processorInfoAnnotation = AutoUtils.newProcessorInfoAnnotation(getClass());
    }

    private void ensureInited() {
        if (null != httpRequestMappingElement) {
            return;
        }
        httpRequestMappingElement = elementUtils.getTypeElement(HTTP_REQUEST_MAPPING_CANONICAL_NAME);
        httpRequestMappingDeclaredType = typeUtils.getDeclaredType(httpRequestMappingElement);

        sessionDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SESSION_CANONICAL_NAME));
        pathDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(String.class.getCanonicalName()));
        requestParamDeclaredType = typeUtils.getDeclaredType(elementUtils.getTypeElement(PARAM_CANONICAL_NAME));

        registryTypeName = TypeName.get(elementUtils.getTypeElement(REGISTRY_CANONICAL_NAME).asType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ensureInited();

        // 该注解可以用在类和方法上，需要注意
        final Map<Element, ? extends List<? extends Element>> class2MethodsMap = roundEnv.getElementsAnnotatedWith(httpRequestMappingElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2MethodsMap.forEach((element, object) -> {
            try {
                genProxyClass((TypeElement) element, (List<ExecutableElement>) object);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.toString(), element);
            }
        });
        return true;
    }

    private void genProxyClass(TypeElement typeElement, List<ExecutableElement> methodList) {
        final String parentPath = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, typeElement, httpRequestMappingDeclaredType)
                .map(annotationMirror -> (String) AutoUtils.getAnnotationValueNotDefault(annotationMirror, PATH_METHOD_NAME))
                .orElse(null);
        // 父路径存在时需要校验
        if (parentPath != null && checkPath(parentPath) != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, checkPath(parentPath), typeElement);
            return;
        }

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getProxyClassName(typeElement))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(processorInfoAnnotation)
                .addMethod(genRegisterMethod(typeElement, methodList, parentPath));

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }

    private MethodSpec genRegisterMethod(TypeElement typeElement, List<ExecutableElement> methodList, String parentPath) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(registryTypeName, "registry")
                .addParameter(TypeName.get(typeElement.asType()), "instance");

        for (ExecutableElement method : methodList) {
            final Optional<? extends AnnotationMirror> methodAnnotation = AutoUtils.findFirstAnnotationWithoutInheritance(typeUtils, method, httpRequestMappingDeclaredType);
            assert methodAnnotation.isPresent();
            final String childPath = AutoUtils.getAnnotationValueNotDefault(methodAnnotation.get(), PATH_METHOD_NAME);
            assert null != childPath;

            // 路径检查
            if (checkPath(childPath) != null) {
                messager.printMessage(Diagnostic.Kind.ERROR, checkPath(childPath), method);
                continue;
            }

            // 不可以是静态方法
            if (method.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method can't be static！", method);
                continue;
            }
            // 访问权限不可以是private
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method can't be private！", method);
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
            if (!AutoUtils.isTargetDeclaredType(secondVariableElement, declaredType -> typeUtils.isSameType(declaredType, pathDeclaredType))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method second parameter type must be String!", method);
                continue;
            }
            final VariableElement thirdVariableElement = method.getParameters().get(2);
            // 第三个参数必须是httpRequestParam
            if (!AutoUtils.isTargetDeclaredType(thirdVariableElement, declaredType -> typeUtils.isSameType(declaredType, requestParamDeclaredType))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method third parameter type must be HttpRequestParam!", method);
                continue;
            }

            // 是否继承父节点路径，如果继承，则使用组合路径，否则使用方法指定的路径
            final Boolean inherit = AutoUtils.getAnnotationValueWithDefaults(elementUtils, methodAnnotation.get(), INHERIT_METHOD_NAME);
            final String finalPath = inherit ? makePath(parentPath, childPath) : childPath;

            // 生成lambda表达式
            builder.addStatement("registry.register($S, (httpSession, path, params) -> instance.$L(httpSession, path, params))",
                    finalPath, method.getSimpleName().toString());
        }
        return builder.build();
    }

    private String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "HttpRegister";
    }

    private static String makePath(@Nullable String parentPath, @Nonnull String childPath) {
        // 格式都是 /parent/child
        return parentPath == null ? childPath : parentPath + childPath;
    }

    /**
     * 检查路径是否合法
     *
     * @param path http路径
     * @return errorMessage
     */
    @Nullable
    private String checkPath(String path) {
        if (path.length() == 0) {
            return "path is not allowed empty!";
        }
        if (path.charAt(0) != PATH_PREFIX) {
            return "path must start with '/' !";
        }
        return null;
    }
}
