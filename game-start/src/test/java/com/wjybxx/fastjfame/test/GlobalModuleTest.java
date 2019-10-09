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

package com.wjybxx.fastjfame.test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.module.WorldGroupModule;
import com.wjybxx.fastjgame.module.SceneModule;
import com.wjybxx.fastjgame.mgr.GameConfigMgr;

/**
 * 测试同一个module在不同的injector中是会保持单例，并不能。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/5
 * github - https://github.com/hl845740757
 */
public class GlobalModuleTest {

	public static void main(String[] args) {
		WorldGroupModule worldGroupModule = new WorldGroupModule();

		Injector parentModule = Guice.createInjector(worldGroupModule);
		Injector injector2 = Guice.createInjector(worldGroupModule);

		Injector childModule1 = parentModule.createChildInjector(new SceneModule());
		Injector childModule2 = parentModule.createChildInjector(new SceneModule());

		// 不等。。
		System.out.println(parentModule.getInstance(GameConfigMgr.class) == injector2.getInstance(GameConfigMgr.class));
		// 相等
		System.out.println(parentModule.getInstance(GameConfigMgr.class) == childModule1.getInstance(GameConfigMgr.class));
		System.out.println(parentModule.getInstance(GameConfigMgr.class) == childModule2.getInstance(GameConfigMgr.class));
	}
}
