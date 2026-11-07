package com.tirso.tirsolador;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Per-device input state. HAT/D-pad, left stick and right stick never share axes. */
final class ControllerProfiles {
    private final SharedPreferences prefs;
    private final Map<Integer, State> states = new HashMap<>();
    static final class State {
        float leftX, leftY, rightX, rightY;
        int hatX, hatY, keyX, keyY;
        long lastStickNavigation, lastHatNavigation, lastHatTime;
        int lastHatDirection;
        boolean stickNavigating, hatNavigating;
    }
    ControllerProfiles(Context context) { prefs=context.getSharedPreferences("controller_profiles",Context.MODE_PRIVATE); }
    static String key(InputDevice d) {
        return d.getVendorId()+":"+d.getProductId()+":"+d.getDescriptor();
    }
    static String model(InputDevice d) {
        int vendor=d.getVendorId(), product=d.getProductId();
        String name=d.getName().toLowerCase(Locale.ROOT);
        if(vendor==0x054c && product==0x0df2 || name.contains("dualsense edge"))return "DualSense Edge";
        if(vendor==0x054c && product==0x0ce6 || name.contains("dualsense"))return "DualSense";
        if(vendor==0x054c && (product==0x05c4||product==0x09cc) || name.contains("dualshock"))return "DualShock 4";
        if(name.contains("8bitdo") && name.contains("2c"))return "8BitDo Ultimate 2C";
        if(vendor==0x057e && product==0x2009 || name.contains("pro controller"))return "Nintendo Switch Pro";
        return d.getName();
    }
    float deadzone(InputDevice d) { return prefs.getFloat(key(d)+":left_deadzone",.20f); }
    void deadzone(InputDevice d,float value) { prefs.edit().putFloat(key(d)+":left_deadzone",value).apply(); }
    State state(int id) { State s=states.get(id);if(s==null){s=new State();states.put(id,s);}return s; }
    void disconnect(int id) { states.remove(id); }
    static int direction(float x,float y,float threshold) {
        if(Math.max(Math.abs(x),Math.abs(y))<threshold)return 0;
        if(Math.abs(x)>Math.abs(y))return x>0?View.FOCUS_RIGHT:View.FOCUS_LEFT;
        return y>0?View.FOCUS_DOWN:View.FOCUS_UP;
    }
    static int hatDirection(State s) { return direction(s.hatX,s.hatY,.5f); }
    private float axis(MotionEvent event, InputDevice d,int axis) {
        InputDevice.MotionRange range=d.getMotionRange(axis,event.getSource());
        if(range==null)return 0;
        float value=event.getAxisValue(axis);
        return Math.abs(value)<=range.getFlat()?0:value;
    }
    int motion(MotionEvent event) {
        InputDevice device=event.getDevice();if(device==null)return 0;
        State s=state(event.getDeviceId());
        s.leftX=axis(event,device,MotionEvent.AXIS_X);s.leftY=axis(event,device,MotionEvent.AXIS_Y);
        int rightX=device.getMotionRange(MotionEvent.AXIS_Z,event.getSource())==null?MotionEvent.AXIS_RX:MotionEvent.AXIS_Z;
        int rightY=device.getMotionRange(MotionEvent.AXIS_RZ,event.getSource())==null?MotionEvent.AXIS_RY:MotionEvent.AXIS_RZ;
        s.rightX=axis(event,device,rightX);s.rightY=axis(event,device,rightY);
        s.hatX=Math.round(axis(event,device,MotionEvent.AXIS_HAT_X));s.hatY=Math.round(axis(event,device,MotionEvent.AXIS_HAT_Y));
        return navigate(s,event.getEventTime(),deadzone(device));
    }
    static int navigate(State s,long time,float deadzone) {
        int hat=hatDirection(s);
        int stick=direction(s.leftX,s.leftY,Math.max(.35f,deadzone));
        if(stick==0)s.stickNavigating=false;
        if(hat==0)s.hatNavigating=false;
        if(hat!=0){
            s.lastHatDirection=hat;s.lastHatTime=time;
            if(s.hatNavigating && time-s.lastHatNavigation<210)return 0;
            s.hatNavigating=true;s.lastHatNavigation=time;return hat;
        }
        if(stick==0)return 0;
        if(s.stickNavigating && time-s.lastStickNavigation<210)return 0;
        s.stickNavigating=true;s.lastStickNavigation=time;return stick;
    }
    boolean key(KeyEvent e,boolean down) {
        State s=state(e.getDeviceId());
        int direction=0;
        switch(e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:s.keyX=down?-1:(s.keyX==-1?0:s.keyX);direction=View.FOCUS_LEFT;break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:s.keyX=down?1:(s.keyX==1?0:s.keyX);direction=View.FOCUS_RIGHT;break;
            case KeyEvent.KEYCODE_DPAD_UP:s.keyY=down?-1:(s.keyY==-1?0:s.keyY);direction=View.FOCUS_UP;break;
            case KeyEvent.KEYCODE_DPAD_DOWN:s.keyY=down?1:(s.keyY==1?0:s.keyY);direction=View.FOCUS_DOWN;break;
        }
        // Suppress only the duplicate of a just-handled HAT event, never a stick event.
        return down && direction!=0 && s.lastHatTime>0 && direction==s.lastHatDirection
                && e.getEventTime()-s.lastHatTime>=0 && e.getEventTime()-s.lastHatTime<90;
    }
}
