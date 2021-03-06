package com.xyy.game.ai.Character.NPC;

import android.support.annotation.NonNull;
import android.util.Log;

import com.xyy.game.ANN.Data;
import com.xyy.game.ANN.NeuralNet;
import com.xyy.game.AStar.AStarFindPath;
import com.xyy.game.FSM.FSMState;
import com.xyy.game.FSM.FSMTransition;
import com.xyy.game.FSM.FiniteStateMachine;
import com.xyy.game.ai.Assets;
import com.xyy.game.ai.Attack.AtkInfo;
import com.xyy.game.ai.Attack.Attack;
import com.xyy.game.ai.Attack.EnergyBox;
import com.xyy.game.ai.Attack.GeneralCircleAttack;
import com.xyy.game.ai.Attack.LifeBox;
import com.xyy.game.ai.Attack.BuffArea;
import com.xyy.game.ai.Buff;
import com.xyy.game.ai.Character.Character;
import com.xyy.game.ai.Effect.MultSquareEffect;
import com.xyy.game.ai.Environment;
import com.xyy.game.ai.Stage;
import com.xyy.game.framework.Graphics;
import com.xyy.game.util.GeneralLine;
import com.xyy.game.util.IntArrayList;
import com.xyy.game.util.Line;

import java.util.ArrayList;

/**
 * Created by berryice on 2017/4/20.
 */

public class AssistCharacter extends Character implements NPC{
    private static final String TAG = "AssistCharacter";

    public static final Data sData = new Data() {
        public double[][] GetInputSet() {
            return new double[][]{{-0.1,0,0}, {0,0,0}, {1,0,0}, {0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}};
        }

        public double[][] GetOutputSet() {
            return new double[][]{{0,1,0,0}, {0,0,0,0},{1,0,0,0}, {0,0,0,1}, {0,0,1,0}, {0,0,0,1}, {0,0,1,0}};
        }
    };
    private static final int minDistanceFromPlayer = 300;
    private static final int avoidSensitivity = 300;
    private static final int disperseSensitivity = 300;

    /**
     * x/y方向上的速度（px/s）
     */
    private float vx, vy;
    /**
     * 旋转角（角度）
     */
    private float rotation;
    /**
     * 攻击间隔计时器
     */
    private float delayTimer;
    /**
     * 攻击间隔
     */
    private float atkDelay;
    /**
     * 适应度
     */
    private float fitness;
    /**
     * 存活时间
     */
    private int liveTime;
    /**
     * 有限状态机
     */
    private FiniteStateMachine FSM;
    /**
     * 输入：到玩家的距离，到最近的攻击对象的距离，到最近的攻击对象的连线与到玩家连线的夹角（弧度）
     * 输出：前/后/左/右
     */
    private NeuralNet neuralNet = new NeuralNet(3, 4, 6);
    /**
     * 缓存网络的输入
     */
    private double[] NetInputs = new double[3];
    /**
     * A*寻路
     */
    private AStarFindPath aStarFindPath;
    /**
     * 寻得的路径
     */
    private IntArrayList path;
    /**
     * true=玩家坐标更改，请求新路径
     */
    private boolean reqNewPath;
    /**
     * 寻路起点，终点
     */
    private int start, end;
    /**
     * 寻路线程
     */
    private Thread findPathThread;
    /**
     * 当前状态
     */
    private boolean findPath;

    private boolean shouldAddBuff;

    private ArrayList<Character> hostileList;

    public AssistCharacter(Stage stage) {
        super(stage,TAG);
        this.r = 37;
        setV(200);
        setMaxHp(20);
        setHp(20);

        /**
         * 初始化状态表
         */
        FSMState[] states = new FSMState[]{new AssistCharacter.State_Active(), new AssistCharacter.State_FindPath()};
        /**
         * 初始化有限状态机
         */
        FSM = new FiniteStateMachine(states);
        /**
         * 初始化A*寻路
         */
        aStarFindPath = new AStarFindPath();
        /**
         * 初始化当前状态为非寻路状态，即正常运行状态
         */
        findPath = false;

        start = end = -1;

        findPathThread = new Thread() {
            @Override
            public void run() {
                //等待，直到update()被调用
                while (start == -1 || end == -1) {
                    try { sleep(100); } catch (InterruptedException ignored) {}
                }
                while (!isDead()) {
                    if (!findPath) {
                        int ifSee = AStarFindPath.ifSeeDirectly(start, end);
                        if (ifSee == 2) {
                            path = aStarFindPath.findPath(start, end);
                            if(!path.isEmpty()) {
                                reqNewPath = false;
                                findPath = true;
                            }else{
                                findPath = false;
                            }
                        } else {
                            try { sleep(100); } catch (InterruptedException ignored) {}
                        }
                    } else {
                        int ifSee = AStarFindPath.ifSeeDirectly(start, end);
                        if (ifSee == 0) {
                            findPath = false;
                        } else if (reqNewPath) {
                            path = aStarFindPath.findPath(start, end);
                            if(!path.isEmpty()) {
                                reqNewPath = false;
                            }else{
                                findPath = false;
                            }
                        } else {
                            try { sleep(100); } catch (InterruptedException ignored) {}
                        }
                    }
                }
            }
        };
        findPathThread.start();
    }

