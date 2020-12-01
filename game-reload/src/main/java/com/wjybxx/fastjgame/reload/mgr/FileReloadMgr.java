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

import com.wjybxx.fastjgame.reload.file.FileCacheBuilder;
import com.wjybxx.fastjgame.reload.file.FileName;
import com.wjybxx.fastjgame.reload.file.FileReader;
import com.wjybxx.fastjgame.reload.file.FileReloadListener;
import com.wjybxx.fastjgame.reload.mgr.ReloadUtils.FileStat;
import com.wjybxx.fastjgame.util.concurrent.FutureUtils;
import com.wjybxx.fastjgame.util.misc.StepWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件热更管理器。
 * 实现上是否觉得像NIO的Select模型？
 * <p>
 * 约定：该类不修改方法传入的任何集合，也不保留其引用。
 *
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
public final class FileReloadMgr implements ExtensibleObject {

    private static final Logger logger = LoggerFactory.getLogger(FileReloadMgr.class);

    private final Map<String, Object> blackboard = new HashMap<>();

    private final String projectResDir;
    private final Executor executor;
    private final FileDataMgr fileDataMgr;

    private final FileDataContainer fileDataContainer = new FileDataContainer();
    private final Map<FileName<?>, ListenerWrapper> listenerWrapperMap = new IdentityHashMap<>(50);

    private final Map<FileName<?>, ReaderMetadata<?>> readerMetadataMap = new IdentityHashMap<>(500);
    private final Map<Class<?>, BuilderMetadata<?>> builderMetadataMap = new IdentityHashMap<>(50);

    /**
     * @param projectResDir 项目资源目录，所有的{@link FileName}都是相对于该目录的路径
     * @param executor      用于并发读取文件的线程池。
     *                      如果是共享线程池，该线程池上不要有大量的阻塞任务即可。
     *                      如果独享，建议拒绝策略为{@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy}。
     * @param fileDataMgr   应用自身管理数据的地方
     */
    public FileReloadMgr(String projectResDir, Executor executor, FileDataMgr fileDataMgr) {
        this.projectResDir = projectResDir;
        this.executor = executor;
        this.fileDataMgr = fileDataMgr;
    }

    public void registerReaders(Collection<? extends FileReader<?>> readers) {
        for (FileReader<?> reader : readers) {
            final FileName<?> fileName = reader.fileName();
            if (readerMetadataMap.containsKey(fileName)) {
                final String msg = String.format("fileName has more than one associated reader, fileName: %s, reader: %s", fileName, reader.getClass().getName());
                throw new IllegalArgumentException(msg);
            }
            final File file = checkedFileOfName(projectResDir, fileName);
            readerMetadataMap.put(fileName, new ReaderMetadata<>(reader, file));
        }
    }

    /**
     * reader存在，对应的文件必须存在。
     */
    private static File checkedFileOfName(String projectResDir, FileName<?> fileName) {
        final String relativePath = fileName.getRelativePath();
        final File file = new File(projectResDir + File.separator + relativePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(relativePath + " must be a normal file that already exists");
        }
        return file;
    }

    public void unregisterReader(Collection<? extends FileReader<?>> readers) {
        for (FileReader<?> reader : readers) {
            final FileName<?> fileName = reader.fileName();
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(fileName);
            if (readerMetadata == null) {
                continue;
            }
            if (readerMetadata.reader != reader) {
                final String msg = String.format("reader mismatch, fileName: %s, reader: %s", fileName, reader.getClass().getName());
                throw new IllegalArgumentException(msg);
            }
            readerMetadataMap.remove(fileName);
        }
    }

    /**
     * 注意：需要先调用{@link #registerReaders(Collection)}注册文件读取实现。
     */
    public void registerCacheBuilders(Collection<? extends FileCacheBuilder<?>> cacheBuilders) {
        for (FileCacheBuilder<?> builder : cacheBuilders) {
            final Class<?> builderType = builder.getClass();
            final Set<FileName<?>> builderFileNames = builder.fileNames();
            // 不可以为空
            if (builderFileNames.isEmpty()) {
                throw new IllegalStateException("fileName is empty, builder: " + builder.getClass().getName());
            }
            // reader缺失检查
            for (FileName<?> fileName : builderFileNames) {
                if (!readerMetadataMap.containsKey(fileName)) {
                    final String msg = String.format("reader is null, fileName: %s, builder: %s", fileName, builder.getClass().getName());
                    throw new IllegalStateException(msg);
                }
            }
            builderMetadataMap.put(builderType, new BuilderMetadata<>(builder, builder.fileNames()));
        }
    }

