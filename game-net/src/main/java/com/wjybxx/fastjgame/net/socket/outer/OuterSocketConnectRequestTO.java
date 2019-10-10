package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.net.socket.SocketConnectRequest;
import com.wjybxx.fastjgame.net.socket.SocketConnectRequestTO;

/**
 * 对外连接请求传输对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/2
 * github - https://github.com/hl845740757
 */
public class OuterSocketConnectRequestTO implements SocketConnectRequestTO {

    /**
     * 我期望的下一个消息号
     */
    private final long ack;

    private final SocketConnectRequest connectRequest;

    OuterSocketConnectRequestTO(long ack, SocketConnectRequest connectRequest) {
        this.ack = ack;
        this.connectRequest = connectRequest;
    }

    @Override
    public long getAck() {
        return ack;
    }

    @Override
    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }
}
