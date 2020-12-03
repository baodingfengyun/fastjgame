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

import com.google.common.collect.Sets;
import com.wjybxx.fastjgame.reload.file.*;
import com.wjybxx.fastjgame.reload.mgr.FileReloadTask.TaskMetadata;
import com.wjybxx.fastjgame.reload.mgr.FileReloadTask.TaskResult;
import com.wjybxx.fastjgame.reload.mgr.ReloadUtils.FileStat;
import com.wjybxx.fastjgame.util.concurrent.FutureUtils;
import com.wjybxx.fastjgame.util.misc.StepWatch;
import com.wjybxx.fastjgame.util.time.TimeUtils;
import com.wjybxx.fastjgame.util.timer.TimerHandle;
import com.wjybxx.fastjgame.util.timer.TimerSystem;
import com.wjybxx.fastjgame.util.timer.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final FileDataMgr fileDataMgr;

    private final TimerSystem timerSystem;
    private final Executor executor;
    private final long timeoutFindChangedFiles;
    private final long timeoutReadFiles;
    private final long autoReloadInterval;

    private final FileDataContainer fileDataContainer = new FileDataContainer();
    private final Map<FileName<?>, ListenerWrapper> listenerWrapperMap = new IdentityHashMap<>(50);

    private final Map<FileName<?>, ReaderMetadata<?>> readerMetadataMap = new IdentityHashMap<>(500);
    private final Map<Class<?>, BuilderMetadata<?>> builderMetadataMap = new IdentityHashMap<>(50);

    private TimerHandle autoReloadTimerHandle;
    private TimerHandle reloadTimerHandle;

    public FileReloadMgr(String projectResDir, FileDataMgr fileDataMgr, TimerSystem timerSystem, Executor executor) {
        this(projectResDir, fileDataMgr, timerSystem, executor, 30 * TimeUtils.SEC, 5 * TimeUtils.SEC, TimeUtils.MIN);
    }

    /**
     * @param projectResDir           项目资源目录，所有的{@link FileName}都是相对于该目录的路径
     * @param fileDataMgr             应用自身管理数据的地方
     * @param timerSystem             主线程的timer管理器，用于异步热更新时检查热更新进度
     * @param executor                用于并发读取文件的线程池。
     *                                如果是共享线程池，该线程池上不要有大量的阻塞任务即可。
     *                                如果独享，建议拒绝策略为{@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy}。
     * @param autoReloadInterval      后台自动更新检查间隔(毫秒)
     * @param timeoutFindChangedFiles 统计文件变化的超时时间(毫秒)
     * @param timeoutReadFiles        读取文件内容的超时时间(毫秒)
     */
    public FileReloadMgr(String projectResDir, FileDataMgr fileDataMgr,
                         TimerSystem timerSystem, Executor executor,
                         long autoReloadInterval, long timeoutFindChangedFiles, long timeoutReadFiles) {
        this.projectResDir = Objects.requireNonNull(projectResDir, "projectResDir");
        this.timerSystem = timerSystem;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fileDataMgr = Objects.requireNonNull(fileDataMgr, "fileDataMgr");
        this.autoReloadInterval = autoReloadInterval;
        this.timeoutFindChangedFiles = timeoutFindChangedFiles;
        this.timeoutReadFiles = timeoutReadFiles;
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
        if (autoReloadTimerHandle != null || reloadTimerHandle != null) {
            throw new IllegalStateException();
        }
        autoReloadTimerHandle = timerSystem.newFixedDelay(autoReloadInterval, autoReloadInterval, new AutoReloadTask());

        logger.info("loadAll started");
        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMgr:loadAll");
        try {
            final List<TaskMetadata> taskMetadataList = createTaskMetadata(readerMetadataMap.keySet(), ReloadMode.START_SERVER);
            final List<TaskResult> taskResultList = FileReloadTask.runAsync(taskMetadataList, executor, timeoutFindChangedFiles, timeoutReadFiles)
                    .join();
            stepWatch.logStep("join");

            reloadImpl(taskResultList, ReloadMode.START_SERVER);
            stepWatch.logStep("reloadImpl");

            logger.info("loadAll completed, fileNum {}, stepInfo {}", readerMetadataMap.size(), stepWatch);
        } catch (Exception e) {
            logger.info("loadAll failure, fileNum {}},  stepInfo {}", readerMetadataMap.size(), stepWatch);
            throw new ReloadException(FutureUtils.unwrapCompletionException(e));
        }
    }

    /**
     * 重新加载所有的配置文件中改变的文件
     *
     * @param callback 用于监听回调完成事件，可以为null。如果
     */
    public void reloadAll(@Nullable FileReloadCallback callback) {
        reloadScope(readerMetadataMap.keySet(), callback);
    }

    /**
     * 重新加载指定的配置文件中改变的文件
     *
     * @param scope    检测范围
     * @param callback
     */
    public void reloadScope(@Nonnull Set<FileName<?>> scope, @Nullable FileReloadCallback callback) {
        Objects.requireNonNull(scope, "scope");
        ensureFileReaderExist(scope);
        final List<TaskMetadata> taskMetadataList = createTaskMetadata(readerMetadataMap.keySet(), ReloadMode.RELOAD_SCOPE);
        final CompletableFuture<List<TaskResult>> future = FileReloadTask.runAsync(taskMetadataList, executor, timeoutFindChangedFiles, timeoutReadFiles);
        reloadTimerHandle = timerSystem.newFixedDelay(1000, 50, new ReloadTaskTracker(future, ReloadMode.RELOAD_SCOPE, callback));
    }

    /**
     * 重新加载指定的配置文件。
     * 在指定文件的情况下，不论文件是否变化，都执行回调逻辑。
     * <p>
     * Q: 为何如此设计？
     * A: 避免md5相等导致的无法更新问题（全部更新时某些逻辑失败了，无法再次更新的情况）。
     * 另一种方式是稍微改动一点内容，导致md5改变，再热更新。
     */
    public void forceReload(Set<FileName<?>> scope, FileReloadCallback callback) {
        Objects.requireNonNull(scope, "scope");
        ensureFileReaderExist(scope);
        final List<TaskMetadata> taskMetadataList = createTaskMetadata(readerMetadataMap.keySet(), ReloadMode.FORCE_RELOAD);
        final CompletableFuture<List<TaskResult>> future = FileReloadTask.runAsync(taskMetadataList, executor, timeoutFindChangedFiles, timeoutReadFiles);
        reloadTimerHandle = timerSystem.newFixedDelay(1000, 50, new ReloadTaskTracker(future, ReloadMode.FORCE_RELOAD, callback));
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
        ensureFileReaderExist(fileNameSet);

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

    private void ensureFileReaderExist(@Nonnull Collection<FileName<?>> fileNameSet) {
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
        ensureFileReaderExist(fileNameSet);

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

    private List<TaskMetadata> createTaskMetadata(Set<FileName<?>> scope, ReloadMode reloadMode) {
        if (scope.isEmpty()) {
            return new ArrayList<>();
        }
        final List<TaskMetadata> result = new ArrayList<>(scope.size());
        for (FileName<?> fileName : scope) {
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(fileName);
            if (reloadMode == ReloadMode.FORCE_RELOAD) {
                result.add(new TaskMetadata(readerMetadata.file, readerMetadata.reader, null));
            } else {
                result.add(new TaskMetadata(readerMetadata.file, readerMetadata.reader, readerMetadata.fileStat));
            }
        }
        return result;
    }

    private void reloadImpl(List<TaskResult> changedFiles, ReloadMode reloadMode) throws Exception {
        if (changedFiles.isEmpty()) {
            return;
        }

        final StepWatch stepWatch = StepWatch.createStarted("FileReloadMrg:reloadImpl");
        // 合并文件读取结果
        final Map<FileName<?>, Object> fileDataMap = new IdentityHashMap<>(changedFiles.size());
        for (TaskResult taskResult : changedFiles) {
            Objects.requireNonNull(taskResult.fileData);
            fileDataMap.put(taskResult.taskMetadata.reader.fileName(), taskResult.fileData);
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
            notifyListeners(Set.copyOf(fileDataMap.keySet()));
            stepWatch.logStep("notifyListeners");
        }

        // 最后更新文件状态(这可以使得在前面发生异常后可以重试)
        for (TaskResult taskResult : changedFiles) {
            final ReaderMetadata<?> readerMetadata = readerMetadataMap.get(taskResult.taskMetadata.reader.fileName());
            readerMetadata.fileStat = taskResult.fileStat;
        }
        logger.info(stepWatch.getLog());
    }


    private void notifyListeners(Set<FileName<?>> allChangedFileNameSet) throws Exception {
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

    enum ReloadMode {
        START_SERVER,
        RELOAD_SCOPE,
        FORCE_RELOAD
    }

    private class ReloadTaskTracker implements TimerTask {

        final CompletableFuture<List<TaskResult>> future;
        final ReloadMode reloadMode;
        final FileReloadCallback callback;
        final Thread thread;

        ReloadTaskTracker(CompletableFuture<List<TaskResult>> future, ReloadMode reloadMode,
                          @Nullable FileReloadCallback callback) {
            this.future = future;
            this.reloadMode = reloadMode;
            this.callback = callback;
            this.thread = Thread.currentThread();
        }

        @Override
        public void run(TimerHandle handle) throws Exception {
            if (thread != Thread.currentThread()) {
                throw new IllegalStateException("timer is running on the wrong thread");
            }

            if (reloadTimerHandle != handle) {
                handle.close();
                logger.info("reload cancelled, reloadMode {}", reloadMode);
                if (callback != null) {
                    callback.onCompleted(new HashSet<>(), new CancellationException());
                }
                return;
            }

            if (!future.isDone()) {
                return;
            }

            try {
                // 检查是否还满足热更新条件
                final List<TaskResult> resultList = future.join();
                final Set<FileName<?>> changedFiles = Sets.newHashSetWithExpectedSize(resultList.size());
                for (TaskResult taskResult : resultList) {
                    final FileName<?> fileName = taskResult.taskMetadata.reader.fileName();
                    if (readerMetadataMap.get(fileName) == null) {
                        throw new IllegalStateException("the state of fileReloadMgr has changed, fileName: " + fileName);
                    }
                    changedFiles.add(fileName);
                }
                // 执行热更新
                reloadImpl(resultList, reloadMode);
                logger.warn("reload completed, reloadMode {}, fileNum {}", reloadMode, resultList.size());
                if (callback != null) {
                    callback.onCompleted(changedFiles, null);
                }
            } catch (Throwable e) {
                final Throwable cause = FutureUtils.unwrapCompletionException(e);
                logger.warn("reload failure, reloadMode {}", reloadMode, cause);
                if (callback != null) {
                    callback.onCompleted(new HashSet<>(), cause);
                }
            } finally {
                handle.close();
                reloadTimerHandle = null;
            }
        }
    }

    private class AutoReloadTask implements TimerTask {

        @Override
        public void run(TimerHandle handle) throws Exception {
            if (reloadTimerHandle != null) {
                // 有正在执行的任务
                return;
            }
            final Set<FileName<?>> scope = readerMetadataMap.values().stream()
                    .filter(readerMetadata -> readerMetadata.reader.autoReloadable())
                    .map(readerMetadata -> readerMetadata.reader.fileName())
                    .collect(Collectors.toSet());
            if (!scope.isEmpty()) {
                reloadScope(scope, null);
            }
        }
    }

    // 全部为静态内部类，避免引用错误的数据

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

    private static class FileDataProviderImpl implements FileCacheBuilder.FileDataProvider {

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