    public void unregisterCacheBuilders(Collection<? extends FileCacheBuilder<?>> cacheBuilders) {
        for (FileCacheBuilder<?> builder : cacheBuilders) {
            final Class<?> builderType = builder.getClass();
            builderMetadataMap.remove(builderType);
        }
    }

    /**
     * 获取指定文件的数据。
     * 注意：这是给扩展预留的接口，不是应用层使用的接口。
     */
    public <T> T getFileData(FileName<T> fileName) {
        return fileDataContainer.getFileData(fileName);
    }

    public Object getCacheData(Class<?> builderType) {
        return fileDataContainer.getCacheData(builderType);
    }

    /**
     * 启服加载所有文件
     */
    public void loadAll() throws Exception {
        logger.info("loadAll started");
        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMrg:loadAll");
        try {
            final List<TaskContext> changedFiles = findChangedFiles(readerMetadataMap.keySet());
            stepWatch.logStep("findChangedFiles ");

            reloadImpl(changedFiles, ReloadMode.START_SERVER);
            stepWatch.logStep("reloadImpl");

            logger.info("loadAll completed, stepInfo {}", stepWatch);
        } catch (Exception e) {
            logger.info("loadAll failure, stepInfo {}", stepWatch);
            throw new ReloadException(FutureUtils.unwrapCompletionException(e));
        }
    }

    /**
     * @param scope 文件检索范围
     * @return 文件状态发生改变的文件
     */
    private List<TaskContext> findChangedFiles(Set<FileName<?>> scope) {
        if (scope.isEmpty()) {
            return new ArrayList<>();
        }

        final List<TaskContext> contextList = new ArrayList<>(scope.size());
        final List<CompletableFuture<?>> futureList = new ArrayList<>(scope.size());
        for (FileName<?> fileName : scope) {
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(fileName);
            if (null == readerMetadata) {
                throw new IllegalArgumentException("unknown fileName: " + fileName);
            }

            final TaskContext taskContext = new TaskContext(readerMetadata.reader, readerMetadata.file, readerMetadata.fileStat);
            futureList.add(CompletableFuture.runAsync(new StatisticFileStatTask(taskContext), executor));
            contextList.add(taskContext);
        }

        CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
                .orTimeout(1, TimeUnit.MINUTES)
                .join();

        return contextList.stream()
                .filter(c -> c.fileStat != c.oldFileStat)
                .collect(Collectors.toList());
    }

    private void reloadImpl(List<TaskContext> changedFiles, ReloadMode reloadMode) throws Exception {
        if (changedFiles.isEmpty()) {
            logger.info("reloadImpl, changedFiles is empty");
            return;
        }

        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMrg:reloadImpl");
        final List<CompletableFuture<?>> futureList = new ArrayList<>(changedFiles.size());
        for (TaskContext context : changedFiles) {
            Objects.requireNonNull(context.fileStat);
            futureList.add(CompletableFuture.runAsync(new ReadFileTask(context), executor));
        }
        CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
                .orTimeout(1, TimeUnit.MINUTES)
                .join();
        stepWatch.logStep("join");

        // 合并文件读取结果
        final Map<FileName<?>, Object> fileDataMap = new IdentityHashMap<>(changedFiles.size());
        for (TaskContext context : changedFiles) {
            Objects.requireNonNull(context.fileData);
            fileDataMap.put(context.reader.fileName(), context.fileData);
        }

        // 创建沙箱，在沙箱上进行模拟赋值和验证
        final SandBox sandbox = createSandbox(fileDataMgr, fileDataContainer);
        assignFileData(fileDataMap, sandbox.fileDataMgr, sandbox.fileDataContainer);
        final Map<Class<?>, Object> cacheDataMap = buildCacheData(fileDataMap.keySet(), sandbox.fileDataContainer);
        assignCacheData(cacheDataMap, sandbox.fileDataMgr, sandbox.fileDataContainer);
        stepWatch.logStep("buildSandbox");

        // TODO 在线热更新的时候全部校验开销是否较高？有无必要省去，只在本地测试？
        validateOther(sandbox.fileDataMgr);
        stepWatch.logStep("validateOther");

        // 赋值到真实环境（这里其实已经完成了读表逻辑）
        assignFileData(fileDataMap, fileDataMgr, fileDataContainer);
        assignCacheData(cacheDataMap, fileDataMgr, fileDataContainer);

        // 执行回调逻辑（这里可能产生异常）
        if (reloadMode != ReloadMode.START_SERVER) {
            notifyFileReloadListeners(Collections.unmodifiableSet(fileDataMap.keySet()));
            stepWatch.logStep("notifyListeners");
        }

        // 最后更新文件状态(这可以使得在前面发生异常后可以重试)
        for (TaskContext context : changedFiles) {
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(context.reader.fileName());
            readerMetadata.fileStat = context.fileStat;
        }
        logger.info(stepWatch.getLog());
    }

