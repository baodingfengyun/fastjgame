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

package com.wjybxx.fastjgame.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.wjybxx.fastjgame.mrg.*;
import com.wjybxx.fastjgame.world.GameEventLoop;
import com.wjybxx.fastjgame.world.GameEventLoopMrg;

/**
 * {@link GameEventLoop}级别的单例
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/6
 * github - https://github.com/hl845740757
 */
public class GameEventLoopModule extends AbstractModule {

	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(GameEventLoopMrg.class).in(Singleton.class);
		bind(CuratorMrg.class).in(Singleton.class);
		bind(GuidMrg.class).to(ZkGuidMrg.class).in(Singleton.class);

		bind(TemplateMrg.class).in(Singleton.class);
	}
}
