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

package com.wjybxx.fastjgame.time;


import com.wjybxx.fastjgame.utils.TimeUtils;

/**
 * 绝对时间偏移量。
 *
 * 配置格式{@link TimeUtils#DEFAULT_PATTERN}
 * yyyy-MM-dd HH:mm:ss
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/7 22:50
 * github - https://github.com/hl845740757
 */
public class AbsoluteTimeOffset implements TimeOffset{

	private long offset;

	public AbsoluteTimeOffset(long offset) {
		this.offset = offset;
	}

	@Override
	public long toOffset() {
		return offset;
	}

	@Override
	public String toString() {
		return "AbsoluteTimeOffset{" +
				"offset=" + offset +
				'}';
	}

	/**
	 * 从配置表中解析
	 * @param confParam 格式 yyyy-MM-dd HH:mm:ss
	 * @return AbsoluteTimeOffset
	 */
	public static AbsoluteTimeOffset parseFromConf(String confParam){
		long curMillTime = TimeUtils.parseMillTime(confParam);
		return new AbsoluteTimeOffset(curMillTime);
	}
}
