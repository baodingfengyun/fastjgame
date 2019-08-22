##### 一套良好的Rpc接口应该是啥样的呢？

异步Rpc调用：
1. 通过Proxy生成一个MethodCall<V>
2. 获取session相关信息，接收一个MethodCall<V>的参数，返回一个Future<V>
3. 通过Future添加回调， ifFailed,ifSuccess, any
4. 调用发送命令call()

同步Rpc调用：
1. 通过Proxy生成一个MethodCall<V>
2. 获取session相关信息，接收一个MethodCall<V>的参数，返回一个RpcResponse。