    private void notifyFileReloadListeners(Set<FileName<?>> allChangedFileNameSet) throws Exception {
        final Set<ListenerWrapper> notifiedListeners = Collections.newSetFromMap(new IdentityHashMap<>(20));
        for (FileName<?> fileName : allChangedFileNameSet) {
            final ListenerWrapper listenerWrapper = listenerWrapperMap.get(fileName);
            if (null == listenerWrapper) {
                continue;
            }
            if (!notifiedListeners.add(listenerWrapper)) {
                // 已通知
                continue;
            }
            // Q: 为什么允许抛出异常?
            // A: 这样会中断热更流程，避免文件状态被修改。这样当我们修改代码之后，可以直接再次调用热更方法更新文件。
            listenerWrapper.afterReload(allChangedFileNameSet);
        }
    }

    private void validateOther(FileDataMgr sandbox) {
        for (ReaderMetadata<?> readerMetadata : readerMetadataMap.values()) {
            readerMetadata.reader.validateOther(sandbox);
        }
        for (BuilderMetadata<?> builderMetadata : builderMetadataMap.values()) {
            builderMetadata.builder.validateOther(sandbox);
        }
    }

    private Map<Class<?>, Object> buildCacheData(Set<FileName<?>> fileNameSet, FileDataContainer fileDataContainer) {
        final Map<Class<?>, Object> result = new IdentityHashMap<>(builderMetadataMap.size());
        // 这里数量较少，直接遍历所有的builder(减少不必要的依赖)
        for (BuilderMetadata<?> builderMetadata : builderMetadataMap.values()) {
            for (FileName<?> fileName : builderMetadata.fileNameSet) {
                if (fileNameSet.contains(fileName)) {
                    final FileCacheBuilder.FileDataProvider sheetDataProvider = new FileDataProviderImpl(fileDataContainer, builderMetadata.fileNameSet);
                    final Object cache = builderMetadata.builder.build(sheetDataProvider);
                    // null检查
                    Objects.requireNonNull(cache, builderMetadata.builder.getClass().getName());
                    result.put(builderMetadata.builder.getClass(), cache);
                    break;
                }
            }
        }
        return result;
    }

    private SandBox createSandbox(FileDataMgr fileDataMgProtoType, FileDataContainer containerProtoType) {
        final FileDataMgr sandboxFileDataMgr = fileDataMgProtoType.newInstance();
        final FileDataContainer sandBoxFileDataContainer = new FileDataContainer();
        assignFileData(containerProtoType.getAllFileData(), sandboxFileDataMgr, sandBoxFileDataContainer);
        assignCacheData(containerProtoType.getAllCacheData(), sandboxFileDataMgr, sandBoxFileDataContainer);
        return new SandBox(sandboxFileDataMgr, sandBoxFileDataContainer);
    }

    private void assignFileData(Map<FileName<?>, Object> fileDataMap, FileDataMgr fileDataMgr, FileDataContainer fileDataContainer) {
        for (Map.Entry<FileName<?>, Object> entry : fileDataMap.entrySet()) {
            @SuppressWarnings("unchecked") final FileName<Object> fileName = (FileName<Object>) entry.getKey();
            @SuppressWarnings("unchecked") final ReaderMetadata<Object> readerMetadata = (ReaderMetadata<Object>) readerMetadataMap.get(fileName);

            final Object fileData = entry.getValue();
            readerMetadata.reader.assignTo(fileData, fileDataMgr);
            fileDataContainer.putFileData(fileName, fileData);
        }
    }

