package com.xyy.game.ai;

import com.xyy.game.ai.Character.Character;
import com.xyy.game.framework.Pixmap;
import com.xyy.game.util.iPoint;

/**
 * 用于构建世界的数据，
 * 数据将被传递给WorldBuilder
 * Created by ${XYY} on ${2016/8/26}.
 */
public interface WorldData {
    /**
     * @return 地图唯一标识符
     */
    String getUid();
    /**
     * @return 根角色
     */
    Character getRootCharacter(Stage stage);

    /**
     * @return 地图点集
     */
    iPoint[] getMapPoints();

    /**
     * @return 返回碰撞检测分块宽（高）度（px）
     */
    int getBlockWidth();

    /**
     * @return 地图背景
     */
    Pixmap getMapBackGround();

    /**
     * @return 在该地图中所要用到的Buff实例
     */
    Buff[] getBuffList();

    /**
     * @return 需加载的权重
     */
    String[] getDataToLoad();

    /**
     * @return 玩家开始地点坐标
     */
    int[] getPlayerStartPoint();
}