    public void initialize(@NonNull NPC parent, int x, int y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        //Log.e(getName(),"initialize x/y = "+x+" / "+y);

        setHp(getMaxHp());
        this.fitness = 0;
        this.liveTime = 0;

        vx = vy = 0;

        atkDelay = (float) (3 + 2 * Math.random());

        shouldAddBuff = false;
    }

    /**
     * 状态转换，从活动到寻路
     * 在FSMState中以线性表的形式储存
     */
    private class Active2FindPath implements FSMTransition {
        /**
         * 如果玩家不能直接可见
         *
         * @return true=玩家不能直接可见，需状态转换
         */
        @Override
        public boolean isValid(float deltaTime, Environment environment) {
            return findPath;
        }

        /**
         * 转换至寻路状态
         *
         * @return 寻路状态
         */
        @Override
        public int getNextState() {
            return 1;
        }
    }

    /**
     * 状态转换，从寻路到活动
     * 在FSMState中以线性表的形式储存
     */
    private class FindPath2Active implements FSMTransition {
        /**
         * 如果玩家直接可见
         *
         * @return true=玩家直接可见，需状态转换
         */
        @Override
        public boolean isValid(float deltaTime, Environment environment) {
            return !findPath;
        }

        /**
         * 转换至活动状态
         *
         * @return 活动状态
         */
        @Override
        public int getNextState() {
            return 0;
        }
    }

    /**
     * 活动状态
     */
    private class State_Active implements FSMState {
        /**
         * 初始化状态转换表
         */
        private FSMTransition[] transitions = new FSMTransition[]{new AssistCharacter.Active2FindPath()};

        @Override
        public void onEnter() {

        }

        @Override
        public void onUpdate(float deltaTime, Environment environment) {
            active(deltaTime, environment);
        }

        @Override
        public void onExit() {

        }

        @Override
        public FSMTransition[] getTransitions() {
            return transitions;
        }
    }

    /**
     * 寻路状态
     */
    private class State_FindPath implements FSMState {

        private int targetIndex;
        private int targetX, targetY;

        private float oldX, oldY;

        /**
         * 初始化状态转换表
         */
        private FSMTransition[] transitions;

        private State_FindPath() {
            transitions = new FSMTransition[]{new AssistCharacter.FindPath2Active()};
        }

        @Override
        public void onEnter() {
            targetIndex = path.removeLast();
        }

        @Override
        public void onUpdate(float deltaTime, Environment environment) {
            start = environment.getIndex(x, y);
            end = environment.getPlayerIndex();
            if (environment.isPlayerIndexChanged()) {
                reqNewPath = true;
            } else if (!path.isEmpty() && environment.getIndex(x, y) == targetIndex) {
                targetIndex = path.removeLast();
            }

            targetX = environment.index2X(targetIndex);
            targetY = environment.index2Y(targetIndex);

            //指向目标的向量
            float XComponentToTarget = targetX - x;
            float YComponentToTarget = targetY - y;

            //如果坐标与目标重合，则直接返回
            if(XComponentToTarget==0 && YComponentToTarget==0) return;

            //到目标的距离
            float distanceToTarget = (float) Math.sqrt
                    (XComponentToTarget * XComponentToTarget + YComponentToTarget * YComponentToTarget);

            //指向目标的单位向量
            float XUnitToTarget = XComponentToTarget / distanceToTarget;
            float YUnitToTarget = YComponentToTarget / distanceToTarget;

            //获取bot所处的区块
            Line[] blockOfLines = environment.getBlockOfLines(x, y);

            vx += XUnitToTarget * getV();
            vy += YUnitToTarget * getV();

            //clamp the velocity
            int min = -getV();
            int max = getV();
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            if (len < min) {
                vx = vx / len * min;
                vy = vy / len * min;
            } else if (len > max) {
                vx = vx / len * max;
                vy = vy / len * max;
            }

            oldX = x;
            oldY = y;

            //更新角色坐标
            x += vx * deltaTime;
            y += vy * deltaTime;

            //BOT与地图碰撞检测
            float[] output = new float[2];
            for (Line line : blockOfLines) {
                hitTest(output, x, y, r, line);
                x += output[0];
                y += output[1];
            }

            //旋转角
            float dx = x - oldX;
            float dy = y - oldY;
            float l = (float) Math.sqrt(dx * dx + dy * dy);
            float ux = dx / l;
            float uy = dy / l;
            rotation = (float) (90 + Math.acos((double) ux) * 180 / Math.PI * (uy < 0 ? -1 : 1));

            oldX = x;
            oldY = y;

        }

