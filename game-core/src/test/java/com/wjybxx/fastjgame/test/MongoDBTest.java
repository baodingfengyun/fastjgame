/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.mgr.CuratorMgr;
import com.wjybxx.fastjgame.mgr.GameConfigMgr;
import com.wjybxx.fastjgame.mgr.MongoDBMgr;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/21 22:56
 * github - https://github.com/hl845740757
 */
public class MongoDBTest {

    public static void main(String[] args) throws Exception {
        GameConfigMgr gameConfigMgr = new GameConfigMgr();
        CuratorMgr curatorMgr = CuratorTest.newCuratorMrg();
        MongoDBMgr mongoDBMgr = new MongoDBMgrImp(gameConfigMgr, curatorMgr);

        System.out.println("在这里打个断点,使用idea debug 界面测试");
    }

    private static class MongoDBMgrImp extends MongoDBMgr {

        public MongoDBMgrImp(GameConfigMgr gameConfigMgr, CuratorMgr curatorMgr) throws Exception {
            super(gameConfigMgr, curatorMgr);
        }

        @Override
        protected void cacheDB() {

        }

        @Override
        protected void createIndex() {

        }
    }
}
