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

package com.wjybxx.fastjgame.findpath;

/**
 * 寻路过程中的中间节点
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/11 22:19
 * github - https://github.com/hl845740757
 */
public class FindPathNode {
    // 对应的格子坐标
    private int x;

    private int y;

    /**
     * 出发节点通过该节点到达目的地的消耗
     * F(x) = G + H
     * which passes current node
     */
    private float F = 0.0f;

    /**
     * 出发格子到当前格子的距离
     * G(x)
     */
    private float G = 0.0f;

    /**
     * 启发函数计算的当前格子到目标格子的距离
     * H(x)
     */
    private float H = 0.0f;

    /**
     * 该格子的父节点
     */
    private FindPathNode parent;

    public FindPathNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public float getF() {
        return F;
    }

    public void setF(float f) {
        this.F = f;
    }

    public float getG() {
        return G;
    }

    public void setG(float g) {
        this.G = g;
    }

    public float getH() {
        return H;
    }

    public void setH(float h) {
        this.H = h;
    }

    public FindPathNode getParent() {
        return parent;
    }

    public void setParent(FindPathNode parent) {
        this.parent = parent;
    }

    public int getDepth() {
        int depth = 0;
        FindPathNode node = this;
        while (node.parent != null) {
            depth++;
            node = node.parent;
        }
        return depth;
    }
}
