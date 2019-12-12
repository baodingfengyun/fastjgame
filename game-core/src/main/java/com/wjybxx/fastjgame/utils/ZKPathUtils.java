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

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.misc.CenterServerId;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.misc.RoleType;
import com.wjybxx.fastjgame.node.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.utils.PathUtils;

import java.io.File;

/**
 * zookeeper节点路径辅助类。
 * 注意区分： 节点名字和节点路径的概念。
 * 节点路径 NodePath：从根节点到当前节点的完整路径。 参考{@link File#getAbsolutePath()}
 * 节点名字 NodeName：节点路径的最后一部分。 参考{@link File#getName()}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 20:49
 * github - https://github.com/hl845740757
 */
public class ZKPathUtils {

    private static final String ONLINE_ROOT_PATH = "/online";
    private static final String NODE_NAME_SEPARATOR = "-";

    /**
     * 寻找节点的名字，即最后一部分
     */
    public static String findNodeName(String path) {
        PathUtils.validatePath(path);
        int delimiterIndex = path.lastIndexOf("/");
        return path.substring(delimiterIndex + 1);
    }

    /**
     * 寻找节点的父节点路径
     *
     * @param path 路径参数，不可以是根节点("/")
     */
    public static String findParentPath(String path) {
        PathUtils.validatePath(path);
        int delimiterIndex = path.lastIndexOf("/");
        // root(nameSpace)
        if (delimiterIndex == 0) {
            throw new IllegalArgumentException("path " + path + " is root");
        }
        return path.substring(0, delimiterIndex);
    }

    /**
     * 获取父节点的名字
     *
     * @param path 节点路径
     */
    public static String findParentNodeName(String path) {
        return findNodeName(findParentPath(path));
    }

    /**
     * 构建一个全路径
     *
     * @param parent   父节点全路径
     * @param nodeName 属性名字
     */
    public static String makePath(String parent, String nodeName) {
        return parent + "/" + nodeName;
    }

    /**
     * 找到一个合适的锁节点，锁的范围越小越好。
     * 对于非根节点来说，使用它的父节点路径。
     * 对应根节点来说，使用全局锁位置。
     *
     * @param path 查询的路径
     * @return 一个合适的加锁路径
     */
    public static String findAppropriateLockPath(String path) {
        PathUtils.validatePath(path);
        int delimiterIndex = path.lastIndexOf("/");
        if (delimiterIndex == 0) {
            return globalLockPath();
        }
        return path.substring(0, delimiterIndex);
    }

    /**
     * 获取全局锁路径
     */
    private static String globalLockPath() {
        return "/globalLock";
    }

    /**
     * 返回全局guidIndex所在路径
     */
    public static String guidIndexPath() {
        return "/mutex/guid/guidIndex";
    }

    /**
     * 运行平台参数路径
     *
     * @param platformType 平台枚举
     */
    public static String platParamPath(PlatformType platformType) {
        return "/config/platform/" + platformType;
    }

    /**
     * 真实服配置节点
     *
     * @param actualServerId 真实服id，现存的服务器
     */
    public static String actualServerConfigPath(CenterServerId actualServerId) {
        return platParamPath(actualServerId.getPlatformType()) + "/actualserver/" + actualServerId.getInnerServerId();
    }

    /**
     * 逻辑服到真实服映射节点
     *
     * @param originalServerId 逻辑服id(合服前的服id)
     */
    public static String originalServerConfigPath(CenterServerId originalServerId) {
        return platParamPath(originalServerId.getPlatformType()) + "/originalserver/" + originalServerId.getInnerServerId();
    }

    /**
     * 获取战区的配置路径
     *
     * @param warzoneId 战区id
     */
    public static String warzoneConfigPath(int warzoneId) {
        return "/config/warzone/" + warzoneId;
    }

    /**
     * @return mongodb的配置路径
     */
    public static String mongoConfigPath() {
        return "/config/mongodb";
    }

    /**
     * 解析顺序节点的序号
     *
     * @param path 有序节点的名字
     * @return zk默认序号是从0开始的，最小为0
     */
    public static int parseSequentialId(String path) {
        final String nodeName = findNodeName(path);
        final int index = nodeName.lastIndexOf("-");
        if (index < 0 || index == path.length() - 1) {
            throw new IllegalArgumentException(path);
        }
        return Integer.parseInt(nodeName.substring(index + 1));
    }

    // region 在线节点路径

    /**
     * @return 在线节点信息的根节点
     */
    public static String onlineRootPath() {
        return ONLINE_ROOT_PATH;
    }

    /**
     * 同一个战区下的服务器注册在同一节点下.
     * <p>
     * 如果战区跨平台：
     * online/warzone-x/
     * 如果战区不夸平台:
     * online/platfrom/warzone
     *
     * @param warzoneId 战区id
     * @return
     */
    public static String onlineWarzonePath(int warzoneId) {
        return "/online/warzone-" + warzoneId;
    }

    /**
     * 寻找在线节点所属的战区
     *
     * @param onlinePath 在线节点的路径，在线节点全部在战区节点之下
     * @return
     */
    private static int findWarzoneId(String onlinePath) {
        // "warzone-x"
        String onlineParentNodeName = findParentNodeName(onlinePath);
        String[] params = onlineParentNodeName.split("-", 2);
        return Integer.parseInt(params[1]);
    }

