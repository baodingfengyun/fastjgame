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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.gameobject.GameObject;
import com.wjybxx.fastjgame.gameobject.Npc;
import com.wjybxx.fastjgame.gameobject.Pet;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.EnumMap;

/**
 * gameObject的容器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/4 21:52
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class GameObjectContainer {

    /**
     * 所有游戏对象
     */
    private final Long2ObjectMap<GameObject> totalGameObjectMap = new Long2ObjectOpenHashMap<>(64);

    /**
     * 玩家集合，用于快速访问;
     * （初始容量不是很好确定，依赖于地图玩法）
     */
    private final Long2ObjectMap<Player> playerMap;
    /**
     * 宠物集合
     */
    private final Long2ObjectMap<Pet> petMap;
    /**
     * npc集合
     */
    private final Long2ObjectMap<Npc> npcMap;

    /**
     * 辅助map，消除switch case；
     * 能改善代码的可读性，可维护性，但可能降低了速度。
     */
    private final EnumMap<GameObjectType, Long2ObjectMap<? extends GameObject>> helperMap = new EnumMap<>(GameObjectType.class);

    public GameObjectContainer() {
        playerMap = createMap(GameObjectType.PLAYER);
        petMap = createMap(GameObjectType.PET);
        npcMap = createMap(GameObjectType.NPC);
    }

    private <V extends GameObject> Long2ObjectMap<V> createMap(GameObjectType gameObjectType) {
        Long2ObjectMap<V> result = new Long2ObjectOpenHashMap<>();
        helperMap.put(gameObjectType, result);
        return result;
    }

    public GameObject getObject(long guid) {
        return totalGameObjectMap.get(guid);
    }

    public int getAllGameObjectNum() {
        return totalGameObjectMap.size();
    }

    public ObjectCollection<GameObject> getAllGameObject() {
        return totalGameObjectMap.values();
    }

    public int getPlayerNum() {
        return playerMap.size();
    }

    public ObjectCollection<Player> getPlayerSet() {
        return playerMap.values();
    }

    public ObjectCollection<Pet> getPetSet() {
        return petMap.values();
    }

    public ObjectCollection<Npc> getNpcSet() {
        return npcMap.values();
    }

    public ObjectCollection<? extends GameObject> getGameObjectSet(GameObjectType gameObjectType) {
        return helperMap.get(gameObjectType).values();
    }

    /**
     * 添加一个场景对象到该视野格子
     *
     * @param gameObject 游戏对象
     * @param <T>        对象的类型
     */
    public <T extends GameObject> void addGameObject(T gameObject) {
        FastCollectionsUtils.requireNotContains(totalGameObjectMap, gameObject.getGuid(), "guid");
        totalGameObjectMap.put(gameObject.getGuid(), gameObject);

        @SuppressWarnings("unchecked")
        Long2ObjectMap<T> objectMap = (Long2ObjectMap<T>) helperMap.get(gameObject.getObjectType());
        assert null != objectMap : gameObject.getObjectType().name();
        objectMap.put(gameObject.getGuid(), gameObject);
    }

    /**
     * 从当前视野格子删除一个对象
     *
     * @param gameObject 游戏对象
     * @param <T>        对象的类型
     */
    public <T extends GameObject> void removeObject(T gameObject) {
        GameObject removeObj = totalGameObjectMap.remove(gameObject.getGuid());
        assert null != removeObj;

        @SuppressWarnings("unchecked")
        Long2ObjectMap<T> objectMap = (Long2ObjectMap<T>) helperMap.get(gameObject.getObjectType());
        assert null != objectMap : gameObject.getObjectType().name();
        objectMap.remove(gameObject.getGuid());
    }
}
