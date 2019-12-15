### fastjgame
fastjgame 为 fast java game framework的缩写，如名字一样，该项目的目标是一个高性能，高稳定性的游戏服务器架构。  
1. **分布式多进程多线程**架构，可退化为**分布式多进程单线程**架构。
2. 优秀的线程模型，兼具**性能和简单性**。--结合了**Netty**与**Disruptor**两者的优势，诞生了**DisruptorEventLoop**。  
3. 高性能的网络层: 基于**protoBuf**的自定义二进制协议，**体积小，编解码速度快**，再辅以代码生成 -- **简单极速的rpc调用**。
4. 简单易用的**双向rpc**，支持**跨线程**和**跨进程**调用，支持**单向通知**，**异步回调**，**同步调用**。
5. **基于注解处理器的代码生成**: RpcService、Subscribe等相应代码自动生成。代码生成一时爽，一直生成一直爽。
6. **异步redis支持**，**支持回调**，一个**RedisEventLoop**hold住全场。
7. **kafka**支持，可用于游戏打点日志，见**LogProducerEventLoop**。
8. **java-zset**，参考redis zset实现，进行了java本地化改造，支持**泛型score**，可用于游戏类**排行榜**和**拍卖行**。
9. 较高的代码质量，胜于你看见的大多数同类型项目，无论是在**线程安全**方面，还是在**代码设计**方面。

### 暂不建议fork
由于项目处于前期，代码改动频繁，甚至很多设计都会推翻，因此暂时不建议fork。

***
### [历史重要更新](https://github.com/hl845740757/fastjgame/blob/master/%E5%8E%86%E5%8F%B2%E9%87%8D%E8%A6%81%E6%9B%B4%E6%96%B0.md)

### [谈多线程框架取舍](https://github.com/hl845740757/fastjgame/blob/master/%e5%a4%9a%e7%ba%bf%e7%a8%8b%e6%a1%86%e6%9e%b6%e5%8f%96%e8%88%8d.md)

***
#### 如何使用注解处理器(编译出现找不到符号问题怎么解决)？
方式1 - 自己打包安装：  
> 1. 将game-auto添加到project-structure
> 2. install game-auto 到本地仓库
> 3. 将game-auto从项目project-structure中移除，注解处理器必须以jar包形式工作。
> 4. 在game-parent下clean，再compile，可消除缺少类文件的报错。

方式2 - 安装已有jar包
> 1. 将game-libs下 game-auto.jar 安装安装到本地仓库。
> 2. 在game-parent下clean，再compile，可消除缺少类文件的报错。

***
### 网络层支持
1. IO框架为Netty,HttpClient为OkHttp3;   
2. 支持断连重连，支持websocket和tcp同时接入。  
3. 采用protoBuf实现自定义二进制协议，**体积小，编解码速度快**，再辅以代码生成 -- **简单极速的rpc调用**。
4. 全双工session，支持**单向通知，异步rpc调用，同步rpc调用。**
5. JVM内线程通信与跨进程通信具有几乎完全相同的API。你可以向另一个**进程**发起RPC请求，也可以向另一个**线程**发起RPC请求。

***
#### 服务器的节点发现
* 基于zookeeper实现，同时zookeeper作为配置中心，以及分布式锁.  
  zookeeper的配置在**game-start**的doc文件夹下可以找到。
  如果你想在zkui中使用中文进行配置，请使用我修正后的[zkui](https://github.com/hl845740757/zkui)，内部有可运行jar包，你也可以自己编译一遍。

#### 更新问题 
+ 由于要上班的，而且没有确切的需求，导致了很多东西无法继续进行，所以架构可能会不停的优化更新，但是业务逻辑可能进展很慢。

(Markdown语法不是很熟悉，排版什么的后期有空再优化~)

#### 吐槽
你在网上可能能看见一些多线程的游戏服务器框架，我也看过部分，基本上看几个类就发现有bug，代码质量也是烂的一塌糊涂。  