    // endregion 在线节点路径

    // region 在线节点名字

    /**
     * 通过服务器的节点名字解析服务器的类型。
     * 名字的第一个字段始终是服务器类型的枚举名。
     *
     * @param nodeName 服务器节点名字
     * @return 返回服务器的类型
     */
    public static RoleType parseServerType(String nodeName) {
        return RoleType.valueOf(nodeName.split("-", 2)[0]);
    }

    // ----------------------------------------- 战区服 -------------------------------------------

    /**
     * 为战区创建一个有意义的节点名字
     * {@code WARZONE-1}
     *
     * @param warzoneId 战区id
     * @return 唯一的有意义的名字
     */
    public static String buildWarzoneNodeName(int warzoneId) {
        return StringUtils.joinWith(NODE_NAME_SEPARATOR, RoleType.WARZONE, warzoneId);
    }

    /**
     * 解析战区的节点路径(名字)
     *
     * @param path fullpath
     * @return 战区基本信息
     */
    public static WarzoneNodeName parseWarzoneNodeName(String path) {
        String[] params = findNodeName(path).split(NODE_NAME_SEPARATOR);
        int warzoneId = Integer.parseInt(params[1]);
        return new WarzoneNodeName(warzoneId);
    }

    // ----------------------------------------- 中心服 -------------------------------------------

    /**
     * 为指定服创建一个有意义的节点名字
     * {@code CENTER-TEST-1}
     *
     * @param serverId 平台唯一id
     * @return 唯一的有意义的名字
     */
    public static String buildCenterNodeName(CenterServerId serverId) {
        return StringUtils.joinWith(NODE_NAME_SEPARATOR, RoleType.CENTER, serverId.getPlatformType(), serverId.getInnerServerId());
    }

    /**
     * 解析game节点的路径(名字)
     *
     * @param centerPath fullpath
     * @return game服的信息
     */
    public static CenterNodeName parseCenterNodeName(String centerPath) {
        String[] params = findNodeName(centerPath).split(NODE_NAME_SEPARATOR);
        PlatformType platformType = PlatformType.valueOf(params[1]);
        int serverId = Integer.parseInt(params[2]);
        return new CenterNodeName(new CenterServerId(platformType, serverId));
    }

    // ----------------------------------------- 场景服 -------------------------------------------

    /**
     * 为指定本服scene进程创建一个有意义的节点名字，用于注册到zookeeper
     * {@code SCENE-12345}
     *
     * @param worldGuid worldGuid
     * @return 唯一的有意义的名字
     */
    public static String buildSceneNodeName(long worldGuid) {
        return StringUtils.joinWith(NODE_NAME_SEPARATOR, RoleType.SCENE, worldGuid);
    }

    /**
     * 解析单服scene进程的节点路径(名字)
     *
     * @param path fullpath
     * @return scene包含的基本信息
     */
    public static SceneNodeName parseSceneNodeName(String path) {
        int warzoneId = findWarzoneId(path);
        String[] params = findNodeName(path).split(NODE_NAME_SEPARATOR);
        long worldGuid = Long.parseLong(params[1]);
        return new SceneNodeName(warzoneId, worldGuid);
    }

    // ------------------------------------------- 登录服 ---------------------------------------

    /**
     * @return 登录服在线信息根节点
     */
    public static String onlineLoginRootPath() {
        return onlineRootPath() + "/login";
    }

    /**
     * 为loginserver创建一个节点名字
     * {@code LOGIN-12345}
     *
     * @param worldGuid worldGuid
     * @return 一个唯一的有意义的名字
     */
    public static String buildLoginNodeName(long worldGuid) {
        return StringUtils.joinWith(NODE_NAME_SEPARATOR, RoleType.LOGIN, worldGuid);
    }

    /**
     * 解析loginserver的节点名字
     *
     * @param path 节点路径
     * @return name
     */
    public static LoginNodeName parseLoginNodeName(String path) {
        final String[] params = findNodeName(path).split(NODE_NAME_SEPARATOR);
        long worldGuid = Long.parseLong(params[1]);
        return new LoginNodeName(worldGuid);
    }
    // endregion

    // ---------------------------------------- 网关服 --------------------------------------------

    /**
     * 为网关服创建一个有意义的名字
     *
     * @param serverId  中心服id
     * @param worldGuid 网关服唯一标识
     * @return nodeName
     */
    public static String buildGateNodeName(CenterServerId serverId, long worldGuid) {
        return StringUtils.joinWith(NODE_NAME_SEPARATOR, RoleType.GATE,
                serverId.getPlatformType(), serverId.getInnerServerId(), worldGuid);
    }

    /**
     * 解析网关节点名字
     *
     * @param path 网关节点名字
     * @return name
     */
    public static GateNodeName parseGateNodeName(String path) {
        final int warzoneId = findWarzoneId(path);
        final String[] params = findNodeName(path).split(NODE_NAME_SEPARATOR);
        final PlatformType platformType = PlatformType.valueOf(params[1]);
        final int serverId = Integer.parseInt(params[2]);
        final long worldGuid = Long.parseLong(params[3]);
        return new GateNodeName(warzoneId, platformType, serverId, worldGuid);
    }
}
