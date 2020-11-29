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

import com.google.common.collect.Maps;
import com.wjybxx.fastjgame.agent.ClassReloadAgent;
import com.wjybxx.fastjgame.reload.mgr.ReloadUtils.FileStat;
import com.wjybxx.fastjgame.util.misc.StepWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.util.*;


/**
 * 代码热更新管理器
 * 1. 将要热更新的class文件上传到指定目录（需要保持class包名对应的目录层级，比如com.wjybxx.utils，则对应三级目录）。
 * 2. 接收热更新的http请求 - 一定不要检测到MD5变化就直接更新，可能导致错误的更新。
 * 3. 加载目录下所有class文件到内存，执行类替换。
 * <p>
 * 注意：启服时必须先执行一次代码操作，且必须在加载表格之前（代码与表格必须都是最新的）。
 *
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
public class ClassReloadMgr implements ExtensibleObject {

    private static final Logger logger = LoggerFactory.getLogger(ClassReloadMgr.class);

    /**
     * 项目资源目录
     */
    private final String projectResDir;
    /**
     * 热更类文件的根目录（在资源目录下）
     */
    private final String classDirName;
    /**
     * 黑板
     */
    private final Map<String, Object> blackboard = new HashMap<>();

    /**
     * 已热更过的class文件的状态，避免重复热更（一次热更太多会卡顿）
     */
    private final Map<String, FileStat> className2StatMap = new HashMap<>();

    public ClassReloadMgr(String projectResDir, String classDirName) {
        this.projectResDir = projectResDir;
        this.classDirName = classDirName;
    }

    /**
     * 更新所有的class文件。
     * 注意：请确保已加载热更新代理
     */
    public void reloadAll() throws Exception {
        // 热更新开始日志
        logger.info("reloadAll start");
        final StepWatch stepWatch = StepWatch.createStarted("ClassReloadMrg:reloadAll");
        try {
            final List<ClassDefinition> classDefinitions = findChangedClass();
            stepWatch.logStep("findChangedClass");

            // 打印发现日志
            for (ClassDefinition classDefinition : classDefinitions) {
                logger.info("reloadAll, class stat changed, className {}", classDefinition.getDefinitionClass().getName());
            }

            // 执行热加载
            redefineClasses(classDefinitions);
            stepWatch.logStep("redefineClasses");

            // 打印详细日志
            for (ClassDefinition classDefinition : classDefinitions) {
                logger.info("reloadAll, redefine class success, className {}", classDefinition.getDefinitionClass().getName());
            }

            // 打印总览日志
            final long outerClassCount = countOuterClass(classDefinitions);
            logger.info("reloadAll completed, classNum {}, outerClassCount {}, stepInfo {}", classDefinitions.size(), outerClassCount, stepWatch);
        } catch (Throwable e) {
            // 打印失败日志
            logger.info("reloadAll failure, stepInfo {}", stepWatch);
            throw new ReloadException(e);
        }
    }

    private List<ClassDefinition> findChangedClass() throws Exception {
        final List<File> classFiles = findClassFiles();
        if (classFiles.isEmpty()) {
            return new ArrayList<>();
        }

        final List<ClassDefinition> classDefinitions = new ArrayList<>(classFiles.size());
        for (File file : classFiles) {
            final String className = findClassName(file);
            final Class<?> redefineClass = Class.forName(className);
            final byte[] classFileBytes = ReloadUtils.readBytes(file);

            // 由于class文件一般较小，且数量不多，所以不缓存fileStat，而是实时计算
            final FileStat oldFileStat = className2StatMap.get(className);
            final FileStat fileStat = ReloadUtils.stateOfFileBytes(classFileBytes, oldFileStat);
            if (oldFileStat == fileStat) {
                continue;
            }

            classDefinitions.add(new ClassDefinition(redefineClass, classFileBytes));
        }
        return classDefinitions;
    }

    private List<File> findClassFiles() {
        final List<File> result = new ArrayList<>(20);
        final String classDirPath = projectResDir + File.separator + classDirName;
        ReloadUtils.recurseDir(new File(classDirPath), result, file -> file.getName().endsWith(".class"));
        return result;
    }

    /**
     * 根据文件的绝对路径找到类名(要去除.class)
     */
    private String findClassName(File file) {
        final LinkedList<String> nameList = new LinkedList<>();

        final String simpleClassName = file.getName().substring(0, file.getName().length() - ".class".length());
        nameList.add(simpleClassName);

        while (true) {
            file = file.getParentFile();
            if (file.getName().equals(classDirName)) {
                break;
            }
            nameList.addFirst(file.getName());
        }

        final StringBuilder stringBuilder = new StringBuilder(64);
        for (String element : nameList) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append('.');
            }
            stringBuilder.append(element);
        }
        return stringBuilder.toString();
    }

    private void redefineClasses(List<ClassDefinition> classDefinitions) throws Exception {
        if (!classDefinitions.isEmpty()) {
            // 注意：这里不能直接更新md5，需要先缓存起来
            final Map<String, FileStat> tempClassName2Md5Map = cacheClassStat(classDefinitions);

            // 执行热更新
            ClassReloadAgent.redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));

            // 必须在热更新之后更新md5
            className2StatMap.putAll(tempClassName2Md5Map);
        }
    }

    private static Map<String, FileStat> cacheClassStat(List<ClassDefinition> classDefinitions) {
        final Map<String, FileStat> tempClassName2Md5Map = Maps.newHashMapWithExpectedSize(classDefinitions.size());
        for (ClassDefinition classDefinition : classDefinitions) {
            final byte[] classFileBytes = classDefinition.getDefinitionClassFile();
            tempClassName2Md5Map.put(classDefinition.getDefinitionClass().getName(), ReloadUtils.stateOfFileBytes(classFileBytes, null));
        }
        return tempClassName2Md5Map;
    }

    private static int countOuterClass(List<ClassDefinition> classDefinitions) {
        // 内部类的simpleName和其外部类的类名相同
        return (int) classDefinitions.stream()
                .filter(classDefinition -> !classDefinition.getDefinitionClass().getName().contains("$"))
                .count();
    }

    @Nonnull
    @Override
    public Map<String, Object> getBlackboard() {
        return blackboard;
    }

    @Override
    public Object execute(@Nonnull String cmd, Object params) {
        return null;
    }
}
