package com.tirso.tirsolador;

import android.view.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/** Sources remain independent: releasing HAT cannot release a held button or stick. */
final class GameInput {
    static final class Pad {int buttons,retroButtons,triggers,hat,stick,port;float lx,ly,rx,ry;boolean touch;}
    private final Map<Integer,Pad> pads=new HashMap<>();
    private Pad pad(int id){Pad p=pads.get(id);if(p==null){p=new Pad();boolean[] used=new boolean[4];for(Pad other:pads.values())used[other.port]=true;for(int i=0;i<4;i++)if(!used[i]){p.port=i;break;}pads.put(id,p);}return p;}
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
    static int retroBit(int key){switch(key){
        case KeyEvent.KEYCODE_BUTTON_A:case KeyEvent.KEYCODE_DPAD_CENTER:return 1;
        case KeyEvent.KEYCODE_BUTTON_B:return 1<<8;case KeyEvent.KEYCODE_BUTTON_X:return 1<<1;case KeyEvent.KEYCODE_BUTTON_Y:return 1<<9;
        case KeyEvent.KEYCODE_BUTTON_SELECT:return 1<<2;case KeyEvent.KEYCODE_BUTTON_START:return 1<<3;
        case KeyEvent.KEYCODE_DPAD_UP:return 1<<4;case KeyEvent.KEYCODE_DPAD_DOWN:return 1<<5;case KeyEvent.KEYCODE_DPAD_LEFT:return 1<<6;case KeyEvent.KEYCODE_DPAD_RIGHT:return 1<<7;
        case KeyEvent.KEYCODE_BUTTON_L1:return 1<<10;case KeyEvent.KEYCODE_BUTTON_R1:return 1<<11;case KeyEvent.KEYCODE_BUTTON_L2:return 1<<12;case KeyEvent.KEYCODE_BUTTON_R2:return 1<<13;
        case KeyEvent.KEYCODE_BUTTON_THUMBL:return 1<<14;case KeyEvent.KEYCODE_BUTTON_THUMBR:return 1<<15;default:return 0;
    }}
    synchronized boolean key(int id,int key,boolean down){int rb=retroBit(key),bit=bit(key);if(rb==0&&bit==0)return false;Pad p=pad(id);if(key==KeyEvent.KEYCODE_BUTTON_THUMBR)p.touch=down;if(down)p.retroButtons|=rb;else p.retroButtons&=~rb;if(down)p.buttons|=bit;else p.buttons&=~bit;return true;}
    static int directions(float x,float y,float flat){return (x>flat?16:0)|(x< -flat?32:0)|(y< -flat?64:0)|(y>flat?128:0);}
    synchronized void motion(int id,float hx,float hy,float sx,float sy,float deadzone){Pad p=pad(id);p.hat=directions(hx,hy,.5f);p.stick=directions(sx,sy,Math.max(.35f,deadzone));}
    synchronized void analog(int id,float x,float y,float rx,float ry,float deadzone){Pad p=pad(id);p.lx=Math.abs(x)>deadzone?x:0;p.ly=Math.abs(y)>deadzone?y:0;p.rx=Math.abs(rx)>deadzone?rx:0;p.ry=Math.abs(ry)>deadzone?ry:0;}
    synchronized void player(int id,int port){if(port>=0&&port<4)pad(id).port=port;}
    synchronized void triggers(int id,float left,float right){pad(id).triggers=(left>.5f?1<<12:0)|(right>.5f?1<<13:0);}
    synchronized float[] axes(){float[] values=new float[16];for(Pad p:pads.values()){int n=p.port*4;values[n]=p.lx;values[n+1]=p.ly;values[n+2]=p.rx;values[n+3]=p.ry;}return values;}
    synchronized int[] retroMasks(boolean analogConsole){int[] result=new int[4];for(Pad p:pads.values()){int d=p.hat|(analogConsole?0:p.stick);result[p.port]|=p.retroButtons|p.triggers|((d&16)!=0?1<<7:0)|((d&32)!=0?1<<6:0)|((d&64)!=0?1<<4:0)|((d&128)!=0?1<<5:0);}return result;}
    synchronized int mask(){int m=0;for(Pad p:pads.values())m|=p.buttons|p.hat|p.stick;return m;}
    synchronized boolean touching(){for(Pad p:pads.values())if(p.touch)return true;return false;}
    synchronized void remove(int id){pads.remove(id);}
    synchronized void clear(){pads.clear();}
}
