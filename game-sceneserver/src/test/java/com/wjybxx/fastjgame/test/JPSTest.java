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

import com.wjybxx.fastjgame.findpath.WalkableGridStrategy;
import com.wjybxx.fastjgame.findpath.WalkableGridStrategys;
import com.wjybxx.fastjgame.findpath.jps.JPSFindPathContext;
import com.wjybxx.fastjgame.findpath.jps.JPSFindPathStrategy;
import com.wjybxx.fastjgame.scene.GridObstacle;
import com.wjybxx.fastjgame.scene.MapData;
import com.wjybxx.fastjgame.scene.MapGrid;
import com.wjybxx.fastjgame.shape.Point2D;
import com.wjybxx.fastjgame.utils.GameConstant;
import com.wjybxx.fastjgame.utils.GameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 左键设置遮挡，右键设置起始点和终点
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/12 19:14
 * github - https://github.com/hl845740757
 */
public class JPSTest {

    private static final JPSFindPathStrategy jpsFindPathStrategy = new JPSFindPathStrategy();

    private static final WalkableGridStrategy walkableStrategy = WalkableGridStrategys.valueOf(GridObstacle.FREE);

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

    private static final  String OBSTACLE_FILE_NAME = "_obstacleInfo.txt";

    /**
     * 主界面
     */
    private final JFrame jFrame = new JFrame("JPS寻路测试界面，就当做扫雷界面，左键刷遮挡，右键刷起始点和目标点");
    /**
     * 显示寻路结果信息
     */
    private JTextArea jTextArea = new JTextArea();

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
    private List<MapGrid> recordPath = null;


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
        startBtn.addActionListener(this::onClickStartBtn);

        JButton resetBtn=new JButton("重置地图");
        resetBtn.addActionListener(this::onClickResetBtn);

        JButton clearBtn=new JButton("清除路径");
        clearBtn.addActionListener(this::onClickClearBtn);

        JButton loadBtn=new JButton("加载地图");
        loadBtn.addActionListener(this::onClickLoacBtn);

        JButton storeBtn=new JButton("保存地图");
        storeBtn.addActionListener(this::onClickStoreBtn);

        JPanel jPanel=new JPanel();
        jPanel.setLayout(new GridLayout(2,3));
        jPanel.setMaximumSize(new Dimension(PANEL_WIDTH,100));
        jPanel.add(storeBtn); jPanel.add(clearBtn); jPanel.add(startBtn);
        jPanel.add(loadBtn);  jPanel.add(resetBtn); jPanel.add(jTextArea);

