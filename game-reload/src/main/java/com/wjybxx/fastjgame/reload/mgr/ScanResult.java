/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.reload.mgr;

import com.wjybxx.fastjgame.reload.excel.SheetCacheBuilder;
import com.wjybxx.fastjgame.reload.excel.SheetReader;
import com.wjybxx.fastjgame.reload.file.FileCacheBuilder;
import com.wjybxx.fastjgame.reload.file.FileReader;
import com.wjybxx.fastjgame.util.ClassScanner;
import com.wjybxx.fastjgame.util.function.FunctionUtils;
import com.wjybxx.fastjgame.util.misc.StepWatch;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 注意：只有满足我们契约的类会被加入。
 * 只有<b>有且只有一个无参构造方法的具体类</b>会被加入，其它类都不会被加入。
 * 1. 抽象类和接口不会被加入。
 * 2. 不包含无参构造方法或包含多个构造方法的类不会被加入。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/11/30
 * github - https://github.com/hl845740757
 */
public class ScanResult {

    private static final Logger logger = LoggerFactory.getLogger(ScanResult.class);

    public final Collection<FileReader<?>> fileReaders;
    public final Collection<FileCacheBuilder<?>> fileCacheBuilders;

    public final Collection<SheetReader<?>> sheetReaders;
    public final Collection<SheetCacheBuilder<?>> sheetCacheBuilders;

    private ScanResult(Collection<FileReader<?>> fileReaders,
                       Collection<FileCacheBuilder<?>> fileCacheBuilders,
                       Collection<SheetReader<?>> sheetReaders,
                       Collection<SheetCacheBuilder<?>> sheetCacheBuilders) {
        this.fileReaders = List.copyOf(fileReaders);
        this.fileCacheBuilders = List.copyOf(fileCacheBuilders);
        this.sheetReaders = List.copyOf(sheetReaders);
        this.sheetCacheBuilders = List.copyOf(sheetCacheBuilders);
    }

    public static ScanResult valueOf(Set<String> readerPackages) throws Exception {
        final StepWatch stepWatch = StepWatch.createStarted("ScanResult:valueOf");
        final List<Class<?>> classList = scanPackages(readerPackages);
        stepWatch.logStep("scanPackages");

        final Collection<FileReader<?>> fileReaders = new ArrayList<>(500);
        final Collection<FileCacheBuilder<?>> fileCacheBuilders = new ArrayList<>(50);
        final Collection<SheetReader<?>> sheetReaders = new ArrayList<>(500);
        final Collection<SheetCacheBuilder<?>> sheetCacheBuilders = new ArrayList<>(50);

        for (Class<?> clazz : classList) {
            final Object instance = createInstance(clazz);
            if (logger.isDebugEnabled()) {
                logger.debug("find class: " + clazz.getName());
            }
            if (instance instanceof FileReader) {
                fileReaders.add((FileReader<?>) instance);
                continue;
            }
            if (instance instanceof FileCacheBuilder) {
                fileCacheBuilders.add((FileCacheBuilder<?>) instance);
                continue;
            }
            if (instance instanceof SheetReader) {
                sheetReaders.add((SheetReader<?>) instance);
            } else {
                sheetCacheBuilders.add((SheetCacheBuilder<?>) instance);
            }
        }
        stepWatch.logStep("createInstances");
        logger.info(stepWatch.getLog());
        return new ScanResult(fileReaders, fileCacheBuilders, sheetReaders, sheetCacheBuilders);
    }

    private static List<Class<?>> scanPackages(Set<String> readerPackages) {
        if (readerPackages.isEmpty()) {
            throw new IllegalArgumentException("packages is empty");
        }
        return readerPackages.stream()
                .flatMap(pkg -> ClassScanner.findClasses(pkg, FunctionUtils.alwaysTrue(), ScanResult::isReaderOrBuilder).stream())
                .collect(Collectors.toList());
    }

    private static boolean isReaderOrBuilder(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        if (FileReader.class.isAssignableFrom(clazz) || FileCacheBuilder.class.isAssignableFrom(clazz)
                || SheetReader.class.isAssignableFrom(clazz) || SheetCacheBuilder.class.isAssignableFrom(clazz)) {
            // 必须只有一个无参方法，否则不自动加入，不满足我们的契约
            final Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            return (declaredConstructors.length == 1) && (declaredConstructors[0].getParameterCount() == 0);
        } else {
            return false;
        }
    }

    private static Object createInstance(Class<?> clazz) throws Exception {
        final Constructor<?> constructor = clazz.getDeclaredConstructor(ArrayUtils.EMPTY_CLASS_ARRAY);
        constructor.setAccessible(true);
        return constructor.newInstance(ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

}