        @Override
        public void onExit() {

        }

        @Override
        public FSMTransition[] getTransitions() {
            return transitions;
        }
    }

    @Override
    protected void updateInner(float deltaTime, Environment environment) {
        FSM.update(deltaTime, environment);
    }

    @Override
    public void present(Graphics g, float offsetX, float offsetY) {
        g.drawPixmapDegree(Assets.assistHostile, this.x - offsetX, this.y - offsetY, rotation);
    }

    @Override
    protected int onDestroyed() {
        stage.accessScore(10);

        //被销毁时产生范围攻击
        GeneralCircleAttack atkObj = new GeneralCircleAttack(stage);
        atkObj.initialize(this, x, y, 0, 0, 0, 25 * getAtk(), 100);
        stage.addAtkHostile(atkObj);
        //产生特效
        MultSquareEffect effect = new MultSquareEffect();
        effect.initialize((int) x, (int) y, 0xFFFF0000, 10, 30, 1, 1, 1);
        stage.addEffect(effect);
        //随机产生能量包或生命包
        double ran = Math.random();
        if(ran>0.9) {
            EnergyBox energyBox = new EnergyBox(stage);
            energyBox.initialize(this, x, y);
            stage.addAtkHostile(energyBox);
            stage.addToTrackList(energyBox, 1);
        }
        else if(ran>0.8){
            LifeBox lifeBox = new LifeBox(stage);
            lifeBox.initialize(this, x, y);
            stage.addAtkHostile(lifeBox);
            stage.addToTrackList(lifeBox,1);
        }
        //向父级回调被销毁
        parent.onChildrenDestroyed(this);
        parent = null;
        if (defender() != null) defender().onDefendedDestroyed(this);

        return 0;
    }

    @Override
    public void onHitCharacter(Character character, AtkInfo attack) {

    }

    @Override
    public void onHitByCharacter(Character character, AtkInfo attack) {
        if (getHp() <= 0 && shouldAddBuff) {
            character.addBuff(0);
        }
        character.accessEnergy(attack.getEnergy() * 2);//返还2被能量
    }

    @Override
    public boolean canBeDefended() {
        return true;
    }

    @Override
    public void putWeights(double[] weights) {
        neuralNet.PutWeights(weights);
    }

    @Override
    public double[] getWeights() {
        return neuralNet.GetWeights();
    }

    @Override
    public float getFitness() {
        return fitness;
    }

    @Override
    public int getLiveTime() {
        return liveTime;
    }

    @Override
    public void onChildrenDestroyed(NPC child) {

    }

