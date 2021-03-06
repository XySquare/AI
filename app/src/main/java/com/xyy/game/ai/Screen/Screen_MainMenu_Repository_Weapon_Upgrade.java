package com.xyy.game.ai.Screen;

import android.graphics.Paint;

import com.xyy.game.ai.Assets;
import com.xyy.game.ai.Screen.Screen_MainMenu_Repository.WeaponRecord;
import com.xyy.game.ai.Weapon.Weapon;
import com.xyy.game.framework.Game;
import com.xyy.game.framework.Graphics;
import com.xyy.game.framework.Input;
import com.xyy.game.framework.Pixmap;
import com.xyy.game.framework.Screen;

import java.util.List;

/**
 * Created by ${XYY} on ${2017/4/22}.
 */

public class Screen_MainMenu_Repository_Weapon_Upgrade extends Screen {
    private final WeaponRecord mItem;

    private boolean mIsLevelUp;
    private int mDeltaDamage;
    private int mDeltaEnergyCost;
    private int mDeltaRpm;

    private int pointer;
    private boolean pressed;

    private Pixmap list_item, list_item_highlight;

    public Screen_MainMenu_Repository_Weapon_Upgrade(Game game, WeaponRecord item) {
        super(game);

        Graphics g = game.getGraphics();

        list_item = g.newPixmap("list_item_weapon_detail.png", Graphics.PixmapFormat.ARGB8888);
        list_item_highlight = g.newPixmap("list_item_weapon_detail_highlight.png", Graphics.PixmapFormat.ARGB8888);

        pointer = -1;
        pressed = false;

        item.mWeapon.loadPixmap(game.getGraphics(), Weapon.PixmapQuality.NORMAL);
        mItem = item;
    }

    @Override
    public void update(float deltaTime) {
        List<Input.Touch> touchEvents = game.getInput().getTouchEvents();

        final int len = touchEvents.size();
        for (int i = 0; i < len; i++) {
            Input.Touch event = touchEvents.get(i);
            if (event.type == Input.Touch.TOUCH_DOWN) {
                if(inBounds(event, 900, 420, 189, 74)) {
                    pointer = event.pointer;
                    pressed = true;
                }
                else if(event.x <= 71 && event.y <= 68){
                    pointer = event.pointer;
                }
                //SETTING
                else if(inBounds(event,0,720-84,81,84)){
                    game.setScreen(new SettingScreen(game));
                }
                //STORE
                else if(inBounds(event,81,720-84,218,84)){
                    game.setScreen(new Screen_MainMenu_Store(game));
                }
                //REPOSITORY
                else if(inBounds(event,81+218,720-84,218,84)){
                    game.setScreen(new Screen_MainMenu_Repository(game));
                }
                //CAMPAIGN
                else if(event.x>81+4*218 && event.y>720-84){
                    game.setScreen(new MapsSelectingScreen(game));
                }
            }
            else if(event.type == Input.Touch.TOUCH_UP){
                if(pointer == event.pointer) {
                    pointer = -1;
                    pressed = false;
                    if(inBounds(event, 900, 420, 189, 74)){
                        /*if(UserDate.sCurrency >= mItem.mPrice) {
                            UserDate.sCurrency -= mItem.mPrice;
                            Screen_MainMenu_Store screen_mainMenu_store = new Screen_MainMenu_Store(game);
                            screen_mainMenu_store.setResult(mItem.mWeapon);
                            game.setScreen(screen_mainMenu_store);
                        }*/
                        if(mItem.mWeapon.getCurLv() < mItem.mWeapon.getMaxLv())
                        game.setScreen(new Screen_MainMenu_Repository_Weapon_Upgrade_Select_Material(game, mItem.mUUID));
                    }
                    else if(event.x <= 71 && event.y <= 68){
                        game.setScreen(new Screen_MainMenu_Repository(game));
                    }
                }
            }
        }
    }

