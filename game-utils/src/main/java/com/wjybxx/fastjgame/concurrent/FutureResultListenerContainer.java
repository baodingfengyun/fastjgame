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

package com.wjybxx.fastjgame.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * listener容器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/1/8
 * github - https://github.com/hl845740757
 */
public class FutureResultListenerContainer<FR extends FutureResult<V>, V> implements GenericFutureResultListener<FR> {

    private static final Logger logger = LoggerFactory.getLogger(FutureResultListenerContainer.class);

    private final List<GenericFutureResultListener<FR>> children = new ArrayList<>(2);

    public FutureResultListenerContainer(GenericFutureResultListener<FR> first, GenericFutureResultListener<FR> second) {
        children.add(first);
        children.add(second);
    }

    public void addChild(GenericFutureResultListener<FR> child) {
        children.add(child);
    }

    @Override
    public void onComplete(FR futureResult) {
        for (GenericFutureResultListener<FR> child : children) {
            notifySafely(child, futureResult);
        }
    }

    private void notifySafely(GenericFutureResultListener<FR> child, FR futureResult) {
        try {
            child.onComplete(futureResult);
        } catch (Throwable e) {
            logger.warn("child.onComplete caught exception", e);
        }
    }
}
