package com.tirso.tirsolador;
final class RetroCore {
    static {System.loadLibrary("tirso_retro");}
    static native long open(String library,String rom,String saveDir,String systemDir,String core) throws java.io.IOException;
    static native int frame(long handle,int[] keys,float[] axes,int[] pixels,short[] audio) throws java.io.IOException;
    static native double[] info(long handle);
    static native byte[] snapshot(long handle) throws java.io.IOException;
    static native boolean restore(long handle,byte[] bytes) throws java.io.IOException;
    static native boolean flush(long handle);
    static native void close(long handle);
}