    @Override
    public void present(float deltaTime) {
        Graphics g = game.getGraphics();

        g.drawPixmap(Assets.background,0,0);

        //顶部条
        g.drawPixmap(Assets.main_menu_top_bar,0,0);
        //返回按钮
        g.drawPixmap(Assets.back,22,20);
        //"MainMenu"文字
        g.drawText("STORE",77,28,0xFF999999,24);
        //"Store"文字
        g.drawText("[WEAPON UPGRADE]",77,53,0xFFFFFFFF,24);

        Weapon weapon = mItem.mWeapon;

        int offsetX = 200;

        if(mIsLevelUp) {
            g.drawPixmap(list_item, offsetX + 91, 82);
            g.drawText("LEVEL UP!",offsetX + 110+190,143,0xFF0ADB1B,42);
            g.drawPixmap(list_item_highlight, 91, 82);
        }
        else
            g.drawPixmap(list_item,91,82);
        g.drawText("LEVEL",110,123,0xFFFFFFFF,30);
        g.drawText(weapon.getCurLv()+"/"+weapon.getMaxLv(),110+220,143,0xFFFFFFFF,42);

        if(mDeltaDamage>0) {
            g.drawPixmap(list_item, offsetX + 91, 89 + 70 + 1);
            g.drawText('+'+String.valueOf(mDeltaDamage),offsetX + 110+190,221,0xFF0ADB1B,42);
            g.drawPixmap(list_item_highlight, 91, 89 + 70 + 1);
        }
        else if(mDeltaDamage<0){
            g.drawPixmap(list_item, offsetX + 91, 89 + 70 + 1);
            g.drawText(String.valueOf(mDeltaDamage),offsetX + 110+190,221,0xFFFB5809,42);
            g.drawPixmap(list_item_highlight, 91, 89 + 70 + 1);
        }
        else
            g.drawPixmap(list_item,91,89+70+1);
        g.drawText("DAMAGE",110,201,0xFFFFFFFF,30);
        g.drawText(String.valueOf(weapon.getDamage()),110+220,221,0xFFFFFFFF,42);

        if(mDeltaEnergyCost>0) {
            g.drawPixmap(list_item, offsetX + 91, 89+70+8+70+1);
            g.drawText('+'+String.valueOf(mDeltaEnergyCost),offsetX + 110+190,299,0xFFFB5809,42);
            g.drawPixmap(list_item_highlight, 91, 89+70+8+70+1);
        }
        else if(mDeltaEnergyCost<0){
            g.drawPixmap(list_item, offsetX + 91, 89+70+8+70+1);
            g.drawText(String.valueOf(mDeltaEnergyCost),offsetX + 110+190,299,0xFF0ADB1B,42);
            g.drawPixmap(list_item_highlight, 91, 89+70+8+70+1);
        }
        else
            g.drawPixmap(list_item,91,89+70+8+70+1);
        g.drawText("ENERGY COST",110,279,0xFFFFFFFF,30);
        g.drawText(String.valueOf(weapon.getEnergyCost()),110+220,299,0xFFFFFFFF,42);

        if(mDeltaRpm>0) {
            g.drawPixmap(list_item, offsetX + 91, 89+70+8+70+8+70+1);
            g.drawText('+'+String.valueOf(mDeltaRpm),offsetX + 110+190,353+24,0xFF0ADB1B,42);
            g.drawPixmap(list_item_highlight, 91, 89+70+8+70+8+70+1);
        }
        else if(mDeltaRpm<0){
            g.drawPixmap(list_item, offsetX + 91, 89+70+8+70+8+70+1);
            g.drawText(String.valueOf(mDeltaRpm),offsetX + 110+190,353+24,0xFFFB5809,42);
            g.drawPixmap(list_item_highlight, 91, 89+70+8+70+8+70+1);
        }
        else
            g.drawPixmap(list_item,91,89+70+8+70+8+70+1);
        g.drawText("RATE OF FIRE",110,357,0xFFFFFFFF,30);
        String num = String.valueOf((int)(60/weapon.getAtkDelay()));
        g.drawText(num,110+220,353+24,0xFFFFFFFF,42);
        g.drawText("RPM",110+220+num.length()*25,353+24,0xFFFFFFFF,28);

        g.drawRect(575, 410, 525, 210, 0x7F000000);

        if(weapon.getCurLv() == weapon.getMaxLv()) {
            g.drawPixmap(Assets.button_details, 900, 420, 378 ,0, 189, 74);
        }
        else if(pressed)
            g.drawPixmap(Assets.button_details, 900, 420, 189 ,0, 189, 74);
        else
            g.drawPixmap(Assets.button_details, 900, 420, 0 ,0, 189, 74);
        g.drawText("UPGRADE",900+95, 420+24+20, 0xFFFFFFFF, 24, Paint.Align.CENTER);

        if(weapon.getCurLv() < weapon.getMaxLv())
            g.drawText("EXP "+weapon.getCurExp()+"/"+weapon.getNextLvExpReq(),575+24, 420+24+20, 0xFFFFFFFF, 24);
        else
            g.drawText("LEVEL MAX ",575+24, 420+24+20, 0xFFFFFFFF, 24);

        g.drawPixmap(weapon.getPixmap(),630,200);
        String name = weapon.getName();
        g.drawText(name,1100,130,0xFFFFFFFF,35, Paint.Align.RIGHT);

        offsetX = 81;
        String[] bottom_bar_text = new String[]{"STORE","REPOSITORY","","",""};
        g.drawPixmap(Assets.main_menu_bottom_bar_button,-218+offsetX,720-84,218,0,218,84);
        g.drawPixmap(Assets.setting,18,720-84+20);
        for(int i=0;i<4;i++){
            g.drawPixmap(Assets.main_menu_bottom_bar_button,offsetX+i*218,720-84,218,0,218,84);
            g.drawText(bottom_bar_text[i],offsetX+i*218+109,720-84+36+19,0xFFFFFFFF,26, Paint.Align.CENTER);
        }
        g.drawPixmap(Assets.main_menu_bottom_bar_button_red,offsetX+4*218,720-84);
        g.drawText("CAMPAIGN",offsetX+4*218+164,720-84+36+19,0xFFFFFFFF,26, Paint.Align.CENTER);

        g.drawPixmap(Assets.main_menu_bottom_bar_button,offsetX+218,720-84,0,0,218,84);
        g.drawText(bottom_bar_text[1],offsetX+218+109,720-84+36+19,0xFFFFFFFF,26, Paint.Align.CENTER);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean onBack() {
        game.setScreen(new Screen_MainMenu_Store(game));
        return true;
    }

    private boolean inBounds(Input.Touch event, int x, int y, int width, int height) {
        return event.x > x && event.x < x + width - 1 &&
                event.y > y && event.y < y + height - 1;
    }

    public void setResult(boolean isLevelUp, int deltaDamage, int deltaEnergyCost, int deltaRpm) {
        mIsLevelUp = isLevelUp;
        mDeltaDamage = deltaDamage;
        mDeltaEnergyCost = deltaEnergyCost;
        mDeltaRpm = deltaRpm;
    }
}
