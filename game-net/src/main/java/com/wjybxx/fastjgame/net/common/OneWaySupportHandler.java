/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net.common;

import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.task.OneWayMessageCommitTask;
import com.wjybxx.fastjgame.net.task.OneWayMessageWriteTask;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

/**
 * 单向消息支持
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public class OneWaySupportHandler extends SessionDuplexHandlerAdapter {

    public OneWaySupportHandler() {
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        if (msg instanceof OneWayMessage) {
            // 读取到一个单向消息
            OneWayMessage oneWayMessage = (OneWayMessage) msg;
            ConcurrentUtils.tryCommit(ctx.localEventLoop(),
                    new OneWayMessageCommitTask(ctx.session(), oneWayMessage.getMessage()));
        } else {
            ctx.fireRead(msg);
        }
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof OneWayMessageWriteTask) {
            // 单个消息
            OneWayMessageWriteTask writeTask = (OneWayMessageWriteTask) msg;
            ctx.fireWrite(new OneWayMessage(writeTask.getMessage()));
        } else {
            ctx.fireWrite(msg);
        }
    }
}
