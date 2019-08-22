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

package com.wjybxx.fastjgame.rpcservice;

import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;

import java.util.List;

/**
 * @author houlei
 * @version 1.0
 * date - 2019/8/22
 */
@RpcService(serviceId = ServiceTable.SCENE_REGION_MRG)
public interface ISceneRegionMrg {

	/**
	 * 建收到中心服启动互斥区域的命令 (立连接后)
	 * @param activeMutexRegionsList 需要启动的互斥区域
	 * @return 启动成功
	 */
	@RpcMethod(methodId = 1)
	boolean startMutexRegion(List<Integer> activeMutexRegionsList);

	/**
	 * 收到game的激活区域命名(宕机恢复，挂载其他场景进程宕掉的区域)
	 * @param activeRegionsList 需要启动的区域(可能包含互斥和非互斥区域)
	 * @return 启动成功
	 */
	@RpcMethod(methodId = 2)
	boolean activeRegions(List<Integer> activeRegionsList);

}
