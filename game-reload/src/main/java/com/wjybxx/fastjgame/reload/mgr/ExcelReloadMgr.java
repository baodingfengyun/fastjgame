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
import com.google.common.collect.Sets;
import com.wjybxx.fastjgame.reload.excel.SheetCacheBuilder;
import com.wjybxx.fastjgame.reload.excel.SheetName;
import com.wjybxx.fastjgame.reload.excel.SheetReader;
import com.wjybxx.fastjgame.reload.excel.SheetReloadListener;
import com.wjybxx.fastjgame.reload.file.FileName;
import com.wjybxx.fastjgame.reload.file.FileReader;
import com.wjybxx.fastjgame.reload.file.FileReloadCallback;
import com.wjybxx.fastjgame.reload.file.FileReloadListener;
import com.wjybxx.fastjgame.util.excel.CellValueParser;
import com.wjybxx.fastjgame.util.excel.ExcelUtils;
import com.wjybxx.fastjgame.util.excel.Sheet;
import com.wjybxx.fastjgame.util.misc.StepWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 表格热更新管理器
 * <p>
 * 约定：该类不修改方法传入的任何集合，也不保留其引用。
 *
 * @author wjybxx
 * date - 2020/11/17
 * github - https://github.com/hl845740757
 */
public class ExcelReloadMgr implements ExtensibleObject {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReloadMgr.class);

    private final Map<String, Object> blackboard = new HashMap<>();

    private final String projectResDir;
    private final String configDirName;
    private final FileReloadMgr fileReloadMgr;
    private final SheetDataMgr sheetDataMgr;
    private final Supplier<CellValueParser> parserSupplier;

    private final SheetDataContainer sheetDataContainer = new SheetDataContainer();
    private final Map<SheetName<?>, ListenerWrapper> listenerWrapperMap = new IdentityHashMap<>(50);

    private final Map<SheetName<?>, ReaderMetadata<?>> readerMetadataMap = new IdentityHashMap<>(500);
    private final Map<Class<?>, BuilderMetadata<?>> builderMetadataMap = new IdentityHashMap<>(50);

    private final ExcelReloadListener excelReloadListener = new ExcelReloadListener();
    private final Map<FileName<?>, ExcelReader> excelReaderMap = new IdentityHashMap<>(300);

    /**
     * @param projectResDir  项目资源目录
     * @param configDirName  excel表格所在的文件夹
     * @param fileReloadMgr  真正管理文件更新的地方
     * @param sheetDataMgr   应用自身管理数据的地方
     * @param parserSupplier 用于解析excel表格的内容，注意：如果不能确保线程安全，请每次new新对象。
     */
    public ExcelReloadMgr(String projectResDir, String configDirName,
                          FileReloadMgr fileReloadMgr, SheetDataMgr sheetDataMgr,
                          Supplier<CellValueParser> parserSupplier) {
        this.projectResDir = Objects.requireNonNull(projectResDir, "projectResDir");
        this.configDirName = Objects.requireNonNull(configDirName, "configDirName");
        this.fileReloadMgr = Objects.requireNonNull(fileReloadMgr, "fileReloadMgr");
        this.sheetDataMgr = Objects.requireNonNull(sheetDataMgr, "sheetDataMgr");
        this.parserSupplier = Objects.requireNonNull(parserSupplier, "parserSupplier");
    }

    /**
     * 注意：先注册{@link SheetReader}，再注册{@link SheetCacheBuilder}
     */
    public final void registerReaders(Collection<? extends SheetReader<?>> readers) throws IOException {
        if (readers.isEmpty()) {
            return;
        }

        for (SheetReader<?> reader : readers) {
            final SheetName<?> sheetName = reader.sheetName();
            if (readerMetadataMap.containsKey(sheetName)) {
                final String msg = String.format("sheetName has more than one associated reader, sheetName: %s, reader: %s", sheetName, reader.getClass().getName());
                throw new IllegalArgumentException(msg);
            }
            readerMetadataMap.put(sheetName, new ReaderMetadata<>(reader));
        }

        if (excelReaderMap.size() > 0) {
            // 这里只是为了方便后面采取全部注册的方式
            fileReloadMgr.unregisterReader(excelReaderMap.values());
            // listener一定要取消注册，否则监听的文件不全
            fileReloadMgr.unregisterListener(excelReaderMap.keySet(), excelReloadListener);
            // 清理数据
            excelReaderMap.clear();
        }

        // 根据SheetReader创建对应的Excel读取实现
        excelReaderMap.putAll(createExcelReaders());

        // 表格缺失检查
        ensureReaderSheetExist();

        // 重新注册excel文件读取实现
        fileReloadMgr.registerReaders(excelReaderMap.values());
        // 重新监控所有文件
        fileReloadMgr.registerListener(excelReaderMap.keySet(), excelReloadListener);
    }

    private Map<FileName<?>, ExcelReader> createExcelReaders() throws IOException {
        // 该集合不可以修改，否则会影响readExcelSheetNames调用
        final Map<String, SheetReader<?>> stringSheetName2ReaderMap = readerMetadataMap.values().stream()
                .map(e -> e.reader)
                .collect(Collectors.toUnmodifiableMap(e -> e.sheetName().name(), e -> e));
        // 已发现的页签名称，用于判重
        final Set<String> existSheetNameSet = Sets.newHashSetWithExpectedSize(readerMetadataMap.size());

        final List<File> allExcelFiles = findAllExcelFiles(projectResDir, configDirName);
        final Map<FileName<?>, ExcelReader> excelReaderMap = new IdentityHashMap<>(allExcelFiles.size());
        final Set<String> existExcelNameSet = Sets.newHashSetWithExpectedSize(allExcelFiles.size());
        for (File file : allExcelFiles) {
            if (!existExcelNameSet.add(file.getName())) {
                throw new IllegalArgumentException("excelName is duplicate, excelName: " + file.getName());
            }

            final List<String> sheetNameList = ExcelUtils.readExcelSheetNames(file, stringSheetName2ReaderMap::containsKey);
            if (sheetNameList.isEmpty()) {
                logger.info("skip excel, sheetNames is empty, excelName: {}", file.getName());
                continue;
            }

            // excel的sheet重名检查（TODO 语言表怎么处理？）
            for (String sheetName : sheetNameList) {
                if (!existSheetNameSet.add(sheetName)) {
                    final String msg = String.format("sheetName is duplicate, excelName: %s, sheetName: %s", file.getName(), sheetName);
                    throw new IllegalArgumentException(msg);
                }
            }

            final FileName<ExcelFileData> fileName = findFileName(configDirName, file);
            final Map<String, SheetReader<?>> sheetReaderMap = Maps.newHashMapWithExpectedSize(sheetNameList.size());
            for (String sheetName : sheetNameList) {
                sheetReaderMap.put(sheetName, stringSheetName2ReaderMap.get(sheetName));
            }
            excelReaderMap.put(fileName, new ExcelReader(file.getName(), fileName, sheetReaderMap, parserSupplier));
        }
        return excelReaderMap;
    }

    private static FileName<ExcelFileData> findFileName(String configDirName, File file) {
        final LinkedList<String> nameList = new LinkedList<>();
        nameList.addFirst(file.getName());
        do {
            file = file.getParentFile();
            nameList.addFirst(file.getName());
        } while (!file.getName().equals(configDirName));

        final StringBuilder stringBuilder = new StringBuilder(30);
        for (String element : nameList) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append('/');
            }
            stringBuilder.append(element);
        }
        // configDirName/xxx/xxx/fileName
        final String relativePath = stringBuilder.toString();
        return FileName.valueOf(relativePath);
    }

    private static List<File> findAllExcelFiles(final String projectResDir, final String configDirName) {
        final ArrayList<File> result = new ArrayList<>(300);
        final String configDirPath = projectResDir + File.separator + configDirName;
        ReloadUtils.recurseDir(new File(configDirPath), result, file -> file.getName().endsWith(".xlsx"));
        return result;
    }

    /**
     * reader存在，对应的sheet必须存在
     */
    private void ensureReaderSheetExist() {
        final Set<String> excelSheetNameSet = excelReaderMap.values().stream()
                .flatMap(excelReader -> excelReader.sheetReaderMap.keySet().stream())
                .collect(Collectors.toSet());

        for (SheetName<?> sheetName : readerMetadataMap.keySet()) {
            if (!excelSheetNameSet.contains(sheetName.name())) {
                throw new IllegalStateException("missing sheet of reader, sheetName: " + sheetName);
            }
        }
    }

    /**
     * 注意：先注册{@link SheetReader}，再注册{@link SheetCacheBuilder}
     */
    public final void registerCacheBuilders(Collection<? extends SheetCacheBuilder<?>> cacheBuilders) {
        for (SheetCacheBuilder<?> builder : cacheBuilders) {
            final Class<?> builderType = builder.getClass();
            final Set<SheetName<?>> builderSheetNames = builder.sheetNames();
            // 关注的表格不可以为空
            if (builderSheetNames.isEmpty()) {
                throw new IllegalStateException("sheetNames is empty, builder: " + builderType.getName());
            }
            // reader缺失检查
            for (SheetName<?> sheetName : builderSheetNames) {
                if (!readerMetadataMap.containsKey(sheetName)) {
                    final String msg = String.format("reader is null, sheetName: %s, builder: %s", sheetName, builderType.getName());
                    throw new IllegalStateException(msg);
                }
            }
            builderMetadataMap.put(builderType, new BuilderMetadata<>(builder, builderSheetNames));
        }
    }

    /**
     * 获取某个表格的数据。
     * 注意：这是给扩展预留的接口，不是应用层使用的接口。
     */
    public <T> T getSheetData(SheetName<T> sheetName) {
        return sheetDataContainer.getSheetData(sheetName);
    }

    public Object getCacheData(Class<?> builderType) {
        return sheetDataContainer.getCacheData(builderType);
    }

    public Set<SheetName<?>> getSheetNames(Set<FileName<?>> fileNames) {
        Objects.requireNonNull(fileNames, "fileNames");
        return fileNames.stream()
                .filter(Objects::nonNull)
                .map(excelReaderMap::get)
                .filter(Objects::nonNull)
                .flatMap(excelReader -> excelReader.sheetReaderMap.values().stream())
                .map(SheetReader::sheetName)
                .collect(Collectors.toSet());
    }

    /**
     * 启服加载所有表格
     * 注意：需要先调用{@link FileReloadMgr#loadAll()}
     */
    public void loadAll() throws Exception {
        reloadImpl(excelReaderMap.keySet(), ReloadMode.START_SERVER);
    }

    /**
     * 加载所有变化的表格
     */
    public void reloadAll(@Nullable FileReloadCallback callback) {
        fileReloadMgr.reloadScope(excelReaderMap.keySet(), callback);
    }

    /**
     * 强制热加载指定的表格
     * 在指定sheetName的情况下，不论表格是否变化，都执行重加载和回调逻辑。
     * <p>
     * Q: 为何如此设计？
     * A: 期望在某些表格“未变化”的情况下重新加载表格。
     * 这可能是md5相同的偶然事件，也可能是为了执行某些回调逻辑。
     *
     * @param excelNameSet 要更新的excel名字
     */
    public void forceReload(@Nonnull Set<String> excelNameSet, @Nullable FileReloadCallback callback) throws Exception {
        final Map<String, FileName<ExcelFileData>> stringFileNameMap = excelReaderMap.values().stream()
                .collect(Collectors.toMap(excelReader -> excelReader.stringFileName, excelReader -> excelReader.fileName));
        // 转换类型
        final Set<FileName<?>> scope = Sets.newHashSetWithExpectedSize(excelNameSet.size());
        for (String excelName : excelNameSet) {
            final FileName<?> fileName = excelName.endsWith(".xlsx") ? stringFileNameMap.get(excelName) : stringFileNameMap.get(excelName + ".xlsx");
            if (fileName == null) {
                throw new IllegalArgumentException("unknown excel, excelName: " + excelName);
            }
            scope.add(fileName);
        }
        // 执行热更新
        fileReloadMgr.reloadScope(scope, callback);
    }

    /**
     * 注册一个表格回调，当关心的表格中的一个或多个产生热更新时，将通知指定的监听器。
     *
     * @param sheetNameSet 关注的表格名字
     * @param listener     回调处理器
     */
    public void registerListener(Set<SheetName<?>> sheetNameSet, SheetReloadListener listener) {
        Objects.requireNonNull(sheetNameSet, "sheetNameSet");
        Objects.requireNonNull(listener, "listener");

        if (sheetNameSet.isEmpty()) {
            throw new IllegalArgumentException("sheetNameSet is empty");
        }

        // 防御性拷贝
        sheetNameSet = Set.copyOf(sheetNameSet);
        final ListenerWrapper listenerWrapper = new DefaultListenerWrapper(sheetNameSet, listener);

        for (SheetName<?> sheetName : sheetNameSet) {
            Objects.requireNonNull(sheetName, "sheetName");
            final ReaderMetadata<?> readerMetaData = readerMetadataMap.get(sheetName);
            if (readerMetaData == null) {
                throw new IllegalArgumentException("Unknown sheetName " + sheetName);
            }

            final ListenerWrapper existListenerWrapper = listenerWrapperMap.get(sheetName);
            if (existListenerWrapper == null) {
                // 一般一个listener
                listenerWrapperMap.put(sheetName, listenerWrapper);
                return;
            }

            if (existListenerWrapper instanceof CompositeListenerWrapper) {
                // 超过两个listener
                ((CompositeListenerWrapper) existListenerWrapper).addChild(listenerWrapper);
            } else {
                // 两个listener
                listenerWrapperMap.put(sheetName, new CompositeListenerWrapper(existListenerWrapper, listenerWrapper));
            }
        }
    }

    // region 内部实现

    private void reloadImpl(Set<FileName<?>> changedFileNameSet, ReloadMode reloadMode) throws Exception {
        if (changedFileNameSet.isEmpty()) {
            return;
        }

        final StepWatch stepWatch = StepWatch.createStarted("ExcelReloadMrg:reloadImpl");
        // 合并所有sheet读取结果
        final Map<SheetName<?>, Object> sheetDataMap = new IdentityHashMap<>(changedFileNameSet.size() * 3);
        for (FileName<?> fileName : changedFileNameSet) {
            final ExcelFileData fileData = (ExcelFileData) fileReloadMgr.getFileData(fileName);
            sheetDataMap.putAll(fileData.sheetDataMap);
        }

        // 在沙箱上进行修改和一致性校验
        final SandBox sandBox = createSandBox(sheetDataMgr, sheetDataContainer);
        assignSheetData(sheetDataMap, sandBox.sheetDataMgr, sandBox.sheetDataContainer);
        final Map<Class<?>, Object> sheetCacheMap = buildSheetCache(sheetDataMap.keySet(), sandBox.sheetDataContainer);
        assignCacheData(sheetCacheMap, sandBox.sheetDataMgr, sandBox.sheetDataContainer);
        stepWatch.logStep("buildSandbox");

        // 检查所有表格之间的一致性(开销可能很大，热更前应该在本地进行热更和启服测试)
        // TODO 以后可以本地测试一下开销，再决定热更新时是否也开启检测
        // 这里是沙箱存在的意义
        validateOther(sandBox.sheetDataMgr);
        stepWatch.logStep("validateOther");

        // 赋值到真实环境（这里其实已经完成了读表逻辑）
        assignSheetData(sheetDataMap, sheetDataMgr, sheetDataContainer);
        assignCacheData(sheetCacheMap, sheetDataMgr, sheetDataContainer);

        // 执行回调逻辑（这里可能产生异常）
        if (reloadMode != ReloadMode.START_SERVER) {
            notifyListeners(Set.copyOf(sheetDataMap.keySet()));
            stepWatch.logStep("notifyListeners");
        }

        logger.info(stepWatch.getLog());
    }

    private SandBox createSandBox(SheetDataMgr dataMgrProtoType, SheetDataContainer containerPrototype) {
        // 重建SheetDataMgr和SheetDataContainer
        final SheetDataMgr sandBoxSheetDataMgr = dataMgrProtoType.newInstance();
        ReloadUtils.ensureSameType(dataMgrProtoType, sandBoxSheetDataMgr);
        final SheetDataContainer sandBoxSheetDataContainer = new SheetDataContainer();
        assignSheetData(containerPrototype.getAllSheetData(), sandBoxSheetDataMgr, sandBoxSheetDataContainer);
        assignCacheData(containerPrototype.getAllCacheData(), sandBoxSheetDataMgr, sandBoxSheetDataContainer);
        return new SandBox(sandBoxSheetDataMgr, sandBoxSheetDataContainer);
    }

    private void assignSheetData(Map<SheetName<?>, ?> sheetDataMap, SheetDataMgr sheetDataMgr, SheetDataContainer sheetDataContainer) {
        for (Map.Entry<SheetName<?>, ?> entry : sheetDataMap.entrySet()) {
            @SuppressWarnings("unchecked") final SheetName<Object> sheetName = (SheetName<Object>) entry.getKey();
            final Object sheetData = entry.getValue();
            final ReaderMetadata<?> readerMetaData = readerMetadataMap.get(sheetName);
            @SuppressWarnings("unchecked") final SheetReader<Object> reader = (SheetReader<Object>) readerMetaData.reader;

            reader.assignTo(sheetData, sheetDataMgr);
            sheetDataContainer.putSheetData(sheetName, sheetData);
        }
    }

    private void assignCacheData(Map<Class<?>, ?> cacheMap, SheetDataMgr sheetDataMgr, SheetDataContainer sheetDataContainer) {
        for (Map.Entry<Class<?>, ?> entry : cacheMap.entrySet()) {
            final Class<?> builderType = entry.getKey();
            final Object cache = entry.getValue();
            final BuilderMetadata<?> builderMetadata = builderMetadataMap.get(builderType);
            @SuppressWarnings("unchecked") final SheetCacheBuilder<Object> builder = (SheetCacheBuilder<Object>) builderMetadata.builder;

            builder.assignTo(cache, sheetDataMgr);
            sheetDataContainer.putCacheData(builderType, cache);
        }
    }

    private Map<Class<?>, Object> buildSheetCache(Collection<SheetName<?>> sheetNames, SheetDataContainer sheetDataContainer) {
        final Map<Class<?>, Object> result = new IdentityHashMap<>(20);
        // 这里数量较少，直接遍历所有的builder(减少不必要的依赖)
        for (BuilderMetadata<?> builderMetadata : builderMetadataMap.values()) {
            for (SheetName<?> sheetName : builderMetadata.sheetNamSet) {
                if (sheetNames.contains(sheetName)) {
                    final SheetDataProviderImpl sheetDataProvider = new SheetDataProviderImpl(sheetDataContainer, builderMetadata.sheetNamSet);
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

    private void validateOther(SheetDataMgr sheetDataMgr) {
        for (final ReaderMetadata<?> readerMetaData : readerMetadataMap.values()) {
            readerMetaData.reader.validateOther(sheetDataMgr);
        }

        for (final BuilderMetadata<?> builderMetadata : builderMetadataMap.values()) {
            builderMetadata.builder.validateOther(sheetDataMgr);
        }
    }

    private void notifyListeners(Set<SheetName<?>> allChangedSheetNames) throws Exception {
        final Set<ListenerWrapper> invokedListeners = Collections.newSetFromMap(new IdentityHashMap<>(20));
        for (SheetName<?> sheetName : allChangedSheetNames) {
            final ListenerWrapper listenerWrapper = listenerWrapperMap.get(sheetName);
            if (listenerWrapper == null) {
                continue;
            }
            if (!invokedListeners.add(listenerWrapper)) {
                // 已通知
                continue;
            }
            // 不捕获异常，这会中断热更新流程，上层保证文件状态不被更新，可以再次执行
            listenerWrapper.afterReload(allChangedSheetNames);
        }
    }

    private enum ReloadMode {
        START_SERVER,
        FILE_CHANGED,
    }

    private static class ExcelFileData {

        final Map<SheetName<?>, Object> sheetDataMap;

        ExcelFileData(Map<SheetName<?>, Object> sheetDataMap) {
            this.sheetDataMap = sheetDataMap;
        }
    }

    private static class ExcelReader implements FileReader<ExcelFileData> {

        final String stringFileName;
        final FileName<ExcelFileData> fileName;
        final Map<String, SheetReader<?>> sheetReaderMap;
        final Supplier<CellValueParser> parserSupplier;

        ExcelReader(String stringFileName,
                    FileName<ExcelFileData> fileName,
                    Map<String, SheetReader<?>> sheetReaderMap,
                    Supplier<CellValueParser> parserSupplier) {
            this.stringFileName = stringFileName;
            this.fileName = fileName;
            this.sheetReaderMap = sheetReaderMap;
            this.parserSupplier = parserSupplier;
        }

        @Nonnull
        @Override
        public FileName<ExcelFileData> fileName() {
            return fileName;
        }

        @Nonnull
        @Override
        public ExcelFileData read(File file) throws Exception {
            // 读取Excel到内存
            final Map<String, Sheet> sheetMap = ExcelUtils.readExcel(file, parserSupplier.get(), sheetReaderMap::containsKey);
            final Map<SheetName<?>, Object> sheetDataMap = Maps.newHashMapWithExpectedSize(sheetMap.size());
            for (Map.Entry<String, Sheet> entry : sheetMap.entrySet()) {
                final String sheetName = entry.getKey();
                final Sheet sheet = entry.getValue();
                // 读取Sheet到自定义模板
                final SheetReader<?> sheetReader = sheetReaderMap.get(sheetName);
                final Object sheetData = sheetReader.read(sheet);
                // null检查
                Objects.requireNonNull(sheetData, sheetReader.getClass().getName());
                sheetDataMap.put(sheetReader.sheetName(), sheetData);
            }
            return new ExcelFileData(sheetDataMap);
        }

        @Override
        public void assignTo(ExcelFileData fileData, FileDataMgr fileDataMgr) {
            // 不展开
        }

        @Override
        public void validateOther(FileDataMgr fileDataMgr) {
            // 不验证
        }

    }

    private class ExcelReloadListener implements FileReloadListener {

        @Override
        public void afterReload(Set<FileName<?>> fileNameSet, Set<FileName<?>> changedFileNameSet) throws Exception {
            reloadImpl(changedFileNameSet, ReloadMode.FILE_CHANGED);
        }

    }

    private static class ReaderMetadata<T> {

        final SheetReader<T> reader;

        ReaderMetadata(SheetReader<T> reader) {
            this.reader = reader;
        }
    }

    private static class BuilderMetadata<T> {

        final SheetCacheBuilder<T> builder;
        final Set<SheetName<?>> sheetNamSet;

        BuilderMetadata(SheetCacheBuilder<T> builder, Collection<SheetName<?>> sheetNames) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.sheetNamSet = Set.copyOf(Objects.requireNonNull(sheetNames, "sheetNames"));
        }
    }

    private static class SheetDataProviderImpl implements SheetCacheBuilder.SheetDataProvider {

        final SheetDataContainer sheetDataContainer;

        final Set<SheetName<?>> sheetNamSet;

        SheetDataProviderImpl(SheetDataContainer sheetDataContainer, Set<SheetName<?>> sheetNamSet) {
            this.sheetDataContainer = sheetDataContainer;
            this.sheetNamSet = sheetNamSet;
        }

        @Override
        public <T> T getSheetData(SheetName<T> sheetName) {
            if (sheetNamSet.contains(sheetName)) {
                return sheetDataContainer.getSheetData(sheetName);
            }
            throw new IllegalArgumentException("restricted sheetName: " + sheetName);
        }

    }

    private interface ListenerWrapper {

        void afterReload(Set<SheetName<?>> allChangedSheetNames) throws Exception;
    }

    private static class DefaultListenerWrapper implements ListenerWrapper {

        final Set<SheetName<?>> sheetNameSet;
        final SheetReloadListener reloadListener;

        DefaultListenerWrapper(Set<SheetName<?>> sheetNameSet, SheetReloadListener reloadListener) {
            // 外部保证是个不可变集合
            this.sheetNameSet = sheetNameSet;
            this.reloadListener = reloadListener;
        }

        @Override
        public void afterReload(Set<SheetName<?>> allChangedSheetNames) throws Exception {
            final Set<SheetName<?>> changedSheetNames = new HashSet<>(sheetNameSet);
            changedSheetNames.retainAll(allChangedSheetNames);
            reloadListener.afterReload(sheetNameSet, changedSheetNames);
        }
    }

    private static class CompositeListenerWrapper implements ListenerWrapper {

        final List<ListenerWrapper> children;

        CompositeListenerWrapper(ListenerWrapper first, ListenerWrapper second) {
            this.children = new ArrayList<>(2);
            this.children.add(first);
            this.children.add(second);
        }

        void addChild(ListenerWrapper child) {
            this.children.add(child);
        }

        @Override
        public void afterReload(Set<SheetName<?>> allChangedSheetNames) throws Exception {
            for (ListenerWrapper child : children) {
                child.afterReload(allChangedSheetNames);
            }
        }
    }

    static class SandBox {

        final SheetDataMgr sheetDataMgr;
        final SheetDataContainer sheetDataContainer;

        SandBox(SheetDataMgr sheetDataMgr, SheetDataContainer sheetDataContainer) {
            this.sheetDataMgr = sheetDataMgr;
            this.sheetDataContainer = sheetDataContainer;
        }
    }

    static class SheetDataContainer {

        private final Map<SheetName<?>, Object> sheetDataMap;
        private final Map<Class<?>, Object> cacheDataMap;

        SheetDataContainer() {
            this.sheetDataMap = new IdentityHashMap<>(500);
            this.cacheDataMap = new IdentityHashMap<>(50);
        }

        final <T> void putSheetData(SheetName<T> sheetName, T data) {
            Objects.requireNonNull(sheetName, "sheetName");
            Objects.requireNonNull(data, "data");
            sheetDataMap.put(sheetName, data);
        }

        final <T> T getSheetData(SheetName<T> sheetName) {
            Objects.requireNonNull(sheetName, "sheetName");
            @SuppressWarnings("unchecked") final T result = (T) sheetDataMap.get(sheetName);
            if (result == null) {
                throw new IllegalArgumentException("sheetData is null, sheetName: " + sheetName);
            }
            return result;
        }

        final Map<SheetName<?>, Object> getAllSheetData() {
            return new IdentityHashMap<>(sheetDataMap);
        }

        final void putCacheData(Class<?> builderType, Object cacheData) {
            Objects.requireNonNull(builderType, "builderType");
            Objects.requireNonNull(cacheData, "cacheData");
            cacheDataMap.put(builderType, cacheData);
        }

        final Object getCacheData(Class<?> builderType) {
            Objects.requireNonNull(builderType, "builderType");
            final Object result = cacheDataMap.get(builderType);
            if (result == null) {
                throw new IllegalArgumentException("cacheData is null, builderType: " + builderType);
            }
            return result;
        }

        final Map<Class<?>, Object> getAllCacheData() {
            return new IdentityHashMap<>(cacheDataMap);
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
