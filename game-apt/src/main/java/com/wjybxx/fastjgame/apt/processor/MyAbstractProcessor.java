package com.wjybxx.fastjgame.apt.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * 自定义注解处理器基类，指定生成代码版本，指定处理流程，消除重复模板式代码。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/17
 * github - https://github.com/hl845740757
 */
public abstract class MyAbstractProcessor extends AbstractProcessor {

    protected Types typeUtils;
    protected Elements elementUtils;
    protected Messager messager;
    protected Filer filer;
    protected AnnotationSpec processorInfoAnnotation;

    @Override
    public final synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
        processorInfoAnnotation = AutoUtils.newProcessorInfoAnnotation(getClass());
    }

    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return AutoUtils.SOURCE_VERSION;
    }

    @Override
    public abstract Set<String> getSupportedAnnotationTypes();

    /**
     * 只有代码中出现指定的注解时才会走到该方法，而{@link #init(ProcessingEnvironment)}每次编译都会执行，不论是否出现指定注解。
     */
    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            ensureInited();
        } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, AutoUtils.getStackTrace(e));
        }
        try {
            return doProcess(annotations, roundEnv);
        } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.ERROR, AutoUtils.getStackTrace(e));
            return false;
        }
    }

    /**
     * 确保完成了初始化
     */
    protected abstract void ensureInited();

    /**
     * 如果返回true，表示注解已经被认领，并且不会要求后续处理器处理它们;
     * 如果返回false，表示注解类型无人认领，并且可能要求后续处理器处理它们。 处理器可以始终返回相同的布尔值，或者可以基于所选择的标准改变结果。
     */
    protected abstract boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
}
