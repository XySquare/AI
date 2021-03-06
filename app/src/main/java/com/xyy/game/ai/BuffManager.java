package com.xyy.game.ai;


import android.util.Log;

import java.util.ArrayList;

/**
 * Created by ${XYY} on ${2016/9/7}.
 */
public class BuffManager {
    public final class BuffRecord {
        private Buff buff;
        /**
         * 剩余时间
         */
        private float timer;
        /**
         * 叠加层数
         */
        private int counter;
        /**
         * 剩余时间比
         */
        private float remainTimeRatio;

        private void initialize(Buff buff) {
            this.buff = buff;
            timer = buff.duration;
            counter = 1;
        }

        private boolean add() {
            //若该Buff小于最大叠加层数，或叠加无上限...
            if (counter < buff.max || buff.max == 0) {
                //Buff叠加一层
                counter++;
                //刷新Buff时间
                timer = buff.duration;
                Log.i("BuffMag", HOST + " 获得 " + buff.name +"("+ counter + "/" + buff.max + ") 效果");
                //需刷新当前属性
                return true;
            }
            //若叠加以达上限，如果Buff可刷新
            else if (buff.refresh) {
                //刷新Buff时间
                timer = buff.duration;
                Log.i("BuffMag", HOST + " 获得 " + buff.name + "(MAX) 效果，并刷新");
            } else {
                Log.i("BuffMag", HOST + " 获得 " + buff.name +"(MAX) 效果，但该效果无法刷新");
            }
            //无需刷新当前属性
            return false;
        }

        private boolean accessTime(float deltaTime) {
            if (timer > 0) {
                timer -= deltaTime;
                remainTimeRatio = timer / buff.duration;
                if (timer <= 0)
                    return true;
            }
            return false;
        }

        public float getRemainTimeRatio() {
            return remainTimeRatio;
        }

        public int getCounter() {
            return counter;
        }

        public int getBuffIco() {
            return buff.ico;
        }

        private boolean equals(int uid) {
            return buff.equals(uid);
        }
    }

    private final String HOST;
    /**
     * 角色当前持有的Buff
     */
    private ArrayList<BuffRecord> buffList;
    /**
     * 原始属性
     */
    private int[] primitiveProperty;
    /**
     * 叠加Buff后的数值
     */
    private int[] currentProperty;
    /**
     * Buff叠加的属性统计（数值）
     */
    private int[] values;
    /**
     * Buff叠加的属性统计（百分比）
     */
    private float[] percentages;


    public BuffManager(String host, int[] primitiveProperty, int[] currentProperty) {
        HOST = host;

        buffList = new ArrayList<>();

        values = new int[5];
        percentages = new float[]{1, 1, 1, 1, 1};

        this.primitiveProperty = primitiveProperty;
        this.currentProperty = currentProperty;
    }

    /**
     * 通过uid为该角色添加Buff
     *
     * @param uid Buff对应的uid
     */
    public void addBuff(int uid) {
        //查找当前是否存在相同的Buff
        int index;
        for (index = 0; index < buffList.size(); index++) {
            if (buffList.get(index).equals(uid)) {
                //如果存在，则尝试叠加
                if (buffList.get(index).add()) {
                    //如果叠加成功
                    //Buff buff = getBuff(uid);
                    Buff buff = buffList.get(index).buff;
                    //则刷新当前属性
                    int len = buff.type.length;
                    for (short i = 0; i < len; i++) {
                        char type = buff.type[i];
                        values[type] += buff.value[i];
                        percentages[type] += buff.percentage[i];

                        currentProperty[type] = (int) (primitiveProperty[type] * percentages[type] + values[type]);
                    }
                }
                return;
            }
        }
        //如果不存在，则新增
        Buff buff = Buff.get(uid);
        BuffRecord buffRecord = new BuffRecord();
        buffRecord.initialize(buff);
        buffList.add(buffRecord);
        //则刷新当前属性
        int len = buff.type.length;
        for (short i = 0; i < len; i++) {
            char type = buff.type[i];
            values[type] += buff.value[i];
            percentages[type] += buff.percentage[i];

            currentProperty[type] = (int) (primitiveProperty[type] * percentages[type] + values[type]);
        }
        Log.i("BuffMag", HOST + " 获得 " + buff.name + " 效果");
    }

    /**
     * 更新Buff剩余时间，移除过期Buff
     */
    public void update(float deltaTime) {
        for (int index = 0; index < buffList.size(); index++) {
            BuffRecord buffRecord = buffList.get(index);
            //若当前Buff剩余时间为0
            if (buffRecord.accessTime(deltaTime)) {
                //刷新当前属性
                Buff buff = buffRecord.buff;
                int count = buffRecord.counter;
                int len = buff.type.length;
                for (short j = 0; j < len; j++) {
                    char type = buff.type[j];
                    values[type] -= (buff.value[j] * count);
                    percentages[type] -= (buff.percentage[j] * count);

                    currentProperty[type] = (int) (primitiveProperty[type] * percentages[type] + values[type]);
                }

                buffList.remove(index);
            }
        }
    }

    public void updatePropertyType(char type) {
        currentProperty[type] = (int) (primitiveProperty[type] * percentages[type] + values[type]);
    }

    public ArrayList<BuffRecord> getBuffList() {
        return buffList;
    }

    /**
     * 通过uid获得Buff
     * @param uid Buff对应的uid
     * @return Buff对象
     */
    /*private Buff getBuff(int uid){
        for (Buff buff:buffs) {
            if(buff.equals(uid)) return buff;
        }
        Log.e("Buff", "无法找到对应 Buff uid = " + uid);
        return nullBuff;
    }*/

    //一个空Buff
    //private Buff nullBuff = new Buff(Buff.NULL, new char[0], new int[0], new float[0], 0, 0, false, -1, "");

}
