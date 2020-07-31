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

package com.wjybxx.fastjgame.util.concurrent.unbounded;

/**
 * 当等待任务的过程中，发现{@link TemplateEventLoop}已经开始关闭时，则抛出该异常。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/24
 */
public class ShuttingDownException extends Exception {

    public static final ShuttingDownException INSTANCE = new ShuttingDownException();

    private ShuttingDownException() {
        super(null, null, false, false);
    }

}
