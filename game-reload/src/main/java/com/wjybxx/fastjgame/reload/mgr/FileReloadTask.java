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

package com.wjybxx.fastjgame.reload.mgr;

import com.wjybxx.fastjgame.reload.file.FileReader;
import com.wjybxx.fastjgame.reload.mgr.ReloadUtils.FileStat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * date - 2020/12/2
 * github - https://github.com/hl845740757
 */
class FileReloadTask implements Runnable {

    private final List<TaskContext> taskContextList;
    private final Executor executor;
    private final long timeoutFindChangedFiles;
    private final long timeoutReadFiles;
    private final CompletableFuture<List<TaskResult>> future;

    private FileReloadTask(List<TaskContext> taskContextList,
                           Executor executor, long timeoutFindChangedFiles, long timeoutReadFiles,
                           CompletableFuture<List<TaskResult>> future) {
        // 拷贝保证安全性
        this.taskContextList = taskContextList;
        this.executor = executor;
        this.timeoutFindChangedFiles = timeoutFindChangedFiles;
        this.timeoutReadFiles = timeoutReadFiles;
        this.future = future;
    }

    static CompletableFuture<List<TaskResult>> runAsync(List<TaskMetadata> taskMetadataList,
                                                        Executor executor, long timeoutFindChangedFiles, long timeoutReadFiles) {
        if (taskMetadataList.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        // 拷贝加转换
        final List<TaskContext> taskContextList = new ArrayList<>(taskMetadataList.size());
        for (TaskMetadata taskMetadata : taskMetadataList) {
            taskContextList.add(new TaskContext(taskMetadata));
        }

        final CompletableFuture<List<TaskResult>> future = new CompletableFuture<>();
        final FileReloadTask fileReloadTask = new FileReloadTask(taskContextList, executor, timeoutFindChangedFiles, timeoutReadFiles, future);
        executor.execute(fileReloadTask);
        return future;
    }

    static class TaskMetadata {

        final File file;
        final FileReader<?> reader;
        final FileStat oldFileStat;

        TaskMetadata(File file, FileReader<?> reader, FileStat oldFileStat) {
            this.file = Objects.requireNonNull(file);
            this.reader = Objects.requireNonNull(reader);
            this.oldFileStat = oldFileStat;
        }
    }

    static class TaskResult {

        final TaskMetadata taskMetadata;
        final FileStat fileStat;
        final Object fileData;

        TaskResult(TaskMetadata taskMetadata, FileStat fileStat, Object fileData) {
            this.taskMetadata = taskMetadata;
            this.fileStat = fileStat;
            this.fileData = fileData;
        }
    }

    // 内部实现

    @Override
    public void run() {
        try {
            statisticFileStats()
                    .thenRun(this::retainChangedFiles)
                    .thenCompose(aVoid -> readChangedFiles())
                    .whenComplete(this::finish);
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }
    }

    private CompletableFuture<Void> statisticFileStats() {
        final List<CompletableFuture<?>> futureList = new ArrayList<>(taskContextList.size());
        for (TaskContext taskContext : taskContextList) {
            futureList.add(CompletableFuture.runAsync(new StatisticSingleFileStatTask(taskContext), executor));
        }
        return CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
                .orTimeout(timeoutFindChangedFiles, TimeUnit.MILLISECONDS);
    }

    private void retainChangedFiles() {
        taskContextList.removeIf(taskContext -> taskContext.taskMetadata.oldFileStat == taskContext.fileStat);
    }

    private CompletableFuture<Void> readChangedFiles() {
        if (taskContextList.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final List<CompletableFuture<?>> futureList = new ArrayList<>(taskContextList.size());
        for (TaskContext context : taskContextList) {
            Objects.requireNonNull(context.fileStat);
            futureList.add(CompletableFuture.runAsync(new ReadSingleFileTask(context), executor));
        }
        return CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new))
                .orTimeout(timeoutReadFiles, TimeUnit.MILLISECONDS);
    }

    private void finish(Void aVoid, Throwable throwable) {
        if (throwable != null) {
            future.completeExceptionally(throwable);
        } else {
            final List<TaskResult> resultBeanList = new ArrayList<>(taskContextList.size());
            for (TaskContext taskContext : taskContextList) {
                resultBeanList.add(new TaskResult(taskContext.taskMetadata, taskContext.fileStat, taskContext.fileData));
            }
            future.complete(resultBeanList);
        }
    }

    private static class TaskContext {

        final TaskMetadata taskMetadata;

        FileStat fileStat;
        Object fileData;

        TaskContext(TaskMetadata taskMetadata) {
            this.taskMetadata = taskMetadata;
        }

        File getFile() {
            return taskMetadata.file;
        }

        FileReader<?> getReader() {
            return taskMetadata.reader;
        }

        FileStat getOldFileStat() {
            return taskMetadata.oldFileStat;
        }

    }

    /**
     * 统计单个文件状态
     */
    private static class StatisticSingleFileStatTask implements Runnable {

        final TaskContext context;

        private StatisticSingleFileStatTask(TaskContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                context.fileStat = ReloadUtils.statOfFile(context.getFile(), context.getOldFileStat());
            } catch (Exception e) {
                throw new RuntimeException("fileName: " + context.getFile().getName(), e);
            }
        }
    }

    /**
     * 读取单个文件内容
     */
    private static class ReadSingleFileTask implements Runnable {

        final TaskContext context;

        private ReadSingleFileTask(TaskContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                final Object fileData = context.getReader().read(context.getFile());
                Objects.requireNonNull(fileData, context.getReader().getClass().getName());
                context.fileData = fileData;
            } catch (Exception e) {
                throw new RuntimeException("fileName: " + context.getFile().getName(), e);
            }
        }
    }
}
