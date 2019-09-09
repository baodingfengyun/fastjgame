package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.net.DefaultRpcPromise;
import com.wjybxx.fastjgame.net.RpcPromise;
import com.wjybxx.fastjgame.utils.TimeUtils;

/**
 * RpcPromise超时测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/14
 */
public class RpcPromiseTest {

    public static void main(String[] args) throws InterruptedException {
        NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupImp(1, new DefaultThreadFactory("Net"), RejectedExecutionHandlers.log());
        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(1, new DefaultThreadFactory("DEF"), RejectedExecutionHandlers.log());

        RpcPromise rpcPromise = new DefaultRpcPromise(netEventLoopGroup.next(), defaultEventLoopGroup.next(), 10 * TimeUtils.SEC);
        System.out.println(rpcPromise.get());
        System.out.println(rpcPromise.get());
    }
}
