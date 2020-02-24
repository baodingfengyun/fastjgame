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

package com.wjybxx.fastjgame.apt.serializer;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wjybxx.fastjgame.apt.core.AbstractGenerator;
import com.wjybxx.fastjgame.apt.utils.AutoUtils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static com.wjybxx.fastjgame.apt.serializer.SerializableClassProcessor.getSerializerClassName;
import static com.wjybxx.fastjgame.apt.utils.BeanUtils.FOR_INDEX_METHOD_NAME;
import static com.wjybxx.fastjgame.apt.utils.BeanUtils.GET_INDEX_METHOD_NAME;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 */
class IndexableEntitySerializerGenerator extends AbstractGenerator<SerializableClassProcessor> {

    IndexableEntitySerializerGenerator(SerializableClassProcessor processor, TypeElement typeElement) {
        super(processor, typeElement);
    }

    @Override
    public void execute() {
        gen();
    }

    private void gen() {
        final TypeName instanceRawTypeName = TypeName.get(typeUtils.erasure(typeElement.asType()));
        final DeclaredType superDeclaredType = typeUtils.getDeclaredType(processor.serializerTypeElement, typeUtils.erasure(typeElement.asType()));

        final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(getSerializerClassName(typeElement));
        final CodeBlock.Builder staticCodeBlockBuilder = CodeBlock.builder();

        // 获取实例方法
        final MethodSpec getEntityMethod = processor.newGetEntityMethod(superDeclaredType);

        // 写入索引即可 outputStream.writeObject(instance.getIndex())
        final MethodSpec.Builder writeMethodBuilder = processor.newWriteMethodBuilder(superDeclaredType);
        writeMethodBuilder.addStatement("outputStream.writeObject(instance.$L())", GET_INDEX_METHOD_NAME);

        // 读取索引即可 return A.forIndex(InputStream.readObject());
        final MethodSpec.Builder readMethodBuilder = processor.newReadObjectMethodBuilder(superDeclaredType);
        readMethodBuilder.addStatement("return $T.$L(inputStream.readObject())", instanceRawTypeName, FOR_INDEX_METHOD_NAME);

        typeBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AutoUtils.SUPPRESS_UNCHECKED_ANNOTATION)
                .addAnnotation(processorInfoAnnotation)
                .addStaticBlock(staticCodeBlockBuilder.build())
                .addSuperinterface(TypeName.get(superDeclaredType))
                .addMethod(getEntityMethod)
                .addMethod(writeMethodBuilder.build())
                .addMethod(readMethodBuilder.build());

        // 写入文件
        AutoUtils.writeToFile(typeElement, typeBuilder, elementUtils, messager, filer);
    }
}
