package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/14 20:23
 * github - https://github.com/hl845740757
 */
public class ZKLockTest {

    public static void main(String[] args) throws Exception {
        CuratorMgr curatorMgr = CuratorTest.newCuratorMgr();

        curatorMgr.actionWhitLock("/mutex/guid", () -> {
            ConcurrentUtils.sleepQuietly(60 * 1000);
        });
    }
}
