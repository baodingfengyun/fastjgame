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

package com.wjybxx.fastjgame.mrg;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.rpcservice.ISceneRegionMrg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 场景区域管理器
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 11:35
 * github - https://github.com/hl845740757
 */
public class SceneRegionMrg implements ISceneRegionMrg {

    private static final Logger logger= LoggerFactory.getLogger(SceneRegionMrg.class);

    private final SceneWorldInfoMrg sceneWorldInfoMrg;
    /**
     * 已激活的场景区域
     */
    private final Set<SceneRegion> activeRegions = EnumSet.noneOf(SceneRegion.class);

    @Inject
    public SceneRegionMrg(SceneWorldInfoMrg sceneWorldInfoMrg) {
        this.sceneWorldInfoMrg = sceneWorldInfoMrg;
    }

    public void onWorldStart(){
        if (sceneWorldInfoMrg.getSceneWorldType() == SceneWorldType.SINGLE){
            activeSingleSceneNormalRegions();
        }else {
            activeAllCrossSceneRegions();
        }
    }

    public Set<SceneRegion> getActiveRegions() {
        return Collections.unmodifiableSet(activeRegions);
    }

    /**
     * 启动所有跨服场景(目前跨服场景不做互斥)
     */
    private void activeAllCrossSceneRegions(){
        for (SceneRegion sceneRegion:sceneWorldInfoMrg.getConfiguredRegions()){
            activeOneRegion(sceneRegion);
        }
    }

    /**
     * 启动所有本服普通场景
     */
    private void activeSingleSceneNormalRegions(){
        for (SceneRegion sceneRegion:sceneWorldInfoMrg.getConfiguredRegions()){
            // 互斥区域等待centerserver通知再启动
            if (sceneRegion.isMutex()){
                continue;
            }
            activeOneRegion(sceneRegion);
        }
    }

    /**
     * 激活一个region，它就做一件事，创建该region里拥有的城镇。
     * @param sceneRegion 场景区域
     */
    private void activeOneRegion(SceneRegion sceneRegion){
        logger.info("try active region {}.",sceneRegion);
        try{
            // TODO 激活区域
            activeRegions.add(sceneRegion);
            logger.info("active region {} success.",sceneRegion);
        }catch (Exception e){
            // 这里一定不能出现异常
            logger.error("active region {} caught exception.",sceneRegion,e);
        }
    }

    @Override
    public boolean startMutexRegion(List<Integer> activeMutexRegionsList) {
        assert sceneWorldInfoMrg.getSceneWorldType() == SceneWorldType.SINGLE;
        for (int regionId:activeMutexRegionsList){
            SceneRegion sceneRegion = SceneRegion.forNumber(regionId);
            if (activeRegions.contains(sceneRegion)){
                continue;
            }
            activeOneRegion(sceneRegion);
        }
        return true;
    }

    @Override
    public boolean activeRegions(List<Integer> activeRegionsList) {
        assert sceneWorldInfoMrg.getSceneWorldType() == SceneWorldType.SINGLE;
        for (int regionId:activeRegionsList){
            SceneRegion sceneRegion = SceneRegion.forNumber(regionId);
            if (activeRegions.contains(sceneRegion)){
                continue;
            }
            activeOneRegion(sceneRegion);
        }
        return true;
    }

}