    private void assignCacheData(Map<Class<?>, Object> cacheDataMap, FileDataMgr fileDataMgr, FileDataContainer fileDataContainer) {
        for (Map.Entry<Class<?>, Object> entry : cacheDataMap.entrySet()) {
            final Class<?> builderType = entry.getKey();
            @SuppressWarnings("unchecked") final BuilderMetadata<Object> builderMetadata = (BuilderMetadata<Object>) builderMetadataMap.get(builderType);

            final Object cacheData = entry.getValue();
            builderMetadata.builder.assignTo(cacheData, fileDataMgr);
            fileDataContainer.putCacheData(builderType, cacheData);
        }
    }

    /**
     * 重新加载所有的配置文件中改变的文件
     */
    public void reloadAll() {
        reloadScope(readerMetadataMap.keySet());
    }

    /**
     * 重新加载指定的配置文件中改变的文件
     *
     * @param scope 检测范围
     */
    public void reloadScope(Set<FileName<?>> scope) {
        logger.info("reloadScope started");
        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMrg:reloadScope");
        try {
            final List<TaskContext> changedFiles = findChangedFiles(scope);
            // 打印发现日志
            for (TaskContext context : changedFiles) {
                logger.info("reloadScope, file stat changed, fileName {}", context.fileName());
            }

            // 执行热更新
            reloadImpl(changedFiles, ReloadMode.RELOAD_SCOPE);

            // 打印详细日志
            for (TaskContext context : changedFiles) {
                logger.info("reloadScope, reload file success, fileName {}", context.fileName());
            }

            // 打印总览日志
            logger.info("reloadScope completed, fileNum {}, stepInfo {}", changedFiles.size(), stepWatch);
        } catch (Throwable e) {
            // 打印失败日志
            logger.info("reloadScope failure, stepInfo {}", stepWatch);
            throw new ReloadException(FutureUtils.unwrapCompletionException(e));
        }
    }

    /**
     * 重新加载指定的配置文件。
     * 在指定文件的情况下，不论文件是否变化，都执行回调逻辑。
     * <p>
     * Q: 为何如此设计？
     * A: 避免md5相等导致的无法更新问题（全部更新时某些逻辑失败了，无法再次更新的情况）。
     * 另一种方式是稍微改动一点内容，导致md5改变，再热更新。
     */
    public void forceReload(Set<FileName<?>> fileNameSet) {
        logger.info("forceReload started");
        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMrg:forceReload");
        try {
            final List<TaskContext> contextList = new ArrayList<>(fileNameSet.size());
            for (FileName<?> fileName : fileNameSet) {
                Objects.requireNonNull(fileName, "fileName");
                final ReaderMetadata readerMetadata = readerMetadataMap.get(fileName);
                if (readerMetadata == null) {
                    throw new IllegalArgumentException("unknown fileName: " + fileName);
                }
                final TaskContext context = new TaskContext(readerMetadata.reader, readerMetadata.file, readerMetadata.fileStat);
                contextList.add(context);
            }

            // 打印文件信息
            for (TaskContext context : contextList) {
                logger.info("forceReload, file changed, fileName {}", context.fileName());
            }

            // 统计文件状态（这里文件一般不多，主线程统计）
            for (TaskContext context : contextList) {
                context.fileStat = ReloadUtils.statOfFile(context.file, context.fileStat);
            }
            stepWatch.logStep("statOfFile");

            // 执行热更新
            reloadImpl(contextList, ReloadMode.FORCE_RELOAD);
            stepWatch.logStep("reloadImpl");

            // 打印详细日志
            for (TaskContext context : contextList) {
                logger.info("forceReload, reload file success, fileName {}", context.fileName());
            }

            // 打印总览日志
            logger.info("forceReload completed, fileNum {}, stepInfo {}", contextList.size(), stepWatch);
        } catch (Throwable e) {
            // 打印失败日志
            logger.warn("forceReload failure, stepInfo {}", stepWatch);
            throw new ReloadException(FutureUtils.unwrapCompletionException(e));
        }
    }

