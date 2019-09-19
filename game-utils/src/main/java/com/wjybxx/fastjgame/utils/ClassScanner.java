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

package com.wjybxx.fastjgame.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.wjybxx.fastjgame.utils.ClassScannerFilters.exceptInnerClass;

/**
 * 类扫描器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/20 13:57
 * github - https://github.com/hl845740757
 */
public class ClassScanner {

    private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);
    /**
     * 工程项目文件夹
     * target为生成的class文件目录
     */
    private static final Set<String> ignoreDirs = new HashSet<>(Arrays.asList(".git", ".svn", ".idea"));

    private ClassScanner() {

    }

    /**
     * 从包package中获取所有的Class
     *
     * @param pkgName java包名,eg: com.wjybxx.game
     * @return classSet
     */
    public static Set<Class<?>> findAllClass(String pkgName) {
        return findClasses(pkgName, clazzName -> true, clazz -> true);
    }

    /**
     * 返回指定文件夹下的所有外部类
     *
     * @param pkgName java包名,eg: com.wjybxx.game
     * @return classSet
     */
    public static Set<Class<?>> findAllOuterClass(String pkgName) {
        return findClasses(pkgName, exceptInnerClass(), clazz -> true);
    }

    /**
     * 加载指定包下符合条件的class
     *
     * @param pkgName         java包名,eg: com.wjybxx.game
     * @param classNameFilter 过滤要加载的类，避免加载过多无用的类 test返回true的才会加载
     * @param classFilter     对加载后的类进行再次确认 test返回true的才会添加到结果集中
     * @return 符合过滤条件的class文件
     */
    public static Set<Class<?>> findClasses(String pkgName, Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        //第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<>();
        // 获取包的名字 并进行替换
        String pkgDirName = pkgName.replace('.', '/');
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> urls = classLoader.getResources(pkgDirName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上 file:
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findClassesByFile(classLoader, pkgName, filePath, classes, classNameFilter, classFilter);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件 jar:
                    // 获取jar
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    //扫描jar包文件 并添加到集合中
                    findClassesByJar(classLoader, pkgName, jar, classes, classNameFilter, classFilter);
                }
            }
        } catch (IOException e) {
            logger.warn("scanner pkgName {} caught exception.", pkgName, e);
        }
        return classes;
    }

    /**
     * 从文件夹中加载class文件
     *
     * @param classLoader     类加载器
     * @param pkgName         java包名
     * @param pkgPath         文件夹路径
     * @param classes         结果输出
     * @param classNameFilter 过滤要加载的类，避免加载过多无用的类
     * @param classFilter     对加载后的类进行再次确认
     */
    public static void findClassesByFile(ClassLoader classLoader, String pkgName, String pkgPath, Set<Class<?>> classes,
                                         Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        // 获取此包的目录 建立一个File
        File dir = new File(pkgPath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // 只接受文件夹和class文件
        File[] dirFiles = dir.listFiles(file -> file.isDirectory() || file.getName().endsWith(".class"));

        // 没有符合条件的文件
        if (dirFiles == null || dirFiles.length == 0) {
            return;
        }

        // 循环所有文件
        for (File file : dirFiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                if (ignoreDirs.contains(file.getName())) {
                    // 被忽略的文件夹
                    continue;
                }
                findClassesByFile(classLoader, pkgName + "." + file.getName(), pkgPath + "/" + file.getName(), classes,
                        classNameFilter, classFilter);
                continue;
            }
            // 如果是java类文件 去掉后面的.class 只留下类名
            String className = pkgName + "." + file.getName().substring(0, file.getName().length() - 6);

            // 不需要加载
            if (!classNameFilter.test(className)) {
                continue;
            }
            //加载类
            try {
                Class clazz = classLoader.loadClass(className);
                if (classFilter.test(clazz)) {
                    // 不需要的类
                    classes.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                logger.warn("load class {} from file {} caught exception", className, file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * 从jar包中搜索class文件
     *
     * @param classLoader     类加载器
     * @param pkgName         java包名
     * @param jar             jar包对象
     * @param classes         结果输出
     * @param classNameFilter 过滤要加载的类，避免加载过多无用的类
     * @param classFilter     对加载后的类进行再次确认
     */
    public static void findClassesByJar(ClassLoader classLoader, String pkgName, JarFile jar, Set<Class<?>> classes,
                                        Predicate<String> classNameFilter, Predicate<Class<?>> classFilter) {
        String pkgDir = pkgName.replace(".", "/");
        Enumeration<JarEntry> entry = jar.entries();

        // 同样的进行循环迭代
        while (entry.hasMoreElements()) {
            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文
            JarEntry jarEntry = entry.nextElement();

            // 包也是一个entry
            if (jarEntry.isDirectory()) {
                continue;
            }

            String name = jarEntry.getName();
            // 如果是以/开头的
            if (name.charAt(0) == '/') {
                // 获取后面的字符串
                name = name.substring(1);
            }

            if (!name.startsWith(pkgDir) || !name.endsWith(".class")) {
                continue;
            }
            // 如果是一个.class文件，去掉后面的".class" 获取真正的类名
            String className = name.substring(0, name.length() - 6).replace("/", ".");

            // 不需要加载
            if (!classNameFilter.test(className)) {
                continue;
            }

            //加载类
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (classFilter.test(clazz)) {
                    // 不需要的类
                    classes.add(clazz);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // jdk 1.7的新语法还没用过...
                logger.error("load class {} from jar {} caught exception", className, jar.getName(), e);
            }
        }
    }

    public static void main(String[] args) {
        Set<Class<?>> allClass = findAllClass("com.wjybxx.fastjgame");
        allClass.forEach(System.out::println);

        System.out.println(" ------------------------------------- ");

        Set<Class<?>> allClass2 = findAllClass("io.netty.util.concurrent");
        allClass2.forEach(System.out::println);
    }
}
