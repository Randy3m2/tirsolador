package com.tirso.tirsolador;

import android.view.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/** Sources remain independent: releasing HAT cannot release a held button or stick. */
final class GameInput {
    static final class Pad {int buttons,hat,stick;boolean touch;}
    private final Map<Integer,Pad> pads=new HashMap<>();
    private Pad pad(int id){Pad p=pads.get(id);if(p==null){p=new Pad();pads.put(id,p);}return p;}
    static int bit(int key){switch(key){
        case KeyEvent.KEYCODE_BUTTON_A:case KeyEvent.KEYCODE_DPAD_CENTER:return 1;
        case KeyEvent.KEYCODE_BUTTON_B:return 2;
        case KeyEvent.KEYCODE_BUTTON_SELECT:return 4;
        case KeyEvent.KEYCODE_BUTTON_START:return 8;
        case KeyEvent.KEYCODE_DPAD_RIGHT:return 16;
        case KeyEvent.KEYCODE_DPAD_LEFT:return 32;
        case KeyEvent.KEYCODE_DPAD_UP:return 64;
        case KeyEvent.KEYCODE_DPAD_DOWN:return 128;
        case KeyEvent.KEYCODE_BUTTON_R1:return 256;
        case KeyEvent.KEYCODE_BUTTON_L1:return 512;
        case KeyEvent.KEYCODE_BUTTON_X:return 1024;
        case KeyEvent.KEYCODE_BUTTON_Y:return 2048;
        default:return 0;
    }}
    synchronized boolean key(int id,int key,boolean down){Pad p=pad(id);if(key==KeyEvent.KEYCODE_BUTTON_THUMBR){p.touch=down;return true;}int bit=bit(key);if(bit==0)return false;if(down)p.buttons|=bit;else p.buttons&=~bit;return true;}
    static int directions(float x,float y,float flat){return (x>flat?16:0)|(x< -flat?32:0)|(y< -flat?64:0)|(y>flat?128:0);}
    synchronized void motion(int id,float hx,float hy,float sx,float sy,float deadzone){Pad p=pad(id);p.hat=directions(hx,hy,.5f);p.stick=directions(sx,sy,Math.max(.35f,deadzone));}
    synchronized int mask(){int m=0;for(Pad p:pads.values())m|=p.buttons|p.hat|p.stick;return m;}
    synchronized boolean touching(){for(Pad p:pads.values())if(p.touch)return true;return false;}
    synchronized void remove(int id){pads.remove(id);}
    synchronized void clear(){pads.clear();}
}