    /**
     * 注册文件热更新回调
     *
     * @param fileNameSet 关注的文件名
     * @param listener    监听器
     */
    public void registerListener(@Nonnull Set<FileName<?>> fileNameSet, @Nonnull FileReloadListener listener) throws IOException {
        Objects.requireNonNull(fileNameSet, "fileNameSet");
        Objects.requireNonNull(listener, "listener");

        if (fileNameSet.isEmpty()) {
            throw new IllegalArgumentException("fileNameSet is empty");
        }

        // 必须拷贝，该类的约定是不修改和引用外部传入的集合，这样更加可靠
        fileNameSet = Set.copyOf(fileNameSet);
        // 努力保证失败原子性，这里务必在拷贝的集合上检查
        validateFileNameSet(fileNameSet);

        final DefaultListenerWrapper listenerWrapper = new DefaultListenerWrapper(fileNameSet, listener);
        for (FileName<?> fileName : fileNameSet) {
            final ListenerWrapper existListenerWrapper = listenerWrapperMap.get(fileName);
            if (null == existListenerWrapper) {
                // 一般一个listener
                listenerWrapperMap.put(fileName, listenerWrapper);
                return;
            }
            if (existListenerWrapper instanceof CompositeListenerWrapper) {
                // 超过两个listener
                ((CompositeListenerWrapper) existListenerWrapper).addChild(listenerWrapper);
            } else {
                // 两个listener
                listenerWrapperMap.put(fileName, new CompositeListenerWrapper(existListenerWrapper, listenerWrapper));
            }
        }
    }

    private void validateFileNameSet(@Nonnull Set<FileName<?>> fileNameSet) {
        for (FileName<?> fileName : fileNameSet) {
            Objects.requireNonNull(fileName, "fileName");
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(fileName);
            if (null == readerMetadata) {
                throw new IllegalArgumentException("unknown fileName: " + fileName);
            }
        }
    }

    public void unregisterListener(@Nonnull Set<FileName<?>> fileNameSet, FileReloadListener listener) {
        Objects.requireNonNull(fileNameSet, "fileNameSet");
        Objects.requireNonNull(listener, "listener");
        validateFileNameSet(fileNameSet);

        for (FileName<?> fileName : fileNameSet) {
            final ListenerWrapper existListenerWrapper = listenerWrapperMap.get(fileName);
            if (existListenerWrapper == null) {
                continue;
            }
            if (existListenerWrapper instanceof DefaultListenerWrapper) {
                if (((DefaultListenerWrapper) existListenerWrapper).reloadListener == listener) {
                    listenerWrapperMap.remove(fileName);
                }
            } else {
                final CompositeListenerWrapper compositeListenerWrapper = (CompositeListenerWrapper) existListenerWrapper;
                compositeListenerWrapper.children.removeIf(e -> ((DefaultListenerWrapper) e).reloadListener == listener);
                if (compositeListenerWrapper.children.isEmpty()) {
                    listenerWrapperMap.remove(fileName);
                }
            }
        }
    }

    // region 内部实现

    static class FileDataProviderImpl implements FileCacheBuilder.FileDataProvider {

        final FileDataContainer fileDataContainer;
        final Set<FileName<?>> fileNameSet;

        FileDataProviderImpl(FileDataContainer fileDataContainer, Set<FileName<?>> fileNameSet) {
            this.fileDataContainer = fileDataContainer;
            this.fileNameSet = fileNameSet;
        }

        @Override
        public <T> T getFileData(@Nonnull FileName<T> fileName) {
            if (fileNameSet.contains(fileName)) {
                return fileDataContainer.getFileData(fileName);
            }
            throw new IllegalArgumentException("restricted filename: " + fileName);
        }
    }

    private enum ReloadMode {
        START_SERVER,
        RELOAD_SCOPE,
        FORCE_RELOAD
    }

    private static class TaskContext {

        final FileReader<?> reader;
        final File file;
        final FileStat oldFileStat;

        FileStat fileStat;
        Object fileData;

        TaskContext(FileReader<?> reader, File file, FileStat oldFileStat) {
            this.reader = reader;
            this.file = file;
            this.oldFileStat = oldFileStat;
        }

        FileName<?> fileName() {
            return reader.fileName();
        }
    }

    /**
     * 统计文件的状态
     */
    private static class StatisticFileStatTask implements Runnable {

        final TaskContext context;

