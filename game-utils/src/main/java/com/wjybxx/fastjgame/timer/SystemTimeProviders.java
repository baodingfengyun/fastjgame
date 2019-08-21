/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.timer;


/**
 * 常用系统时间提供器
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/16
 */
public final class SystemTimeProviders {

	private SystemTimeProviders() {

	}

	/**
	 * 获取实时时间提供器
	 * @return timeProvider
	 */
	public static SystemTimeProvider getRealtimeProvider() {
		return RealTimeProvider.INSTANCE;
	}

	/**
	 * 实时系统时间提供者
	 */
	private static class RealTimeProvider implements SystemTimeProvider{

		public static final RealTimeProvider INSTANCE = new RealTimeProvider();

		private RealTimeProvider() {

		}

		@Override
		public long getSystemMillTime() {
			return System.currentTimeMillis();
		}

		@Override
		public int getSystemSecTime() {
			return (int) (System.currentTimeMillis()/1000);
		}
	}

}
