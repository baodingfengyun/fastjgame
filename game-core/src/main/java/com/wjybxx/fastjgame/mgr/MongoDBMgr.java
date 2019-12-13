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

package com.wjybxx.fastjgame.mgr;

import com.google.inject.Inject;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.wjybxx.fastjgame.annotation.EventLoopSingleton;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.utils.ZKPathUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.function.Consumer;

import static com.mongodb.client.model.Projections.*;
import static com.wjybxx.fastjgame.utils.GameUtils.isBlank;

/**
 * MongoDB驱动封装类。World级别的单例，不同的world有不同的需求
 * <p>
 * 几个重要的类:
 * {@link Updates}
 * {@link Filters}
 * {@link Projections}
 * {@link IndexDocument}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/20 19:42
 * github - https://github.com/hl845740757
 */
@EventLoopSingleton
@NotThreadSafe
public abstract class MongoDBMgr {
    /**
     * 批量查询时，每次返回的数据量大小
     */
    private static final int BATCH_SIZE = 500;
    /**
     * 索引不存在对应的错误码
     */
    private static final int ERROR_CODE_INDEX_NOT_EXIST = 27;

    /**
     * A MongoDB client with internal connection pooling. For most applications,
     * you should have one MongoClient instance for the entire JVM.
     */
    private final MongoClient mongoClient;

    /**
     * 数据库缓存；
     * 之前没做缓存，后面发现要么把枚举变成实例，要么做缓存，否则使用起来不够方便；
     * 子类在构造时需要缓存好需要的数据库。
     */
    protected final EnumMap<MongoDBType, MongoDatabase> dbMap = new EnumMap<>(MongoDBType.class);

    @Inject
    public MongoDBMgr(GameConfigMgr gameConfigMgr, CuratorMgr curatorMgr) throws Exception {
        MongoClientOptions mongoClientOptions = MongoClientOptions
                .builder()
                .connectTimeout(gameConfigMgr.getMongoConnectionTimeoutMs())
                // 每个host保持2个连接即可
                .connectionsPerHost(gameConfigMgr.getMongoConnectionsPerHost())
                .codecRegistry(buildCodecRegistry())
                .build();

        // mongodb在zookeeper中的配置
        Map<String, byte[]> childrenData = curatorMgr.getChildrenData(mongoConfigPath());

        ZKNodeConfigWrapper zkMongoConfig = new ZKNodeConfigWrapper(childrenData);

        HostAndPort serverHostAndPort = HostAndPort.parseHostAndPort(zkMongoConfig.getAsString("serveraddress"));
        ServerAddress addr = new ServerAddress(serverHostAndPort.getHost(), serverHostAndPort.getPort());

        String userName = zkMongoConfig.getAsString("user");
        String password = zkMongoConfig.getAsString("password");
        String authdb = zkMongoConfig.getAsString("authdb");
        // 判断是否需要验证
        if (isBlank(userName) || isBlank(password) || isBlank(authdb)) {
            this.mongoClient = new MongoClient(addr, mongoClientOptions);
        } else {
            MongoCredential credential = MongoCredential.createCredential(userName, authdb, password.toCharArray());
            this.mongoClient = new MongoClient(addr, credential, mongoClientOptions);
        }
    }

    /**
     * 返回mongo数据库的配置路径；
     * 如果连接的不是全局数据库的话，子类可以覆盖，。
     *
     * @return zk path
     */
    protected String mongoConfigPath() {
        return ZKPathUtils.mongoConfigPath();
    }

    /**
     * 缓存需要使用的数据库
     */
    protected abstract void cacheDB();

    /**
     * 创建数据库索引
     */
    protected abstract void createIndex();


    public void close() {
        mongoClient.close();
    }


