package com.tirso.tirsolador;

/** All calls for a handle belong to its one emulation worker. */
final class DsCore {
    static { System.loadLibrary("tirso_ds"); }
    static final String REVISION="497aa6cdd26f45f8064b78916b00a773eab4ded7";
    static native long open(String rom,String save) throws java.io.IOException;
    static native int frame(long handle,int keys,int x,int y,boolean touch,int[] pixels,short[] audio) throws java.io.IOException;
    static native byte[] snapshot(long handle) throws java.io.IOException;
    static native boolean restore(long handle,byte[] bytes) throws java.io.IOException;
    static native void close(long handle);
    static native boolean flush(long handle);
}
