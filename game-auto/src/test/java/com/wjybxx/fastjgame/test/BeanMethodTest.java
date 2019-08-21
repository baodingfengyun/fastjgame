/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.test;

/**
 * 测试正常情况下的getter setter
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/18
 * github - https://github.com/hl845740757
 */
public class BeanMethodTest {

	private boolean isYou;

	public boolean isYou() {
		return isYou;
	}

	public void setYou(boolean you) {
		isYou = you;
	}

	private boolean hasName;

	public boolean isHasName() {
		return hasName;
	}

	public void setHasName(boolean hasName) {
		this.hasName = hasName;
	}


	private int set;

	public int getSet() {
		return set;
	}

	public void setSet(int set) {
		this.set = set;
	}

	private int setName;

	public int getSetName() {
		return setName;
	}

	public void setSetName(int setName) {
		this.setName = setName;
	}
}
