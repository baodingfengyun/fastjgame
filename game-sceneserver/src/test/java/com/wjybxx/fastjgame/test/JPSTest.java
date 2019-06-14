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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.findpath.JPSFindPathContext;
import com.wjybxx.fastjgame.findpath.JPSFindPathStrategy;
import com.wjybxx.fastjgame.findpath.WalkableGridStrategys;
import com.wjybxx.fastjgame.scene.GridObstacle;
import com.wjybxx.fastjgame.scene.MapData;
import com.wjybxx.fastjgame.scene.MapGrid;
import com.wjybxx.fastjgame.shape.Point2D;
import com.wjybxx.fastjgame.utils.GameConstant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 左键设置遮挡，右键设置起始点和终点
 *
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/12 19:14
 * @github - https://github.com/hl845740757
 */
public class JPSTest {

    private static final Field obstacleValueField;

    static {
        Field field = null;
        try {
            field = MapGrid.class.getDeclaredField("obstacleValue");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        obstacleValueField = field;

    }

    private static final int PANEL_WIDTH = 1020;
    private static final int PANEL_HEIGHT = 620;

    private static final int COL_COUNT = PANEL_WIDTH / GameConstant.MAP_GRID_WIDTH;
    private static final int ROW_COUNT = PANEL_HEIGHT / GameConstant.MAP_GRID_WIDTH;

    private static final String midPathMark = "√";

    /**
     * 主界面
     */
    private final JFrame jFrame = new JFrame("JPS寻路测试界面，就当做扫雷界面");

    private JTextArea resultTextArea = new JTextArea();

    private final JPSFindPathStrategy jpsFindPathStrategy = new JPSFindPathStrategy();

    /**
     * 地图资源
     */
    private MapData mapData = newMapData();

    /**
     * 地图格子对应的按钮
     */
    private JButton[][] mapGridItemArray = new JButton[ROW_COUNT][COL_COUNT];

    /**
     * 初始节点和终止节点
     */
    private MapGrid startGrid = null;
    private MapGrid endGrid = null;


    public void show(){
        jFrame.setMinimumSize(new Dimension(PANEL_WIDTH,PANEL_HEIGHT));

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jFrame.add(jPanel);

        // 添加地图格子
        addMapGridPanel(jPanel);
        // 添加启动和重置按钮
        addStartAndResetBtn(jPanel);

        jFrame.pack();
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    // region 开始统计
    private void addStartAndResetBtn(JPanel jSplitPane) {
        JButton startBtn=new JButton("开始寻路");
        startBtn.addActionListener(this::onClickStart);

        JButton resetBtn=new JButton("重置地图");
        resetBtn.addActionListener(this::onClickReset);

        JPanel jPanel=new JPanel();
        jPanel.setLayout(new GridLayout(1,3));
        jPanel.setMaximumSize(new Dimension(PANEL_WIDTH,50));
        jPanel.add(startBtn);
        jPanel.add(resetBtn);
        jPanel.add(resultTextArea);

        jSplitPane.add(jPanel, BorderLayout.SOUTH);
    }

    private void onClickReset(ActionEvent actionEvent) {
        startGrid = null;
        endGrid = null;

        for (int rowIndex = ROW_COUNT - 1; rowIndex >= 0; rowIndex--){
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++){
                MapGrid mapGrid = mapData.getGrid2(rowIndex, colIndex);
                JButton jButton = mapGridItemArray[rowIndex][colIndex];
                // 标记为可行走
                updateGridObstacleValueAndColor(mapGrid, jButton, GridObstacle.FREE);
                // 取消起点和终点标记
                jButton.setText("");
            }
        }
    }

    private void onClickStart(ActionEvent actionEvent) {
        clearOldData();

        if (startGrid == null || endGrid == null){
            resultTextArea.setText("缺少起始点或目标点");
            return;
        }
        long startTimeMills = System.currentTimeMillis();
        JPSFindPathContext context = new JPSFindPathContext(mapData, startGrid, endGrid, WalkableGridStrategys.playerWalkableGrids);
        List<Point2D> path = jpsFindPathStrategy.findPath(context);
        long costTime = System.currentTimeMillis() - startTimeMills;

        if (path == null || path.size() == 0){
            resultTextArea.setText("目标点不可达, time=" + costTime);
            return;
        }

        for (int index=0, end=path.size(); index<end;index++){
            if (index == 0 || index == end - 1){
                continue;
            }
            Point2D point2D = path.get(index);
            MapGrid mapGrid = mapData.getGrid(point2D);
            JButton jButton = mapGridItemArray[mapGrid.getRowIndex()][mapGrid.getColIndex()];
            jButton.setText(index + "");
        }
        resultTextArea.setText("寻路成功, time=" + costTime);
    }

    private void clearOldData() {
        for (int rowIndex = ROW_COUNT - 1; rowIndex >= 0; rowIndex--){
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++){
                JButton mapGridItem = mapGridItemArray[rowIndex][colIndex];
                if (midPathMark.equals(mapGridItem.getText())){
                    mapGridItem.setText("");
                }
            }
        }

    }

