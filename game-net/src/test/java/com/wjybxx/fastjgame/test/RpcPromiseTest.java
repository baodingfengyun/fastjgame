package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.concurrent.DefaultEventLoopGroup;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoopImp;
import com.wjybxx.fastjgame.net.common.DefaultRpcPromise;
import com.wjybxx.fastjgame.net.common.RpcPromise;
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
        NetEventLoop netEventLoopGroup = new NetEventLoopImp(new DefaultThreadFactory("Net"), RejectedExecutionHandlers.log());
        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(1, new DefaultThreadFactory("DEF"), RejectedExecutionHandlers.log());

        RpcPromise rpcPromise = new DefaultRpcPromise(netEventLoopGroup.next(), defaultEventLoopGroup.next(), 10 * TimeUtils.SEC);
        System.out.println(rpcPromise.get());
        System.out.println(rpcPromise.get());
    }
}
