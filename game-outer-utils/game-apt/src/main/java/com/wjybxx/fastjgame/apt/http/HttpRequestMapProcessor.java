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

package com.wjybxx.fastjgame.apt.http;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wjybxx.fastjgame.apt.core.MyAbstractProcessor;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
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
public class HttpRequestMapProcessor extends MyAbstractProcessor {

    private static final String HTTP_REQUEST_MAPPING_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpRequestMapping";
    private static final String PARAM_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpRequestParam";
    private static final String SESSION_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpSession";
    private static final String REGISTRY_CANONICAL_NAME = "com.wjybxx.fastjgame.net.http.HttpRequestHandlerRegistry";

    private static final String PATH_METHOD_NAME = "path";
    private static final String INHERIT_METHOD_NAME = "inherit";

    private static final char PATH_PREFIX = '/';

    private TypeElement httpRequestMappingElement;
    private DeclaredType httpRequestMappingDeclaredType;

    private DeclaredType sessionDeclaredType;
    private DeclaredType pathDeclaredType;
    private DeclaredType requestParamDeclaredType;

    private TypeName registryTypeName;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(HTTP_REQUEST_MAPPING_CANONICAL_NAME);
    }

    @Override
    protected void ensureInited() {
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
    protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 该注解可以用在类和方法上，需要注意
        final Map<Element, ? extends List<? extends Element>> class2MethodsMap = roundEnv.getElementsAnnotatedWith(httpRequestMappingElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .collect(Collectors.groupingBy(Element::getEnclosingElement));

        class2MethodsMap.forEach((typeElement, methodElementList) -> {
            try {
                genProxyClass((TypeElement) typeElement, (List<ExecutableElement>) methodElementList);
            } catch (Throwable e) {
                messager.printMessage(Diagnostic.Kind.ERROR, AutoUtils.getStackTrace(e), typeElement);
            }
        });
        return true;
    }

    private void genProxyClass(TypeElement typeElement, List<ExecutableElement> methodList) {
        final String parentPath = AutoUtils.findAnnotation(typeUtils, typeElement, httpRequestMappingDeclaredType)
                .map(annotationMirror -> (String) AutoUtils.getAnnotationValueValue(annotationMirror, PATH_METHOD_NAME))
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
            final Optional<? extends AnnotationMirror> methodAnnotation = AutoUtils.findAnnotation(typeUtils, method, httpRequestMappingDeclaredType);
            assert methodAnnotation.isPresent();
            final String childPath = AutoUtils.getAnnotationValueValue(methodAnnotation.get(), PATH_METHOD_NAME);
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
            if (!isHttpSessionVariable(firstVariableElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method first parameter type must be HttpSession!", method);
                continue;
            }

            final VariableElement secondVariableElement = method.getParameters().get(1);
            // 第二个参数必须是String
            if (!isStringVariable(secondVariableElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method second parameter type must be String!", method);
                continue;
            }

            final VariableElement thirdVariableElement = method.getParameters().get(2);
            // 第三个参数必须是httpRequestParam
            if (!isHttpRequestParamVariable(thirdVariableElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "HttpRequestMapping method third parameter type must be HttpRequestParam!", method);
                continue;
            }

            // 是否继承父节点路径，如果继承，则使用组合路径，否则使用方法指定的路径
            final Boolean inherit = AutoUtils.getAnnotationValueValueWithDefaults(elementUtils, methodAnnotation.get(), INHERIT_METHOD_NAME);
            final String finalPath = inherit ? makePath(parentPath, childPath) : childPath;

            // 生成lambda表达式
            builder.addStatement("registry.register($S, (httpSession, path, params) -> instance.$L(httpSession, path, params))",
                    finalPath, method.getSimpleName().toString());
        }
        return builder.build();
    }

    private boolean isHttpSessionVariable(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), sessionDeclaredType);
    }

    private boolean isStringVariable(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), pathDeclaredType);
    }

    private boolean isHttpRequestParamVariable(VariableElement variableElement) {
        return AutoUtils.isSameTypeIgnoreTypeParameter(typeUtils, variableElement.asType(), requestParamDeclaredType);
    }

    private static String getProxyClassName(TypeElement typeElement) {
        return typeElement.getSimpleName().toString() + "HttpRegister";
    }

    private static String makePath(@Nullable String parentPath, @Nonnull String childPath) {
        // 格式都是 /parent/child
        if (parentPath == null || parentPath.equals("/")) {
            return childPath;
        }
        if (parentPath.endsWith("/")) {
            return parentPath + childPath.substring(1);
        } else {
            return parentPath + childPath;
        }
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