        jSplitPane.add(jPanel, BorderLayout.SOUTH);
    }

    private void onClickLoacBtn(ActionEvent actionEvent) {
        loadMapInfo();
    }


    private void onClickStoreBtn(ActionEvent actionEvent) {
        storeMapInfo();
    }

    /**
     * 点击清除按钮，清除路径
     * @param actionEvent
     */
    private void onClickClearBtn(ActionEvent actionEvent) {
        clearPath();
    }

    /**
     * 点击重置地图按钮，清空所有信息
     * @param actionEvent
     */
    private void onClickResetBtn(ActionEvent actionEvent) {
        resetMap();
    }

    private void resetMap() {
        startGrid = null;
        endGrid = null;

        forEachGrid((mapGrid, rowIndex, colIndex) -> {
            JButton jButton = mapGridItemArray[rowIndex][colIndex];
            // 标记为可行走
            updateGridObstacleValueAndColor(mapGrid, jButton, GridObstacle.FREE);
            // 取消起点和终点标记
            jButton.setText("");
        });
    }

    /**
     * 点击开始寻路
     * @param actionEvent
     */
    private void onClickStartBtn(ActionEvent actionEvent) {
        clearPath();

        if (startGrid == null || endGrid == null){
            jTextArea.setText("缺少起始点或目标点");
            return;
        }
        long startTimeMills = System.nanoTime();
        JPSFindPathContext context = new JPSFindPathContext(mapData, startGrid, endGrid, walkableStrategy);
        List<Point2D> path = jpsFindPathStrategy.findPath(context);
        long costTime = System.nanoTime() - startTimeMills;

        if (path == null || path.size() == 0){
            jTextArea.setText("目标点不可达, time (纳秒)=" + costTime + ", 微秒=" + TimeUnit.NANOSECONDS.toMicros(costTime));
            return;
        }

        this.recordPath = new ArrayList<>(path.size());
        for (int index=0, end=path.size(); index<end;index++){
            if (index == 0 || index == end - 1){
                continue;
            }
            Point2D point2D = path.get(index);
            MapGrid mapGrid = mapData.getGrid(point2D);
            JButton jButton = mapGridItemArray[mapGrid.getRowIndex()][mapGrid.getColIndex()];
            jButton.setText(index + "");

            recordPath.add(mapGrid);
        }
        jTextArea.setText("寻路成功, time(纳秒)=" + costTime + ", 微秒=" + TimeUnit.NANOSECONDS.toMicros(costTime));
    }

    private void clearPath(){
        if (null == recordPath){
            return;
        }

        for (MapGrid mapGrid:recordPath){
            JButton jButton = mapGridItemArray[mapGrid.getRowIndex()][mapGrid.getColIndex()];
            jButton.setText("");
        }
        recordPath = null;
    }

    private void addMapGridPanel(JPanel jSplitPane) {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(ROW_COUNT, COL_COUNT));

        forEachGrid((mapGrid, rowIndex, colIndex) -> {
            JButton mapGridItem = newMapGridItem(rowIndex, colIndex);
            jPanel.add(mapGridItem);
        });

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

    /**
     * 地图创建之后可以使用
     */
    private void forEachGrid(GridConsumer consumer){
        for (int rowIndex = ROW_COUNT - 1; rowIndex >= 0; rowIndex--) {
            for (int colIndex = 0; colIndex < COL_COUNT; colIndex++) {
                MapGrid mapGrid = mapData.getGrid2(rowIndex, colIndex);
                consumer.accept(mapGrid, rowIndex, colIndex);
            }
        }
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
                } else if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
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

    private void storeMapInfo(){
        Pos start = null == startGrid ? null : new Pos(startGrid.getRowIndex(), startGrid.getColIndex());
        Pos end = null == endGrid ? null : new Pos(endGrid.getRowIndex(), endGrid.getColIndex());

        final List<Pos> obstacles = new ArrayList<>(32);

        forEachGrid((mapGrid, rowIndex, colIndex) -> {
            if (mapGrid.getObstacleValue() == GridObstacle.OBSTACLE){
                obstacles.add(new Pos(rowIndex, colIndex));
            }
        });
        byte[] jsonBytes = GameUtils.serializeToJsonBytes(new MapInfoBean(start, end, obstacles));

        File file = new File(OBSTACLE_FILE_NAME);
        if (file.exists()){
            file.delete();
        }
        try (BufferedOutputStream buffer=new BufferedOutputStream(new FileOutputStream(file))){
            buffer.write(jsonBytes);
            jTextArea.setText("store success, filePath="+file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMapInfo() {
        File file = new File(OBSTACLE_FILE_NAME);
        if (!file.exists()){
            jTextArea.setText("缺少文件信息， filePath="+file.getAbsolutePath());
            return;
        }

        resetMap();

        try (BufferedInputStream buffer=new BufferedInputStream(new FileInputStream(file))){
            byte[] jsonBytes = new byte[buffer.available()];
            buffer.read(jsonBytes);
            MapInfoBean mapInfoBean = GameUtils.parseFromJsonBytes(jsonBytes, MapInfoBean.class);

            forEachGrid((mapGrid, rowIndex, colIndex) -> {
                if (mapInfoBean.isObstacle(rowIndex, colIndex)){
                    JButton jButton = mapGridItemArray[rowIndex][colIndex];
                    updateGridObstacleValueAndColor(mapGrid, jButton, GridObstacle.OBSTACLE);
                }
            });

            if (mapInfoBean.startGrid != null && mapData.inside2(mapInfoBean.startGrid.rowIndex, mapInfoBean.startGrid.colIndex)){
                this.startGrid = mapData.getGrid2(mapInfoBean.startGrid.rowIndex, mapInfoBean.startGrid.colIndex);
                JButton jButtons=mapGridItemArray[mapInfoBean.startGrid.rowIndex][mapInfoBean.startGrid.colIndex];
                jButtons.setText("S");
            }

            if (mapInfoBean.endGrid != null && mapData.inside2(mapInfoBean.endGrid.rowIndex, mapInfoBean.endGrid.colIndex)){
                this.endGrid = mapData.getGrid2(mapInfoBean.endGrid.rowIndex, mapInfoBean.endGrid.colIndex);
                JButton jButtons=mapGridItemArray[mapInfoBean.endGrid.rowIndex][mapInfoBean.endGrid.colIndex];
                jButtons.setText("E");
            }
            jTextArea.setText("load success, filePath="+file.getAbsolutePath());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static class MapInfoBean {

        private final Pos startGrid;
        private final Pos endGrid;
        private final List<Pos> obstacles;

        private MapInfoBean(Pos startGrid, Pos endGrid, List<Pos> obstacles) {
            this.startGrid = startGrid;
            this.endGrid = endGrid;
            this.obstacles = obstacles;
        }

        public Pos getStartGrid() {
            return startGrid;
        }

        public Pos getEndGrid() {
            return endGrid;
        }

        public List<Pos> getObstacles() {
            return obstacles;
        }

        public boolean isObstacle(int rowIndex, int colIndex){
            for (Pos pos : obstacles){
                if (pos.rowIndex == rowIndex && pos.colIndex == colIndex){
                    return true;
                }
            }
            return false;
        }
    }

    private static class Pos{

        private final int rowIndex;
        private final int colIndex;

        private Pos(int rowIndex, int colIndex) {
            this.rowIndex = rowIndex;
            this.colIndex = colIndex;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public int getColIndex() {
            return colIndex;
        }
    }

    @FunctionalInterface
    private interface GridConsumer{

        void accept(MapGrid mapGrid, int rowIndex, int colIndex);
    }
}

