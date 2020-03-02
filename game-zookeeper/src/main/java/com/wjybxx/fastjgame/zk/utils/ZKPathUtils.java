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

package com.wjybxx.fastjgame.zk.utils;

import org.apache.curator.utils.PathUtils;
import org.apache.curator.utils.ZKPaths;

import java.io.File;
import java.util.List;

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

    public static final String GLOBAL_LOCK_PATH = "/_globalLock";

    /**
     * 给定一个全路径，返回节点名字。
     * i.e. "/one/two/three" will return "three"
     */
    public static String findNodeName(String fullPath) {
        return ZKPaths.getNodeFromPath(fullPath);
    }

    /**
     * 获取父节点的名字
     *
     * @param fullPath 节点路径
     */
    public static String findParentNodeName(String fullPath) {
        return findNodeName(findParentPath(fullPath));
    }

    /**
     * 寻找节点的父节点路径
     * i.e. "/one/two/three" will return "/one/two"
     *
     * @param fullPath 路径参数，不可以是根节点("/")
     */
    public static String findParentPath(String fullPath) {
        return ZKPaths.getPathAndNode(fullPath).getPath();
    }

    /**
     * 给定一个全路径，返回各个独立的节点名字。
     * i.e. "/one/two/three" will return "one, two, three"
     * 根节点返回空集合。
     */
    public static List<String> split(String fullPath) {
        return ZKPaths.split(fullPath);
    }

    /**
     * 构建一个全路径
     *
     * @param parent 父节点全路径
     * @param child  属性名字
     * @return fullPath
     */
    public static String makePath(String parent, String child) {
        return ZKPaths.makePath(parent, child);
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
            return GLOBAL_LOCK_PATH;
        }
        return path.substring(0, delimiterIndex);
    }

    /**
     * 解析顺序节点的序号
     *
     * @param path 有序节点的名字
     * @return zk默认序号是从0开始的，最小为0
     */
    public static int parseSequentialId(String path) {
        return Integer.parseInt(ZKPaths.extractSequentialSuffix(path));
    }

    public static void main(String[] args) {
        System.out.println(split("/one/two/three"));
    }
}