    private void addMapGridPanel(JPanel jSplitPane) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(ROW_COUNT, COL_COUNT));

        // 最上面是最后一行，最下面是第一行
        for (int rowIndex = ROW_COUNT - 1; rowIndex >= 0; rowIndex--){
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++){
                JButton mapGridItem = newMapGridItem(rowIndex, colIndex);
                jPanel.add(mapGridItem);
            }
        }
        jSplitPane.add(jPanel,BorderLayout.NORTH);
    }

    // endregion

    private MapData newMapData(){
        MapGrid[][] allMapGrids = new MapGrid[ROW_COUNT][COL_COUNT];
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++){
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++){
                allMapGrids[rowIndex][colIndex] = new MapGrid(rowIndex,colIndex, GridObstacle.FREE, true);
            }
        }
        return new MapData(1,allMapGrids);
    }

    private JButton newMapGridItem(int rowIndex, int colIndex){
        JButton jButton = new JButton();
        String properties = rowIndex + "|" + colIndex;
        jButton.setToolTipText(properties);
        jButton.setBackground(Color.white);
        jButton.setPreferredSize(new Dimension(GameConstant.MAP_GRID_WIDTH, GameConstant.MAP_GRID_WIDTH));
        jButton.setHorizontalTextPosition(SwingConstants.CENTER);
        jButton.setVerticalTextPosition(SwingConstants.CENTER);

        jButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                MapGrid grid = mapData.getGrid2(rowIndex, colIndex);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    invertGridObstacleValue(grid, jButton);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // 在下的电脑，右键对应 BUTTON3
                    updateStartOrEndGrid(grid, jButton);
                }else{
                    super.mousePressed(e);
                }
            }
        });

        mapGridItemArray[rowIndex][colIndex] = jButton;

        return jButton;
    }

    /**
     * 鼠标左键，点击切换遮挡
     * @param grid
     * @param jButton
     */
    private static void invertGridObstacleValue(MapGrid grid, JButton jButton) {
        GridObstacle newValue = grid.getObstacleValue() == GridObstacle.FREE ? GridObstacle.OBSTACLE : GridObstacle.FREE;
        updateGridObstacleValueAndColor(grid, jButton, newValue);
    }

    /**
     * 更新格子的遮挡值和颜色
     * @param grid
     * @param jButton
     * @param value
     */
    private static void updateGridObstacleValueAndColor(MapGrid grid, JButton jButton, GridObstacle value){
        try {
            obstacleValueField.set(grid, value);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        jButton.setBackground(value == GridObstacle.FREE ? Color.white:Color.BLUE);
    }

    private void updateStartOrEndGrid(MapGrid grid, JButton jButton){
        if (grid == startGrid){
            startGrid = null;
            jButton.setText("");
            return;
        }

        if (grid == endGrid){
            endGrid = null;
            jButton.setText("");
            return;
        }

        if (null == startGrid){
            startGrid = grid;
            jButton.setText("S");
            return;
        }

        if (null == endGrid){
            endGrid = grid;
            jButton.setText("E");
            return;
        }
    }


    public static void main(String[] args) {
        new JPSTest().show();
    }
}

