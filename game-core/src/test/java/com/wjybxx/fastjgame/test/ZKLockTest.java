package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/14 20:23
 * github - https://github.com/hl845740757
 */
public class ZKLockTest {

    public static void main(String[] args) throws Exception {
        CuratorMgr curatorMgr = CuratorTest.newCuratorMrg();

        curatorMgr.lock("/mutex/guid");

        // 始终占用锁
        ConcurrentUtils.awaitRemoteWithRetry((timeout, timeUnit) -> false,
                500, TimeUnit.MILLISECONDS);

    }
}
