### fastjgame
fastjgame 为 fast java game framework的缩写，如名字一样，该项目的目标是一个高性能，高稳定性的游戏服务器架构。  

### 暂不建议fork，欢迎star和watch
由于个人经验及能力原因，代码改动频繁，甚至很多设计都会推翻，因此暂时不建议fork，但相信我，一定对得起你的star！

***
### 项目里我要写什么？
能复用的底层技术，目前我总结包括：
1. 线程模型。
2. 网络层(rpc、http)
3. 数据存储
4. 游戏日志搜集
5. 服务器发现
6. guid(唯一id)生成方案

***
### 线程模型
线程模型从Netty借鉴了许多，但也有稍许区别：
1. 拥抱JDK8的CompletableFuture，构建异步管道可以极大的提高代码的表达力，同时降低编程难度。
2. 删除EventExecutor概念，只保留EventLoop概念，EventLoop就是单线程的。如果不需要单线程带来的一些保证，那么使用Executor概念即可。
3. EventLoop是**多生产者单消费者**模型。
4. 提供了基于Disruptor的高性能事件循环**DisruptorEventLoop**。

### RPC
这里的rpc设计并未追求所谓的标准，而更追求实用性，主要特点如下：
1. 为客户端生成的proxy类的方法签名与原方法签名可能并不一样，因此proxy类并不实现接口，也不要求被代理的类必须是接口。
2. 所有的proxy类仅仅是辅助类，方法都是静态的，仅仅用于封装请求参数，并不直接执行调用，用户需要显式的通过RpcClient发送请求。
3. 通过short类型的serviceId和methodId定位被调用方法。

设计目的是什么？
1. 允许参数不一致，可以更好的支持服务器异步编程。如果方法的返回类型为Future，表示服务器**可能**无法立即返回结果，代理方法会自动监听Future的结果。而该参数并不会出现在proxy类中。
2. 允许参数不一致，可以延迟某些参数的序列化或提前某些参数的反序列化，可以消除请求方和执行方的序列化反序列化工作。该实现依赖**LazySerializable**和**PreDeserializable**注解。
3. proxy仅仅是辅助类，通过rpcClient发送请求。用户可以选择是**单项通知**、**异步调用**还是**同步调用**。这非常像ExecutorService中的**execute**和**submit**，
用户可以自由选择是否监听执行结果。放弃**透明性**，其实是提醒用户rpc和普通方法调用存在明显的性能差异，鼓励用户少使用同步调用。
4. 通过short类型的serviceId和methodId定位被调用方法，可以大大减少数据传输量，而且定位方法更快。  

总的来说: Rpc设计更像多线程编程，以异步为主。

### 日志搜集
日志搜集组件是**插件式**的，类似SLF和Log4J。应用基于log-api发布和消费日志，在启动时指定具体的实现和对应的适配组件。
默认提供了基于Kafka的日志发布和消费实现。

### GUID生成方案
Guid生成器中定义了命名空间的概念，只要求同一个命名空间下的id不重复。不同的业务可以使用不同的guid生成器，可以减少guid分配的分配压力。  
该组件也是插件式的，接口包为guid-api包，并提供了基于redis和zookeeper的实现。

***
### 编译要求 JDK11
[jdk11百度云下载链接](https://pan.baidu.com/s/10IWbDpIeVDk5iPjci0gDUw)  提取码: d13j

### 如何使用注解处理器(编译出现找不到符号问题怎么解决)？
+ 方式1 - 自己打包安装：  
> 1. 将game-apt作为单独项目打开
> 2. install game-apt 到本地仓库
> 3. 在game-parent下clean，再compile，可消除缺少类文件的报错。

+ 方式2 - 安装已有jar包
> 1. 将game-libs下 game-apt.jar 安装安装到本地仓库。
> 2. 在game-parent下clean，再compile，可消除缺少类文件的报错。

***
### 并发组件优化 JCTools的Mpsc队列代替ConcurrentLinkedQueue
Disruptor的**RingBuffer**和Jdk的**ConcurrentLinkedQueue**都是多消费者模型，而**EventLoop是多生产者单消费者模型**，使用Mpsc队列可以极大的提高性能。  
Mpsc队列最早是在Netty中看见的，但是苦于是internal的，后来才发现其实是JCTools的，于是引入，作为无界EventLoop的模板，性能确实极好。  
PS: 也很适用Actor模型。

### [历史重要更新](https://github.com/hl845740757/fastjgame/blob/master/%E5%8E%86%E5%8F%B2%E9%87%8D%E8%A6%81%E6%9B%B4%E6%96%B0.md)

(Markdown语法不是很熟悉，排版什么的后期有空再优化~)