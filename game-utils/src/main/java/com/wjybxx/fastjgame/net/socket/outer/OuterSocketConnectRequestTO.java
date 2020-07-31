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

    private final long initSequence;
    private final long ack;
    private final boolean close;
    private final SocketConnectRequest connectRequest;

    OuterSocketConnectRequestTO(long initSequence, long ack, boolean close, SocketConnectRequest connectRequest) {
        this.initSequence = initSequence;
        this.ack = ack;
        this.close = close;
        this.connectRequest = connectRequest;
    }

    @Override
    public long getInitSequence() {
        return initSequence;
    }

    @Override
    public long getAck() {
        return ack;
    }

    @Override
    public boolean isClose() {
        return close;
    }

    @Override
    public SocketConnectRequest getConnectRequest() {
        return connectRequest;
    }
}