    /**
     * 构建codecRegistry
     *
     * @return
     */
    private CodecRegistry buildCodecRegistry() {
        return CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                independentCodecs(),
                dependentCodecs()
        );
    }

    /**
     * 不依赖其他codec的codec
     *
     * @return
     */
    private CodecRegistry independentCodecs() {
        List<Codec<?>> independentCodecs = new ArrayList<>(childIndependentCodecs());
        // TODO 公共codec在这里添加，各个模块都需要的codec

        return CodecRegistries.fromCodecs(independentCodecs);
    }

    ;

    protected List<Codec<?>> childIndependentCodecs() {
        return Collections.emptyList();
    }

    /**
     * 依赖其它codec的codec
     *
     * @return
     */
    private CodecRegistry dependentCodecs() {
        // TODO 父类公共的codec处理器
        CodecProvider dependentProvider = new CodecProvider() {
            /**
             * 获取指定类的codec
             * @param clazz 要进行编解码的类的信息
             * @param registry 在这里应该是根节点
             * @param <T> 期望编解码的codec
             * @return 负责clazz编解码的codec
             */
            @Override
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                return null;
            }
        };

        // CodecProvider 这种方式可以解决复杂依赖 和 循环依赖问题
        CodecProvider childDependentProvider = childDependentProvider();
        if (null != childDependentProvider) {
            return CodecRegistries.fromProviders(dependentProvider, childDependentProvider);
        } else {
            return CodecRegistries.fromProviders(dependentProvider);
        }
    }

    /**
     * 子类的具有依赖的{@link CodecProvider}，
     *
     * @return default null
     */
    protected CodecProvider childDependentProvider() {
        return null;
    }


    protected MongoDatabase getMongoDatabase(String dataBaseName) {
        return mongoClient.getDatabase(dataBaseName);
    }

    /**
     * 为何做了缓存？因为存在这么一个问题:
     * <p>
     * 如果每个服数据库是独立的，使用枚举的名字作为数据库名字即可，不需要缓存。
     * 如果数据库全局唯一的，那么数据库的名字需要有区分度。
     * 枚举的名字就不能做数据库名字，枚举需要映射到另一个东西上，可以是名字，也可以是{@link MongoDatabase}；
     *
     * @param databaseType 数据库类型
     * @return
     */
    private MongoDatabase getMongoDatabase(MongoDBType databaseType) {
        return dbMap.get(databaseType);
    }

    /**
     * 获取集合，对集合中的元素不进行codec操作
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @return document类型的集合 {@link Document}是mongodb的document映射类，
     * 使用{@link Document}而不是{@link BsonDocument}，接口会更友好。
     */
    public MongoCollection<Document> getCollection(MongoDBType databaseType, MongoCollectionName collectionName) {
        return getMongoDatabase(databaseType).getCollection(collectionName.name());
    }

    /**
     * 获取指定数据库的一个集合
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合的名字
     * @param documentClass  该集合里面存储的是什么类型的文档。它也暗示着集合的设计要求：一个集合中尽量存储同类型的数据。
     * @param <T>
     * @return 对应的集合
     */
    public <T> MongoCollection<T> getCollection(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass) {
        return getMongoDatabase(databaseType).getCollection(collectionName.name(), documentClass);
    }

    /**
     * 删除指定集合（慎用）
     *
     * @param databaseType       数据库名字
     * @param collectionNameEnum 集合名字
     */
    public void dropCollection(MongoDBType databaseType, MongoCollectionName collectionNameEnum) {
        getCollection(databaseType, collectionNameEnum).drop();
    }

    // region 增删改查 CRUD

    /**
     * 插入一个文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param tDocument      插入的对象(可codec的对象)
     * @param <T>            该集合中存储的数据类型
     */
    public <T> void insertOne(MongoDBType databaseType, MongoCollectionName collectionName, @Nonnull T tDocument) {
        @SuppressWarnings("unchecked")
        Class<T> documentClass = (Class<T>) tDocument.getClass();
        getCollection(databaseType, collectionName, documentClass)
                .insertOne(tDocument);
    }

    /**
     * 插入多个文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param tDocumentList  要插入的对象
     * @param <T>            元素的类型
     */
    public <T> void insertMany(MongoDBType databaseType, MongoCollectionName collectionName, @Nonnull List<T> tDocumentList) {
        if (tDocumentList.size() == 0) {
            return;
        }
        @SuppressWarnings("unchecked")
        Class<T> documentClass = (Class<T>) tDocumentList.get(0).getClass();
        getCollection(databaseType, collectionName, documentClass).insertMany(tDocumentList);
    }

    /**
     * 删除指定集合内满足条件的第一个文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         匹配条件，匹配到多个时，也只会删除第一个元素。
     */
    public DeleteResult deleteOne(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter) {
        return getCollection(databaseType, collectionName)
                .deleteOne(filter);
    }

    /**
     * 删除指定集合内满足条件的所有文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         匹配条件
     */
    public DeleteResult deleteMany(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter) {
        return getCollection(databaseType, collectionName)
                .deleteMany(filter);
    }

    /**
     * 更新数据库中满足条件的第一个文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param updater        更新器
     */
    public UpdateResult updateOne(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, Bson updater) {
        return getCollection(databaseType, collectionName)
                .updateOne(filter, updater);
    }

    /**
     * 更新数据库中满足条件的所有文档
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param updater        更新器
     */
    public UpdateResult updateMany(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, Bson updater) {
        return getCollection(databaseType, collectionName)
                .updateMany(filter, updater);
    }

    /**
     * 替换满足条件的第一个元素，如果没有匹配的元素，则什么不做
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param tDocument      集合内元素类型
     * @param <T>            集合内元素类型
     */
    public <T> UpdateResult replace(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, T tDocument) {
        @SuppressWarnings("unchecked")
        Class<T> documentClass = (Class<T>) tDocument.getClass();
        return getCollection(databaseType, collectionName, documentClass)
                .replaceOne(filter, tDocument);
    }

    /**
     * 替换满足条件的第一个元素，如果没有匹配的元素则插入该元素
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param tDocument      集合内元素类型
     * @param <T>            集合内元素类型
     */
    public <T> UpdateResult replaceOrInsert(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, T tDocument) {
        @SuppressWarnings("unchecked")
        Class<T> documentClass = (Class<T>) tDocument.getClass();
        return getCollection(databaseType, collectionName, documentClass)
                .replaceOne(filter, tDocument, new ReplaceOptions().upsert(true));
    }

    /**
     * 查询是否存在满足条件的结果
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @return true, if exist
     */
    public boolean exist(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter) {
        return null != getCollection(databaseType, collectionName)
                .find(filter)
                .batchSize(BATCH_SIZE)
                .first();
    }

    /**
     * 查找指定集合中满足条件的第一个元素。
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  集合内元素的类型
     * @param filter         过滤条件，常用可参考{@link Filters}
     * @param <T>            集合内元素的类型
     * @return the first item or null.
     */
    public <T> T findFirst(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter) {
        return getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .first();
    }

    /**
     * 查找满足条件的一个文档，并返回需要的字段(可减少传输数据量)
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param resultFields   取回哪些字段，如果不显示传入"_id"字段，则不返回 "_id"字段
     * @return 满足条件的文档的指定字段
     */
    public Document findFirst(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, List<String> resultFields) {
        return getCollection(databaseType, collectionName)
                .find(filter)
                .projection(validateResultFields(resultFields))
                .first();
    }

    /**
     * 验证要返回的字段
     *
     * @param resultFields 期望返回的字段
     * @return 如果不期望返回"_id"字段，则不返回"_id"字段，因为mongodb查询默认会返回"_id字段"
     */
    private Bson validateResultFields(List<String> resultFields) {
        return resultFields.contains("_id") ? include(resultFields) : fields(include(resultFields), excludeId());
    }

    /**
     * 查找指定集合中满足条件的所有元素
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  集合内元素的类型
     * @param filter         过滤条件
     * @param <T>            集合内元素的类型
     * @return list size may equals 0
     */
    public <T> List<T> findMany(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter) {
        // 使用LinkedList是因为插入次数远大于遍历次数，而且预分配过多内存不好
        List<T> result = new LinkedList<>();
        return getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .batchSize(BATCH_SIZE)
                // into 是ok的，会自动释放资源
                .into(result);
    }

    /**
     * 查找指定集合中满足条件的指定个数元素
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  集合内元素的类型
     * @param filter         过滤条件
     * @param limit          结果个数
     * @param <T>            集合内元素的类型
     * @return list size may equals 0
     */
    public <T> List<T> findMany(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter,
                                int limit) {
        List<T> result = new LinkedList<>();
        return getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .batchSize(BATCH_SIZE)
                .limit(limit)
                // into 是ok的，会自动释放资源
                .into(result);
    }

    /**
     * 查找满足条件的所有文档，并返回需要的字段(可减少传输数据量)
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param resultFields   取回哪些字段，如果不显示传入"_id"字段，则不返回 "_id"字段
     * @return 满足条件的文档的指定字段
     */
    public List<Document> findMany(MongoDBType databaseType, MongoCollectionName collectionName, List<String> resultFields, Bson filter) {
        List<Document> result = new LinkedList<>();
        return getCollection(databaseType, collectionName)
                .find(filter)
                .projection(validateResultFields(resultFields))
                .batchSize(BATCH_SIZE)
                // into 是ok的，会自动释放资源
                .into(result);
    }

    /**
     * 对查找的结果进行分页，获取指定页的结果
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  集合内文档类型
     * @param filter         过滤条件
     * @param page           页数(1开始)
     * @param pageSize       每页大小
     * @param <T>            集合内文档类型
     * @return list size may equals 0
     */
    public <T> List<T> findManyByPage(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter,
                                      int page, int pageSize) {
        List<T> result = new LinkedList<>();
        int skipNum = (page - 1) * pageSize;
        return getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .batchSize(BATCH_SIZE)
                .skip(skipNum)
                .limit(pageSize)
                // into 是ok的，会自动释放资源
                .into(result);
    }

    /**
     * 对查找到的每一个文档执行某个操作。
     * <p>
     * 注意：如果使用外部迭代，一定注意关闭资源。
     * <p>
     * The Mongo Cursor interface implementing the iterator protocol.
     * <p>
     * An application should ensure that a cursor is closed in all circumstances, e.g. using a try-with-resources statement:
     *
     * <blockquote><pre>
     * try (MongoCursor&lt;Document&gt; cursor = collection.find().iterator()) {
     *     while (cursor.hasNext()) {
     *         System.out.println(cursor.next());
     *     }
     * }
     * </pre></blockquote>
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  文档类型
     * @param filter         过滤条件
     * @param consumer       执行的操作
     * @param <T>            集合内文档类型
     */
    public <T> void forEach(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter,
                            Consumer<T> consumer) {
        FindIterable<T> findIterable = getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .batchSize(BATCH_SIZE);
        // 创建迭代器之后，必须释放资源，
        try (MongoCursor<T> itr = findIterable.iterator()) {
            while (itr.hasNext()) {
                consumer.accept(itr.next());
            }
        }
    }

    /**
     * 对集合内满足条件的文档执行什么操作
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filter         过滤条件
     * @param consumer       执行的操作
     */
    public void forEach(MongoDBType databaseType, MongoCollectionName collectionName, Bson filter, Consumer<Document> consumer) {
        forEach(databaseType, collectionName, Document.class, filter, consumer);
    }

    /**
     * 查找满足条件的第一个。
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param documentClass  集合内文档的类型
     * @param filter         过滤条件
     * @param compareKeys    以哪些属性作为比较标准，{@link IndexDocument}对应创建索引和排序都很有帮助。
     * @param <T>            集合内元素的类型
     * @return 满足匹配条件和排序规则的第一个
     */
    public <T> T maxOrMin(MongoDBType databaseType, MongoCollectionName collectionName, Class<T> documentClass, Bson filter, IndexDocument compareKeys) {
        return getCollection(databaseType, collectionName, documentClass)
                .find(filter)
                .batchSize(BATCH_SIZE)
                .sort(compareKeys.getDocument())
                .first();
    }
    // endregion

    // region 索引操作

    /**
     * 为指定集合中的文档的某个字段创建索引
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filed          字段名
     * @param ascending      是否升序
     */
    public void createIndex(MongoDBType databaseType, MongoCollectionName collectionName, String filed, boolean ascending) {
        createIndex(databaseType, collectionName, new IndexDocument(filed, ascending));
    }

    /**
     * 为指定集合创建复合索引
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param indexDocument  索引文档，注意： 数字1表示升序，数字-1表示降序。不能用字符串"1"和"-1"
     */
    public void createIndex(MongoDBType databaseType, MongoCollectionName collectionName, IndexDocument indexDocument) {
        getCollection(databaseType, collectionName).createIndex(indexDocument.getDocument());
    }

    /**
     * 为指定集合创建唯一索引
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param filed          索引字段
     * @param ascending      是否升序
     */
    public void createUniqueIndex(MongoDBType databaseType, MongoCollectionName collectionName, String filed, boolean ascending) {
        createUniqueIndex(databaseType, collectionName, new IndexDocument(filed, ascending));
    }

    /**
     * 创建唯一的复合索引
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param indexDocument  复杂索引内容
     */
    public void createUniqueIndex(MongoDBType databaseType, MongoCollectionName collectionName, IndexDocument indexDocument) {
        getCollection(databaseType, collectionName).createIndex(indexDocument.getDocument(), new IndexOptions().unique(true));
    }

    /**
     * 删除某个简单索引，若索引不存在，则什么也操作(它是一个原子操作，非先检查后执行)
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param field          索引的字段
     */
    public void dropIndex(MongoDBType databaseType, MongoCollectionName collectionName, String field, boolean ascending) {
        dropIndex(databaseType, collectionName, new IndexDocument(field, ascending));
    }

    /**
     * 删除某个复合索引，若索引不存在，则什么也不操作(它是一个原子操作，非先检查后执行)
     *
     * @param databaseType   数据库名字
     * @param collectionName 集合名字
     * @param indexDocument  复杂索引(怎么创建的就怎么删)
     */
    public void dropIndex(MongoDBType databaseType, MongoCollectionName collectionName, IndexDocument indexDocument) {
        try {
            getCollection(databaseType, collectionName).dropIndex(indexDocument.getDocument());
        } catch (MongoException e) {
            if (e.getCode() != ERROR_CODE_INDEX_NOT_EXIST) {
                throw e;
            }
            // ignore, may index not exist
        }
    }
    // endregion

    /**
     * 如果你查询返回的某个字段是一个文档(对象)，且存在对应的codec，则可以进行解码
     *
     * @param document      待解码的文档
     * @param documentClass 文档的类型
     * @param <T>           文档的类型
     * @return 解码的结构
     */
    public <T> T decodeDocument(Document document, Class<T> documentClass) {
        CodecRegistry codecRegistry = mongoClient.getMongoClientOptions().getCodecRegistry();
        BsonDocument bsonDocument = document.toBsonDocument(null, codecRegistry);
        try (BsonDocumentReader bsonDocumentReader = new BsonDocumentReader(bsonDocument)) {
            Codec<T> tCodec = codecRegistry.get(documentClass);
            return tCodec.decode(bsonDocumentReader, DecoderContext.builder().build());
        }
    }
}
