### fastjgame
fastjgame 为 fast java game framework的缩写，如名字一样，该项目的目标是一个高性能，高稳定性的游戏服务器架构。  

### 暂不建议fork，欢迎star和watch
由于个人经验及能力原因，代码改动频繁，甚至很多设计都会推翻，因此暂时不建议fork，但相信我，一定对得起你的star！

#### 序列化支持全新升级 2020年2月18日
新增特性:
1. 支持继承 - 序列化时，会将超类中带有注解的字段一同序列化，即使超类类信息上没有注解。
2. 支持多态解析 - 举个例子：  
 **child1**以**parent**的方式序列化，解析时创建**child2**的实例，并将**parent**的数据赋予**child2**,
 其实底层对于map和collection的处理是一样的，只不过是通过注解来获取合适的实现类。  
 用户需要实现serializer接口，写少量代码即可，更过请查看**EntitySerializer**接口。
3. 支持db注解序列化生成。
4. 所有带注解的类，都生成对应的serializer，能调用普通方法的绝不反射调用，进一步提升编解码效率，现在和protoBuffer的序列化性能几乎相近。
可以运行**SerializePerformanceTest** 和 **ProtoBufSerializePerformanceTest**体验一下，JSON和自定义协议和ProtoBuf自身的性能差异，虽然测试不太规范，但还是能说明问题的。
[生成代码示例图](https://github.com/hl845740757/fastjgame/blob/master/game-net/src/doc/generatedcode.png)
PS: 需要重新安装注解处理器。

***
### 项目里我要写什么？
能复用的底层技术，目前我总结包括：
1. 线程模型。
2. 网络层(rpc、http)
3. 数据存储
4. 游戏日志搜集
5. 服务器发现
6. guid(唯一id)生成方案

### 编译要求 JDK11
[jdk11百度云下载链接](https://pan.baidu.com/s/10IWbDpIeVDk5iPjci0gDUw)  提取码: d13j

***
### 如何使用注解处理器(编译出现找不到符号问题怎么解决)？
+ 方式1 - 自己打包安装：  
> 1. 将game-apt作为单独项目打开
> 2. install game-apt 到本地仓库
> 3. 在game-parent下clean，再compile，可消除缺少类文件的报错。

+ 方式2 - 安装已有jar包
> 1. 将game-libs下 game-apt.jar 安装安装到本地仓库。
> 2. 在game-parent下clean，再compile，可消除缺少类文件的报错。

***
### [历史重要更新](https://github.com/hl845740757/fastjgame/blob/master/%E5%8E%86%E5%8F%B2%E9%87%8D%E8%A6%81%E6%9B%B4%E6%96%B0.md)

(Markdown语法不是很熟悉，排版什么的后期有空再优化~)