    private void active(float deltaTime, Environment environment) {
        start = environment.getIndex(x, y);
        //Log.e(getName(),"x/y = "+x+" / "+y);
        end = environment.getPlayerIndex();

        hostileList = environment.getHostileList();

        //指向玩家的向量
        float XComponentToPlayer = environment.getPlayerX() - this.x;
        float YComponentToPlayer = environment.getPlayerY() - this.y;

        //如果坐标与玩家重合，则随机一个值
        if (XComponentToPlayer == 0 && YComponentToPlayer == 0) {
            XComponentToPlayer = (float) Math.random() + 1;
            YComponentToPlayer = (float) Math.random() + 1;
        }

        //到玩家的距离
        float distanceToPlayer = (float) Math.sqrt
                (XComponentToPlayer * XComponentToPlayer + YComponentToPlayer * YComponentToPlayer);

        //指向玩家的单位向量
        final float XUnitToPlayer = XComponentToPlayer / distanceToPlayer;
        final float YUnitToPlayer = YComponentToPlayer / distanceToPlayer;

        //到攻击对象的距离（的平方）
        float distanceToAtk = distanceToPlayer * distanceToPlayer;

        float playerX = environment.getPlayerX();
        float playerY = environment.getPlayerY();
        float atkX = playerX;
        float atkY = playerY;

        //遍历攻击对象，获得最近的攻击对象的距离以及坐标
        //如果不存在攻击对象，则以玩家的距离和坐标代替
        ArrayList<Attack> playerAtkList = environment.playerAtkList;
        final int len = playerAtkList.size();
        //if(len>0) {
        for (int i = 0; i < len; i++) {
            Attack atk = playerAtkList.get(i);
            final float dxAtk2Player = playerX - atk.getX();
            final float dyAtk2Player = playerY - atk.getY();
            final float dx2Player = playerX - this.x;
            final float dy2Player = playerY - this.y;
            final float dx = this.x - atk.getX();
            final float dy = this.y - atk.getY();
            final float l = dx * dx + dy * dy;
            if (l < distanceToAtk && dxAtk2Player * dxAtk2Player + dyAtk2Player * dyAtk2Player < dx2Player * dx2Player + dy2Player * dy2Player) {
                distanceToAtk = l;
                atkX = atk.getX();
                atkY = atk.getY();
            }
        }

        //旋转角
        rotation = (float) (90 + Math.acos((double) XUnitToPlayer) * 180 / Math.PI * (YUnitToPlayer < 0 ? -1 : 1));

        //获取bot所处的区块
        Line[] blockOfLines = environment.getBlockOfLines(x, y);

        //按一定时间间隔攻击
        delayTimer += deltaTime;
        if (delayTimer >= atkDelay) {
            delayTimer -= atkDelay;
            //将AttackObject(Hostile)置于舞台
            BuffArea atkObj = new BuffArea(stage);
            atkObj.initialize(this, x, y, 300, Buff.ATK_UP_X);//TODO:NPC攻击的能量暂时为0
            stage.addAtkPlayer(atkObj);
        }

        Character near = null;
        Character c = hostileList.get(0);
        float minDistance = distancePoint2Point(this.x, this.y, c.getX(), c.getY());

        for (int i = 1; i < hostileList.size(); i++) {
            c = hostileList.get(i);

            if (!(c == this) && !c.getName().startsWith("NPCProducer") && !c.getName().startsWith("DefenceCharacter")) {
                float distance = distancePoint2Point(this.x, this.y, c.getX(), c.getY());

                if (distance < minDistance) {
                    minDistance = distance;
                    near = c;
                }
            }
        }

        GeneralLine lineToPlayer = GeneralLine.CreateLine(this.x, this.y, playerX, playerY);

        float offsetAtkToLine = lineToPlayer.offset(atkX, atkY);
        if (offsetAtkToLine > avoidSensitivity) offsetAtkToLine = avoidSensitivity;
        else if (offsetAtkToLine < -avoidSensitivity) offsetAtkToLine = -avoidSensitivity;

        if (atkX == playerX && atkY == playerY) offsetAtkToLine = 0;
        else if (offsetAtkToLine > 0) offsetAtkToLine = 1 - offsetAtkToLine / avoidSensitivity;
        else if (offsetAtkToLine < 0) offsetAtkToLine = -1 - offsetAtkToLine / avoidSensitivity;

        float offsetNearToLine = 0;
        if (near != null) offsetNearToLine = lineToPlayer.offset(near.getX(), near.getY());
        if (offsetNearToLine < -disperseSensitivity) offsetNearToLine = -disperseSensitivity;
        else if (offsetNearToLine > disperseSensitivity) offsetNearToLine = disperseSensitivity;

        if (offsetNearToLine < 0) offsetNearToLine = -1 - offsetNearToLine / disperseSensitivity;
        else if (offsetNearToLine > 0) offsetNearToLine = 1 - offsetNearToLine / disperseSensitivity;

        NetInputs[0] = (distanceToPlayer - minDistanceFromPlayer) / minDistanceFromPlayer;
        NetInputs[1] = offsetAtkToLine;
        NetInputs[2] = offsetNearToLine;

        for (int i = 0; i < NetInputs.length; i++) {
            if (NetInputs[i] < -1) NetInputs[i] = -1;
            if (NetInputs[i] > 1) NetInputs[i] = 1;
        }

        //从网络获取下一个行动
        char action = GetActionFromNetwork(NetInputs);

        if (action == 0) {
            vx -= vx * deltaTime * 10;
            vy -= vy * deltaTime * 10;
        } else {
            switch (action & 0xC) {
                case 4:
                    vx += -YUnitToPlayer * getV();
                    vy += XUnitToPlayer * getV();
                    break;
                case 8:
                    vx += YUnitToPlayer * getV();
                    vy += -XUnitToPlayer * getV();
                    break;
                default:
                    switch (action & 0x3) {
                        case 1:
                            vx += XUnitToPlayer * getV();
                            vy += YUnitToPlayer * getV();
                            break;
                        case 2:
                            vx += -XUnitToPlayer * getV();
                            vy += -YUnitToPlayer * getV();
                            break;
                    }
            }
        }

        int min = -getV();
        int max = getV();
        float length = (float) Math.sqrt(vx * vx + vy * vy);
        if (length < min) {
            vx = vx / length * min;
            vy = vy / length * min;
        } else if (length > max) {
            vx = vx / length * max;
            vy = vy / length * max;
        }

        //更新角色坐标
        this.x += vx * deltaTime;
        this.y += vy * deltaTime;

        //BOT与地图碰撞检测
        float[] output = new float[2];
        for (Line line : blockOfLines) {
            hitTest(output, x, y, r, line);
            x += output[0];
            y += output[1];
        }

        //TODO: 适应性函数
        liveTime++;
    }

