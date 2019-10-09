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

package com.wjybxx.fastjgame.scene;

import com.wjybxx.fastjgame.config.SceneConfig;
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.gameobject.GameObject;
import com.wjybxx.fastjgame.gameobject.Npc;
import com.wjybxx.fastjgame.gameobject.Pet;
import com.wjybxx.fastjgame.gameobject.Player;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.mgr.MapDataLoadMgr;
import com.wjybxx.fastjgame.mgr.SceneSendMgr;
import com.wjybxx.fastjgame.mgr.SceneWrapper;
import com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType;
import com.wjybxx.fastjgame.timer.*;
import com.wjybxx.fastjgame.utils.GameConstant;
import com.wjybxx.fastjgame.utils.MathUtils;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;

import static com.wjybxx.fastjgame.protobuffer.p_scene_player.*;
import static com.wjybxx.fastjgame.scene.gameobjectdata.GameObjectType.*;

/**
 * 场景基类，所有的场景都继承该类。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/31 21:17
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public abstract class Scene {

    private static final Logger logger = LoggerFactory.getLogger(Scene.class);

    // 更新单个对象时，缓存对象，避免大量的创建list
    private static final ThreadLocal<List<ViewGrid>> invisibleGridsCache = ThreadLocal.withInitial(() -> new ArrayList<>(GameConstant.VIEWABLE_GRID_NUM));
    private static final ThreadLocal<List<ViewGrid>> visibleGridsCache = ThreadLocal.withInitial(() -> new ArrayList<>(GameConstant.VIEWABLE_GRID_NUM));

    /**
     * 刷新视野格子的间隔
     */
    private static final long DELTA_UPDATE_VIEW_GRIDS = 1000;

    /**
     * 玩家刷帧间隔
     */
    private static final long PLAYER_FRAME_INTERVAL = MathUtils.frameInterval(30);
    private static final long PET_FRAME_INTERVAL = MathUtils.frameInterval(30);
    private static final long NPC_FRAME_INTERVAL = MathUtils.frameInterval(10);

    private final SceneSendMgr sendMrg;
    private final MapDataLoadMgr mapDataLoadMgr;

    /**
     * 该对象上绑定的timer，会随着场景对象的删除而删除
     */
    private final TimerSystem timerSystem;
    /**
     * 下次刷新视野的时间戳；
     */
    private long nextUpdateViewGridTime = 0;

    /**
     * 每一个场景都有一个唯一的id
     */
    private final long guid;
    /**
     * 每个场景都有一个对应的配置文件
     */
    private final SceneConfig sceneConfig;
    /**
     * 场景对象容器,对外提供访问对象的接口
     */
    private final SceneGameObjectManager sceneGameObjectManager;

    private final ViewGridSet viewGridSet;

    /**
     * 场景视野通知策略
     */
    private final GameObjectHandlerMapper<NotifyHandler<?>> notifyHandlerMapper = new GameObjectHandlerMapper<>();

    /**
     * 游戏对象进出场景handler
     * (名字有点长)
     */
    private final GameObjectHandlerMapper<GameObjectInOutHandler<?>> inOutHandlerMapper = new GameObjectHandlerMapper<>();
    /**
     * 场景对象刷帧handler
     */
    private final GameObjectHandlerMapper<GameObjectTickContext<?>> tickContextMapper = new GameObjectHandlerMapper<>();

    /**
     * 场景对序列化handler
     */
    private final GameObjectHandlerMapper<GameObjectSerializer<?>> serializerMapper = new GameObjectHandlerMapper<>();

    public Scene(long guid, SceneConfig sceneConfig, SceneWrapper sceneWrapper) {
        timerSystem = new DefaultTimerSystem(sceneWrapper.getWorldTimeMgr());

        this.guid = guid;
        this.sceneConfig = sceneConfig;
        this.sendMrg = sceneWrapper.getSendMrg();
        this.mapDataLoadMgr = sceneWrapper.getMapDataLoadMgr();

        // 以后再考虑是否需要重用
        MapData mapData = mapDataLoadMgr.loadMapData(sceneConfig.mapId);
        this.viewGridSet = new ViewGridSet(mapData.getMapWidth(),
                mapData.getMapHeight(),
                sceneConfig.viewableRange,
                getViewGridInitCapacityHolder());

        // 创建管理该场景对象的控制器
        this.sceneGameObjectManager = new SceneGameObjectManager(getGameObjectManagerInitCapacityHolder());

        // 注册各种各样的处理器
        registerNotifyHandlers();
        registerInOutHandlers();
        registerTickHandlers();
        registerSerializer();
    }

    private void registerNotifyHandlers() {
        notifyHandlerMapper.registerHandler(PLAYER, new PlayerNotifyHandler());
        notifyHandlerMapper.registerHandler(PET, new BaseNotifyHandler<>());
        notifyHandlerMapper.registerHandler(NPC, new BaseNotifyHandler<>());
    }

    private void registerInOutHandlers() {
        inOutHandlerMapper.registerHandler(PLAYER, new PlayerInOutHandler());
        inOutHandlerMapper.registerHandler(PET, new PetInOutHandler());
        inOutHandlerMapper.registerHandler(NPC, new NpcInOutHandler());
    }

    private void registerTickHandlers() {
        tickContextMapper.registerHandler(PLAYER,
                new GameObjectTickContext<>(PLAYER_FRAME_INTERVAL, new PlayerTickHandler()));

        tickContextMapper.registerHandler(PET,
                new GameObjectTickContext<>(PET_FRAME_INTERVAL, new PetTickHandler()));

        tickContextMapper.registerHandler(NPC,
                new GameObjectTickContext<>(NPC_FRAME_INTERVAL, new NpcTickHandler()));
    }

    private void registerSerializer() {
        serializerMapper.registerHandler(PLAYER, new PlayerSerializer());
        serializerMapper.registerHandler(NPC, new NpcSerializer());
        serializerMapper.registerHandler(PET, new PetSerializer());
    }

    /**
     * 获取创建视野格子的默认容量信息，子类在需要的时候可以覆盖它;
     *
     * @return 默认empty
     */
    protected InitCapacityHolder getViewGridInitCapacityHolder() {
        return InitCapacityHolder.EMPTY;
    }

    /**
     * 获取SceneGameObjectManager创建时的初始容量信息，子类在需要的时候可以覆盖它;
     * 名字是长了点...
     *
     * @return 默认empty
     */
    protected InitCapacityHolder getGameObjectManagerInitCapacityHolder() {
        return InitCapacityHolder.EMPTY;
    }

    public SceneGameObjectManager getSceneGameObjectManager() {
        return sceneGameObjectManager;
    }

    public long getGuid() {
        return guid;
    }

    public SceneConfig getSceneConfig() {
        return sceneConfig;
    }

    public int sceneId() {
        return sceneConfig.sceneId;
    }

    public SceneRegion region() {
        return sceneConfig.sceneRegion;
    }

    public final int mapId() {
        return sceneConfig.mapId;
    }

    public abstract SceneType sceneType();

    public boolean isCross() {
        return sceneConfig.sceneRegion.getSceneWorldType() == SceneWorldType.CROSS;
    }

    // ----------------------------------------核心逻辑开始------------------------------
    // region tick

    /**
     * scene刷帧
     *
     * @param curMillTime 当前系统时间戳
     */
    public void tick(long curMillTime) throws Exception {
        // 检查timer执行
        timerSystem.tick();

        // 场景对象刷帧(场景对象之间刷帧最好也是没有依赖的)
        for (GameObjectType gameObjectType : GameObjectType.values()) {
            GameObjectTickContext<?> tickContext = tickContextMapper.getHandler(gameObjectType);
            if (curMillTime >= tickContext.nextTickTimeMills) {
                tickContext.nextTickTimeMills = curMillTime + tickContext.frameInterval;
                tickGameObjects(gameObjectType, tickContext.handler);
            }
        }

        // 检测视野格子刷新
        if (curMillTime >= nextUpdateViewGridTime) {
            nextUpdateViewGridTime = curMillTime + DELTA_UPDATE_VIEW_GRIDS;
            updateViewableGrid();
        }

    }

    private <T extends GameObject> void tickGameObjects(GameObjectType gameObjectType, GameObjectTickHandler<T> handler) {
        @SuppressWarnings("unchecked")
        ObjectCollection<T> gameObjectSet = (ObjectCollection<T>) sceneGameObjectManager.getGameObjectSet(gameObjectType);
        if (gameObjectSet.size() == 0) {
            return;
        }
        for (T gameObject : gameObjectSet) {
            try {
                handler.tick(gameObject);
            } catch (Exception e) {
                // 避免某一个对象报错导致其它对象tick不到
                logger.error("tick {}-{} caught exception", gameObjectType, gameObject.getGuid(), e);
            }
        }
    }

    // endregion


    // region 视野管理

    /**
     * 刷新所有对象的视野格子
     */
    private void updateViewableGrid() {
        // 视野刷新最好不要有依赖，我们之前项目某些实现导致视野刷新之间有依赖(跟随对象的特殊跟随策略)
        for (GameObjectType gameObjectType : GameObjectType.values()) {
            ObjectCollection<? extends GameObject> gameObjectSet = sceneGameObjectManager.getGameObjectSet(gameObjectType);
            if (gameObjectSet.size() == 0) {
                continue;
            }
            for (GameObject gameObject : gameObjectSet) {
                try {
                    updateViewableGrid(gameObject);
                } catch (Exception e) {
                    // 避免某一个对象报错导致其它对象tick不到
                    logger.error("update {}-{} viewableGrids caught exception", gameObjectType, gameObject.getGuid(), e);
                }
            }
        }
    }

    /**
     * 刷新单个对象的视野格子
     *
     * @param gameObject 指定对象
     * @param <T>        对象类型
     */
    protected final <T extends GameObject> void updateViewableGrid(T gameObject) {
        final ViewGrid preViewGrid = gameObject.getViewGrid();
        final ViewGrid curViewGrid = viewGridSet.getGrid(gameObject.getPosition());

        // 视野范围未发生改变
        if (preViewGrid == curViewGrid) {
            return;
        }

        // 视野格子发生改变(需要找到前后格子差异)
        List<ViewGrid> preViewableGrids = preViewGrid.getViewableGrids();
        List<ViewGrid> curViewableGrids = curViewGrid.getViewableGrids();

        // 视野更新会频繁的执行，不可以大量创建list，因此使用缓存
        List<ViewGrid> invisibleGrids = invisibleGridsCache.get();
        List<ViewGrid> visibleGrids = visibleGridsCache.get();
        try {
            // 旧的哪些看不见了
            for (ViewGrid preViewableGrid : preViewableGrids) {
                if (!viewGridSet.visible(curViewGrid, preViewableGrid)) {
                    invisibleGrids.add(preViewableGrid);
                }
            }
            // 新看见的格子有哪些
            for (ViewGrid curViewableGrid : curViewableGrids) {
                if (!viewGridSet.visible(preViewGrid, curViewableGrid)) {
                    visibleGrids.add(curViewableGrid);
                }
            }

            // 更新所在的视野格子
            gameObject.setViewGrid(curViewGrid);
            preViewGrid.removeObject(gameObject);
            curViewGrid.addGameObject(gameObject);

            @SuppressWarnings("unchecked")
            NotifyHandler<T> notifyHandler = (NotifyHandler<T>) notifyHandlerMapper.getHandler(gameObject);
            // 视野，进入和退出都是相互的，他们离开了我的视野，我也离开了他们的视野
            // 通知该对象，这些对象离开了我的视野
            notifyHandler.notifyGameObjectOthersOut(gameObject, invisibleGrids);
            // 通知旧格子里面的对象，该对象离开了它们的视野
            notifyHandler.notifyOthersGameObjectOut(invisibleGrids, gameObject);

            // 通知该对象，这些格子的对象进入了他的视野
            notifyHandler.notifyGameObjectOthersIn(gameObject, visibleGrids);
            // 通知这些格子的对象，该对象进入了他们的视野
            notifyHandler.notifyOthersGameObjectIn(visibleGrids, gameObject);
        } finally {
            invisibleGrids.clear();
            visibleGrids.clear();
        }
    }

    /**
     * 场景对象广播信息序列化器
     */
    @FunctionalInterface
    private interface GameObjectSerializer<T extends GameObject> {

        /**
         * 序列化游戏对象的关键数据到out中，广播给视野中的玩家
         *
         * @param gameObject 场景游戏对象
         * @param out        结果输出
         */
        void serialize(T gameObject, p_notify_player_others_in.Builder out);

    }

    // 都可以用lambda表达式代替
    private class PlayerSerializer implements GameObjectSerializer<Player> {

        @Override
        public void serialize(Player player, p_notify_player_others_in.Builder out) {
            p_scene_player_data.Builder builder = p_scene_player_data.newBuilder();
            builder.setPlayerGuid(player.getGuid());
            builder.setLogicServerId(player.getLogicServerId());
            builder.setActualServerId(player.getActualServerId());

            // TODO more
            out.addPlayers(builder);
        }
    }

    private class NpcSerializer implements GameObjectSerializer<Npc> {
        @Override
        public void serialize(Npc npc, p_notify_player_others_in.Builder out) {
            p_scene_npc_data.Builder builder = p_scene_npc_data.newBuilder();
            builder.setNpcGuid(npc.getGuid());
            builder.setNpcId(npc.getNpcId());

            // TODO more
            out.addNpcs(builder);
        }
    }

    private class PetSerializer implements GameObjectSerializer<Pet> {

        @Override
        public void serialize(Pet pet, p_notify_player_others_in.Builder out) {
            p_scene_pet_data.Builder builder = p_scene_pet_data.newBuilder();
            builder.setOwnerGuid(pet.getGuid());
            builder.setPetId(pet.getPetId());

            // TODO more
            out.addPets(builder);
        }
    }


    /**
     * 通知骨架实现;
     * 默认其他人进出我的视野什么都不做.
     * 进入玩家视野时，通知玩家；
     * 离开玩家视野时，通知玩家；
     * <p>
     * 对于npc，怪物等，如果需要特殊通知的话，可能和AI有关;
     * 对应player，则需要进行消息通知
     *
     * @param <T>
     */
    private class BaseNotifyHandler<T extends GameObject> implements NotifyHandler<T> {

        @Override
        public void notifyGameObjectOthersIn(T gameObject, List<ViewGrid> newVisibleGrids) {
            // do nothing overridable
        }

        @Override
        public void notifyGameObjectOthersOut(T gameObject, List<ViewGrid> range) {
            // do nothing overridable
        }

        @Override
        public void notifyOthersGameObjectIn(List<ViewGrid> range, T gameObject) {
            @SuppressWarnings("unchecked")
            GameObjectSerializer<T> serializer = (GameObjectSerializer<T>) serializerMapper.getHandler(gameObject);

            p_notify_player_others_in.Builder builder = p_notify_player_others_in.newBuilder();
            serializer.serialize(gameObject, builder);
            p_notify_player_others_in message = builder.build();

            // 通知这些格子的玩家，我进入了你们的视野，不通知自己(gameObject可能在range中)
            if (gameObject.getObjectType() == PLAYER) {
                sendMrg.broadcastPlayerExcept(range, message, (Player) gameObject);
            } else {
                sendMrg.broadcastPlayer(range, message);
            }
        }

        @Override
        public void notifyOthersGameObjectOut(List<ViewGrid> range, T gameObject) {
            p_notify_player_others_out message = p_notify_player_others_out.newBuilder()
                    .addGuids(gameObject.getGuid())
                    .build();

            // 通知这些格子里的玩家，我离开了你们的视野 (gameObject已经不在range中了！)
            sendMrg.broadcastPlayer(range, message);
        }
    }

    private class PlayerNotifyHandler extends BaseNotifyHandler<Player> {

        @SuppressWarnings("unchecked")
        @Override
        public void notifyGameObjectOthersIn(Player player, List<ViewGrid> newVisibleGrids) {
            // 通知玩家，这些格子里的单位进入了视野，player可能在newVisibleGrids中
            int count = 0;
            p_notify_player_others_in.Builder builder = p_notify_player_others_in.newBuilder();
            for (ViewGrid viewGrid : newVisibleGrids) {
                for (GameObjectType gameObjectType : GameObjectType.values()) {
                    ObjectCollection<GameObject> gameObjectSet = (ObjectCollection<GameObject>) viewGrid.getGameObjectSet(gameObjectType);
                    if (gameObjectSet.size() == 0) {
                        continue;
                    }

                    GameObjectSerializer<GameObject> serializer = (GameObjectSerializer<GameObject>) serializerMapper.getHandler(gameObjectType);
                    for (GameObject gameObject : gameObjectSet) {
                        // 这些格子中可能包含自己
                        if (gameObject == player) {
                            continue;
                        }
                        serializer.serialize(gameObject, builder);
                        count++;
                    }
                }
            }
            if (count > 0) {
                sendMrg.sendToPlayer(player, builder.build());

            }
        }

        @Override
        public void notifyGameObjectOthersOut(Player player, List<ViewGrid> range) {
            // 通知玩家，这些格子里的单位离开了我的视野，player一定不在range中
            p_notify_player_others_out.Builder builder = p_notify_player_others_out.newBuilder();
            for (ViewGrid viewGrid : range) {
                if (viewGrid.getAllGameObjectNum() <= 0) {
                    continue;
                }
                for (GameObject gameObject : viewGrid.getAllGameObject()) {
                    builder.addGuids(gameObject.getGuid());
                }
            }
            // 这些格子里没有单位
            if (builder.getGuidsCount() > 0) {
                sendMrg.sendToPlayer(player, builder.build());
            }
        }
    }


    // endregion

    // region 进出场景

    /**
     * 游戏对象进出场景模板实现
     *
     * @param <T>
     */
    protected abstract class AbstractGameObjectInOutHandler<T extends GameObject> implements GameObjectInOutHandler<T> {

        @Override
        public final void processEnterScene(T gameObject) {
            beforeEnterScene(gameObject);
            enterSceneCore(gameObject);
            afterEnterScene(gameObject);
        }

        /**
         * 进入场景的核心逻辑
         *
         * @param gameObject 场景对象
         */
        private void enterSceneCore(T gameObject) {

        }

        /**
         * 在进入场景之前需要进行必要的初始化
         *
         * @param gameObject 场景对象
         */
        protected abstract void beforeEnterScene(T gameObject);

        /**
         * 在成功进入场景之后，可能有额外逻辑
         *
         * @param gameObject 场景对象
         */
        protected abstract void afterEnterScene(T gameObject);

        @Override
        public final void processLeaveScene(T gameObject) {
            beforeLeaveScene(gameObject);
            leaveSceneCore(gameObject);
            afterLeaveScene(gameObject);
        }

        /**
         * 离开场景的核心逻辑(公共逻辑)
         *
         * @param gameObject 场景对象
         */
        private void leaveSceneCore(T gameObject) {

        }

        /**
         * 在离开场景之前可能有额外逻辑
         *
         * @param gameObject 场景对象
         */
        protected abstract void beforeLeaveScene(T gameObject);

        /**
         * 在成功离开场景之后可能有额外逻辑
         *
         * @param gameObject 场景对象
         */
        protected abstract void afterLeaveScene(T gameObject);
    }

    protected class PlayerInOutHandler extends AbstractGameObjectInOutHandler<Player> {

        @Override
        protected void beforeEnterScene(Player player) {

        }

        @Override
        protected void afterEnterScene(Player player) {

        }

        @Override
        protected void beforeLeaveScene(Player player) {

        }

        @Override
        protected void afterLeaveScene(Player player) {

        }
    }

    protected class PetInOutHandler extends AbstractGameObjectInOutHandler<Pet> {
        @Override
        protected void beforeEnterScene(Pet pet) {

        }

        @Override
        protected void afterEnterScene(Pet pet) {

        }

        @Override
        protected void beforeLeaveScene(Pet pet) {

        }

        @Override
        protected void afterLeaveScene(Pet pet) {

        }
    }

    protected class NpcInOutHandler extends AbstractGameObjectInOutHandler<Npc> {

        @Override
        protected void beforeEnterScene(Npc npc) {

        }

        @Override
        protected void afterEnterScene(Npc npc) {

        }

        @Override
        protected void beforeLeaveScene(Npc npc) {

        }

        @Override
        protected void afterLeaveScene(Npc npc) {

        }
    }

    // endregion


    // region 刷帧

    static class GameObjectTickContext<T extends GameObject> {

        /**
         * 帧间隔
         */
        final long frameInterval;

        /**
         * 真正刷帧逻辑
         */
        final GameObjectTickHandler<T> handler;

        /**
         * 下次刷帧事件戳
         */
        long nextTickTimeMills;

        GameObjectTickContext(long frameInterval, GameObjectTickHandler<T> handler) {
            this.frameInterval = frameInterval;
            this.handler = handler;
        }
    }

    /**
     * 游戏对象刷帧模板实现
     *
     * @param <T>
     */
    public abstract class AbstractGameObjectTickHandler<T extends GameObject> implements GameObjectTickHandler<T> {

        @Override
        public final void tick(T gameObject) {
            tickCore(gameObject);
            tickHook(gameObject);
        }

        /**
         * tick公共逻辑(核心逻辑)
         *
         * @param gameObject 场景对象
         */
        private void tickCore(T gameObject) {

        }

        /**
         * 子类独有逻辑
         *
         * @param gameObject 场景对象
         */
        protected void tickHook(T gameObject) {

        }
    }

    protected class PlayerTickHandler extends AbstractGameObjectTickHandler<Player> {

        @Override
        protected void tickHook(Player player) {
            super.tickHook(player);
        }
    }

    protected class PetTickHandler extends AbstractGameObjectTickHandler<Pet> {

        @Override
        protected void tickHook(Pet pet) {
            super.tickHook(pet);
        }
    }

    protected class NpcTickHandler extends AbstractGameObjectTickHandler<Npc> {

        @Override
        protected void tickHook(Npc npc) {
            super.tickHook(npc);
        }
    }
    // endregion

    // ------------------------------------------ 代理timerSystem的方法，只暴露需要暴露的口 -------------------------
    @Nonnull
    protected final TimeoutHandle newTimeout(long timeout, @Nonnull TimerTask<TimeoutHandle> task) {
        return timerSystem.newTimeout(timeout, task);
    }

    @Nonnull
    protected final FixedDelayHandle newFixedDelay(long initialDelay, long delay, @Nonnull TimerTask<FixedDelayHandle> timerTask) {
        return timerSystem.newFixedDelay(initialDelay, delay, timerTask);
    }

    @Nonnull
    protected FixedDelayHandle newFixedDelay(long delay, @Nonnull TimerTask<FixedDelayHandle> timerTask) {
        return timerSystem.newFixedDelay(delay, timerTask);
    }

    @Nonnull
    protected final FixedRateHandle newFixRate(long initialDelay, long period, @Nonnull TimerTask<FixedRateHandle> timerTask) {
        return timerSystem.newFixRate(initialDelay, period, timerTask);
    }

    @Nonnull
    protected final FixedRateHandle newFixRate(long period, @Nonnull TimerTask<FixedRateHandle> timerTask) {
        return timerSystem.newFixRate(period, timerTask);
    }

    /**
     * 清空定时器
     */
    private void cleanTimer() {
        timerSystem.close();
    }
}


