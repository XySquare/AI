package com.xyy.game.ai.Attack;

import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.support.annotation.NonNull;

import com.xyy.game.ai.*;
import com.xyy.game.ai.Character.Character;
import com.xyy.game.framework.Graphics;
import com.xyy.game.util.Line;

/**
 * 生命包，
 * 玩家碰到将恢复500点生命值
 * Created by ${XYY} on ${2016/9/7}.
 */
public class LifeBox extends Attack {
    private float timer;

    private float scaX =0;
    private float scaY =1;
    private float alpha = 0xFF;

    //坐标必须为(0,0),半径应与攻击半径一致
    private static RadialGradient radialGradient = new RadialGradient(0,0,25,0x7FFF0000,0x00FF0000, Shader.TileMode.CLAMP);

    public LifeBox(Stage stage) {
        super((char) 100, stage);
        timer = 0;
    }

    public void initialize(@NonNull Character parent,float x,float y){
        this.parent = parent;
        this.x = x;
        this.y = y;
        isDead = false;
    }

    @Override
    public boolean hitTestLine(Line line) {
        return false;
    }

    @Override
    public boolean hitTestCharacter(Character character) {
        if(isDead || timer>=10) return false;
        final int radius = 25;
        boolean res = ((x-character.getX())*(x-character.getX())
                +(y-character.getY())*(y-character.getY()))
                <= (radius+character.getR())*(radius+character.getR());
        if(res){
            character.accessHp(500);//恢复
            parent.onHitCharacter(character,this);//向父级回调命中对象
            parent = null;
            scaX=1;
            timer = 11;
        }
        return res;
    }

    @Override
    public void update(float deltaTime) {
        //持续10秒
        timer+=deltaTime;

        if(timer>11 && timer<11.2){
            scaX+=scaX*deltaTime*5;
            scaY+=scaY*deltaTime*5;
            alpha-=deltaTime*0xFF*5;
        }
        else if(timer<10){
            float timer1 = timer%1;
            scaX = (float)(4*(timer1-0.5)*(timer1-0.5));
        }
        else if(timer<10.2){
            scaX-=scaX*deltaTime*5;
            scaY-=scaY*deltaTime*5;
            alpha-=deltaTime*0xFF*5;
        }
        else{
            //parent.onHitCharacter(null,this);//向父级回调命中对象
            isDead = true;//等待被移除
        }

    }

    @Override
    public void present(Graphics g, float offsetX, float offsetY) {
        g.drawCircle(x-offsetX,y-offsetY,25,radialGradient,1);
        g.drawPixmapScale(Assets.aid,x - offsetX, y - offsetY, scaX,scaY,(int)alpha);
    }

    @Override
    public int getEnergy() {
        return 0;
    }//无需能量
}
