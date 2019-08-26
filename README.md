### fastjgame
fastjgame 为 fast java game framework的缩写，如名字一样，该项目的目标是一个高性能，高稳定性的游戏架构。  
1. 它将是一个分布式多线程架构，它有着良好的多线程模型。  
2. 高性能从网络层开始： 支持异步消息，异步rpc调用，同步rpc调用;  
3. IO框架为Netty,HttpClient为OkHttp3; 支持断线重连，支持websocket和tcp同时接入。 
4. 支持google protoBuf，json以及自定义消息序列化方式。 支持自定义消息映射;  
5. 分布式协调框架：zookeeper 作为配置中心，节点发现，以及不时的分布式锁需要  
6. NoSQL数据库： mongodb 习惯了mongodb回不去MySQL
7. 代码自动生成: RpcService、Subscribe相应代码自动生成。代码生成一时爽，一直生成一直爽。

### 全新版本 2019-08-05
2019年8月5日 迎来了fastjgame的全新版本，线程模型大改，底层通信重写，网络层与逻辑层独立。
1. 更强大的网络层，异步rpc调用，同步rpc调用使用起来很简单。旧版本的session管理器真的很难用。
2. 线程模型改动，由最开始的单线程模型变为多线程模型，而且是变成模型很友好的多线程模型(并发包主要参考了Netty的思路)。
3. 支持一个进程启动所有类型服务器！在game-start模块中，**StartUp**类是一个测试用例。可以启动所有服务器
   （你需要安装zookeeper，在doc文件夹下有我zookeeper导出的配置）。

### Rpc优化 2019-08-21
1. Rpc调用更加标准化。
2. 编解码更快，功能支持更强，请查看**SerializableClass**和**SerializableField**注解。
3. RpcService代码自动生成，你必须将game-auto打为jar包才可以使用代码生成工具(注解处理器必须是jar包形式)，请确保所有模块都启用了注解处理器。

### 注解处理器优化 2019-08-26
注解处理器不再依赖utils和net模块，而是utils依赖auto模块，这样utils和net模块也可以使用自己定义的注解了。

### 如何使用注解处理器？
1. 将game-auto添加到project-structure
2. install game-auto 到本地仓库
3. 将game-auto从项目project-structure中移除，注解处理器必须以jar包形式工作。
4. 在game-parent下clean，再compile，可消除缺少类文件的报错。

### 编译出现找不到符号问题怎么解决？
1. 确保注解处理器已install到本地仓库。
2. clean
3. compile

### 高性能从网络层开始  
1. IO框架为Netty,HttpClient为OkHttp3;   
2. 支持断线重连，支持websocket和tcp同时接入。  
3. 支持自定义协议解析-实现**ProtocolCodec**。
4. 有基于protoBuf的协议传输，也有基于json的协议传输，都是ProtocolCodec的实现类。
5. 实现了一套完整的rpc，支持异步消息，异步rpc调用，同步rpc调用。
6. 网络层多线程模型(8/5日改动)

#### 游戏的玩法架构已确定：  
1. 游戏世界划分为多个区域，每个进程（频道）承载一个或多个区域，支持跨运营平台，跨服玩法。  
2. 在game-core包的doc文件夹下可以看见服务器架构、场景区域划分等图。  
3. 待完善的是负载均衡和宕机恢复。
4. 此外，由于又开始上班了，而且没有确切的需求，导致了很多东西无法继续进行，所以更新进度可能较慢。

#### 游戏服务器的节点发现以完成：
* 基于zookeeper实现，同时zookeeper作为配置中心，以及分布式锁.  
  zookeeper的配置在**game-start**的doc文件夹下可以找到。
  如果你想使用中文配置zkui，请使用我修正后的[zkui](https://github.com/hl845740757/zkui)，内部有可运行jar包，你也可以自己编译一遍。

#### 数据库引入-MongoDB  
* MongoDB是NOSQL数据库，个人感觉其对程序员非常友好，而且数据扩展容易，性能也好。

#### 视野管理已加入(9宫格)
* 游戏视野管理为9宫格。

#### 寻路算法已加入(Jump Point Search)
1. JPS寻路算法是对A*算法的一个优化，不算很复杂，适合多数的MMO游戏。
2. 图形化测试用例：JPSTest.java  

#### Scene服多线程化
1. 关于Scene多线程化的几种设计思路，可参考SceneServer模块下的doc文件夹。
2. 如果Scene多线程化，游戏整个架构都会变成多线程架构，到时候改动量会比较大。  

#### 说下自己为什么开源
1. 开源的高质量的源码太少，大多质量都太差，代码混乱，编程模型复杂。
2. 我个人非常看重游戏的创意，而这些创意很多时候在中小型游戏公司产生，而往往缺乏技术支撑的却是他们。

(Markdown语法不是很熟悉，排版什么的后期有空再优化~)
