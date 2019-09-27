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

package com.wjybxx.fastjgame.net.pipeline;

import com.wjybxx.fastjgame.manager.NetManagerWrapper;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/25
 * github - https://github.com/hl845740757
 */
class DefaultSessionHandlerContext extends AbstractSessionHandlerContext {

    private final SessionHandler handler;

    DefaultSessionHandlerContext(DefaultSessionPipeline pipeline, NetManagerWrapper netManagerWrapper, SessionHandler handler) {
        super(pipeline, netManagerWrapper);
        this.handler = handler;
    }

    @Override
    public SessionHandler handler() {
        return handler;
    }

}
