package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.misc.CloseableHandle;
import com.wjybxx.fastjgame.mgr.CuratorClientMgr;
import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.mgr.GameConfigMgr;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.mgr.GameEventLoopMgr;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.LockSupport;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/14 19:02
 * github - https://github.com/hl845740757
 */
public class CuratorTest {

    public static CuratorMgr newCuratorMgr() throws Exception {
        GameConfigMgr gameConfigMgr = new GameConfigMgr();
        CuratorClientMgr curatorClientMgr = new CuratorClientMgr(gameConfigMgr);
        GameEventLoopMgr gameEventLoopMgr = new GameEventLoopMgr();
        return new CuratorMgr(curatorClientMgr, gameEventLoopMgr);
    }

    public static void main(String[] args) throws Exception {
        CuratorMgr curatorMgr = newCuratorMgr();
        CloseableHandle closeHandle = curatorMgr.watchChildren("/", CuratorTest::onEvent);

        LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * TimeUtils.MIN);

        closeHandle.close();
    }

    private static void onEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        ChildData childData = event.getData();
        if (childData == null) {
            System.out.println(String.format("thread=%s, eventType=%s",
                    Thread.currentThread().getName(),
                    event.getType()));
        } else {
            System.out.println(String.format("thread=%s, eventType=%s, path=%s, data=%s",
                    Thread.currentThread().getName(),
                    event.getType(),
                    childData.getPath(),
                    new String(childData.getData(), StandardCharsets.UTF_8)));
        }
    }

    private static void printChild(ChildData childData) {
        System.out.println(String.format("childData: path=%s, data=%s", childData.getPath(),
                new String(childData.getData(), StandardCharsets.UTF_8)));
    }

}