    /**
     * 更新ANN并返回网络所选择的动作作为输出
     */
    private char GetActionFromNetwork(double[] netInputs) {
        //储存网络的输出
        double[] outputs;

        //      Log.e("DefenceCharacter", NetInputs[3]+ "_" + NetInputs[4]);

        //输入网络得到输出
        outputs = neuralNet.Update(NetInputs);

        //检查更新ANN时是否出现问题
        if (outputs == null) {
            Log.e("NPC", "神经网络输出错误！");
            return 0;
        }

        //determine which action is valid this frame. The highest valued
        //output over 0.9. If none are over 0.9 then just drift with
        //gravity
        double BiggestSoFar = 0;

        char action = 0;

        /*len = outputs.length;
        for (int i=0; i<len; ++i){
            if( (outputs[i] > BiggestSoFar) && (outputs[i] > 0.9)){
                action = (char) (i + 1);
                BiggestSoFar = outputs[i];
            }
        }*/

        if ((outputs[0] > BiggestSoFar) && (outputs[0] > 0.5)) {
            action = 1;
            BiggestSoFar = outputs[0];
        }
        if ((outputs[1] > BiggestSoFar) && (outputs[1] > 0.5)) {
            action = 2;
        }
        BiggestSoFar = 0;
        if ((outputs[2] > BiggestSoFar) && (outputs[2] > 0.5)) {
            action |= 4;
            BiggestSoFar = outputs[2];
        }
        if ((outputs[3] > BiggestSoFar) && (outputs[3] > 0.5)) {
            action &= 0xB;
            action |= 8;
        }
        return action;
    }

    /**
     * 圆-线段的碰撞检测
     *
     * @param output 输出
     * @param x      圆心X
     * @param y      圆心Y
     * @param r      半径
     * @param line   线段
     */
    private static void hitTest(float[] output, float x, float y, int r, Line line) {
        int x1 = line.getPoint1().x;
        int y1 = line.getPoint1().y;

        //P1到P2的向量b
        //b的长度
        float len = line.getLength();
        //单位化b
        float vx = line.getUnit()[0];
        float vy = line.getUnit()[1];
        //P1到圆心的向量a
        float ax = x - x1;
        float ay = y - y1;
        //计算a在b上的投影
        float u = ax * vx + ay * vy;
        //获得直线上离圆最近的点
        float x0, y0;
        if (u < 0) {
            x0 = x1;
            y0 = y1;
        } else if (u > len) {
            x0 = line.getPoint2().x;
            y0 = line.getPoint2().y;
        } else {
            x0 = x1 + vx * u;
            y0 = y1 + vy * u;
        }
        x0 -= x;
        y0 -= y;
        float d = (float) Math.sqrt(x0 * x0 + y0 * y0);

        if (d <= r) {
            output[0] = (-x0 / d * (r - d));
            output[1] = (-y0 / d * (r - d));
        } else {
            output[0] = 0;
            output[1] = 0;
        }
    }

    /**
     * 求直角坐标系中任意两点的距离
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private static float distancePoint2Point(float x1, float y1, float x2, float y2) {
        return (float)Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
}
