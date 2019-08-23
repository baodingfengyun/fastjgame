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
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分析{@link SerializableField#number()}是否重复，以及是否在 [0,127之间]
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/23
 */
@AutoService(Processor.class)
public class SerializableNumberProcessor extends AbstractProcessor {

	// 工具类
	private Messager messager;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(SerializableClass.class.getCanonicalName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		@SuppressWarnings("unchecked")
		Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(SerializableClass.class);
		for (TypeElement typeElement:typeElementSet) {
			checkNumber(typeElement);
		}
		return false;
	}

	private void checkNumber(TypeElement typeElement) {
		@SuppressWarnings("unchecked")
		final List<VariableElement> serializableFields = (List<VariableElement>) typeElement.getEnclosedElements().stream()
				.filter(element -> element.getKind() == ElementKind.FIELD)
				.filter(element -> element.getAnnotation(SerializableField.class) != null)
				.collect(Collectors.toList());
		IntSet numberSet = new IntOpenHashSet(serializableFields.size());
		for (VariableElement variableElement:serializableFields) {
			final SerializableField annotation = variableElement.getAnnotation(SerializableField.class);
			final int number = annotation.number();
			// 取值范围检测
			if (number <0 || number > 127) {
				messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " must between [0, 127]", variableElement);
			}
			// 重复检测
			if (!numberSet.add(number)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " is duplicate!", variableElement);
			}
		}
	}
}
