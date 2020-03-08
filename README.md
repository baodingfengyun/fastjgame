### fastjgame
fastjgame 为 fast java game framework的缩写，如名字一样，该项目的目标是一个高性能，高稳定性的游戏服务器架构。  

### 暂不建议fork，欢迎star和watch
由于个人经验及能力原因，代码改动频繁，甚至很多设计都会推翻，因此暂时不建议fork，但相信我，一定对得起你的star！

### 并发组件和Rpc组件优化 2020/3/8
并发组件重新优化，**ListenableFuture**不再直接继承JDK的future，使得各种轻量级的future实现变成可能。  
基于新的ListenableFuture重新实写了Rpc和Redis的部分组件，删除了旧的**MethodHandle**组件。  
新实现的Rpc组件，更容易理解，更易扩展，更贴近应用本身。  
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