        private StatisticFileStatTask(TaskContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                context.fileStat = ReloadUtils.statOfFile(context.file, context.oldFileStat);
            } catch (Exception e) {
                throw new RuntimeException("fileName: " + context.file.getName(), e);
            }
        }
    }

    /**
     * 读取文件内容
     */
    private static class ReadFileTask implements Runnable {

        final TaskContext context;

        private ReadFileTask(TaskContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                final Object fileData = context.reader.read(context.file);
                Objects.requireNonNull(fileData, context.reader.getClass().getName());
                context.fileData = fileData;
            } catch (Exception e) {
                throw new RuntimeException("fileName: " + context.file.getName(), e);
            }
        }
    }

    private static class ReaderMetadata<T> {

        final FileReader<T> reader;
        final File file;

        FileStat fileStat;

        ReaderMetadata(FileReader<T> reader, File file) {
            this.file = file;
            this.reader = reader;
        }
    }

    private static class BuilderMetadata<T> {

        final FileCacheBuilder<T> builder;
        final Set<FileName<?>> fileNameSet;

        BuilderMetadata(FileCacheBuilder<T> builder, Collection<FileName<?>> fileNameSet) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.fileNameSet = Set.copyOf(Objects.requireNonNull(fileNameSet, "fileNameSet"));
        }
    }

    private interface ListenerWrapper {

        void afterReload(Set<FileName<?>> allChangedFileNameSet) throws Exception;
    }

    private static class DefaultListenerWrapper implements ListenerWrapper {

        final Set<FileName<?>> fileNameSet;
        final FileReloadListener reloadListener;

        DefaultListenerWrapper(Set<FileName<?>> fileNameSet, FileReloadListener reloadListener) {
            // 外部保证是个不可变集合
            this.fileNameSet = fileNameSet;
            this.reloadListener = reloadListener;
        }

        @Override
        public void afterReload(Set<FileName<?>> allChangedFileNameSet) throws Exception {
            final Set<FileName<?>> changedFileNameSet = new HashSet<>(fileNameSet);
            changedFileNameSet.retainAll(allChangedFileNameSet);
            reloadListener.afterReload(fileNameSet, changedFileNameSet);
        }
    }

    private static class CompositeListenerWrapper implements ListenerWrapper {

        final List<ListenerWrapper> children;

        CompositeListenerWrapper(ListenerWrapper first, ListenerWrapper second) {
            this.children = new ArrayList<>(2);
            this.children.add(first);
            this.children.add(second);
        }

        @Override
        public void afterReload(Set<FileName<?>> allChangedFileNameSet) throws Exception {
            for (ListenerWrapper child : children) {
                child.afterReload(allChangedFileNameSet);
            }
        }

        void addChild(ListenerWrapper child) {
            this.children.add(child);
        }
    }

    private static class SandBox {

        final FileDataMgr fileDataMgr;
        final FileDataContainer fileDataContainer;

        SandBox(FileDataMgr fileDataMgr, FileDataContainer fileDataContainer) {
            this.fileDataMgr = fileDataMgr;
            this.fileDataContainer = fileDataContainer;
        }
    }

    private static class FileDataContainer {

        private final Map<FileName<?>, Object> fileDataMap;
        private final Map<Class<?>, Object> cacheDataMap;

        FileDataContainer() {
            fileDataMap = new IdentityHashMap<>(300);
            cacheDataMap = new IdentityHashMap<>(30);
        }

        final <T> T getFileData(FileName<T> fileName) {
            Objects.requireNonNull(fileName, "fileName");
            @SuppressWarnings("unchecked") final T result = (T) fileDataMap.get(fileName);
            if (null == result) {
                throw new IllegalArgumentException("fileData is null, fileName: " + fileName);
            }
            return result;
        }

        final <T> void putFileData(FileName<T> fileName, T fileData) {
            Objects.requireNonNull(fileName, "fileName");
            Objects.requireNonNull(fileData, "fileData");
            fileDataMap.put(fileName, fileData);
        }

        final Map<FileName<?>, Object> getAllFileData() {
            return new IdentityHashMap<>(fileDataMap);
        }

        final void putCacheData(Class<?> builderType, Object cacheData) {
            Objects.requireNonNull(builderType, "builderType");
            Objects.requireNonNull(cacheData, "cacheData");
            cacheDataMap.put(builderType, cacheData);
        }

        final Map<Class<?>, Object> getAllCacheData() {
            return new IdentityHashMap<>(cacheDataMap);
        }

        final Object getCacheData(Class<?> builderType) {
            Objects.requireNonNull(builderType, "builderType");
            final Object result = cacheDataMap.get(builderType);
            if (result == null) {
                throw new IllegalArgumentException("cacheData is null, builder: " + builderType);
            }
            return result;
        }
    }

    // endregion

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