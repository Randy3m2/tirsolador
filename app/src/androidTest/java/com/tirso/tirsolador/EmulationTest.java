package com.tirso.tirsolador;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.view.KeyEvent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EmulationTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private File rom()throws Exception{
        File f=new File(context().getCacheDir(),"diagnostic.nds");
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("diagnostic.nds");OutputStream out=new FileOutputStream(f)){GameStorage.copy(in,out,1024*1024);}return f;
    }
    @Test public void nativeBootInputAudioAndStateRoundTrip()throws Exception{
        GameStorage store=new GameStorage(context(),rom());long h=DsCore.open(store.rom.getPath(),store.battery().getPath());
        try{
            int[]p=new int[256*384];short[]a=new short[4096];int samples=0;
            for(int i=0;i<5;i++)samples+=DsCore.frame(h,0,0,0,false,p,a);
            int red=p[128*256+128];assertTrue(((red>>16)&255)>240);assertEquals(0,red&65535);assertTrue(samples>0);
            byte[] state=DsCore.snapshot(h);store.saveState(state);
            for(int i=0;i<5;i++)DsCore.frame(h,1,0,0,false,p,a);
            int green=p[128*256+128];assertTrue(((green>>8)&255)>240);assertEquals(0,green&0xFF00FF);
            assertFalse(DsCore.restore(h,new byte[32]));assertTrue(DsCore.restore(h,store.loadState()));
            for(int i=0;i<3;i++)DsCore.frame(h,0,0,0,false,p,a);assertEquals(red,p[128*256+128]);
        }finally{DsCore.close(h);}
    }
    @Test public void backupRoundTripAndCorruptionRejected()throws Exception{
        GameStorage store=new GameStorage(context(),rom());byte[] save={1,3,7,9};GameStorage.write(store.battery(),save);
        ByteArrayOutputStream backup=new ByteArrayOutputStream();store.backup(backup);GameStorage.write(store.battery(),new byte[]{99});
        store.restore(new ByteArrayInputStream(backup.toByteArray()));assertArrayEquals(save,GameStorage.read(store.battery(),100));
        ByteArrayOutputStream bad=new ByteArrayOutputStream();
        try(ZipOutputStream out=new ZipOutputStream(bad)){out.putNextEntry(new ZipEntry("../battery.sav"));out.write(7);out.closeEntry();}
        try{store.restore(new ByteArrayInputStream(bad.toByteArray()));fail("Traversal accepted");}catch(IOException expected){assertArrayEquals(save,GameStorage.read(store.battery(),100));}
    }
    @Test public void stateEnvelopeRejectsChangedBytes()throws Exception{
        GameStorage store=new GameStorage(context(),rom());byte[] wrapped=store.wrapState(new byte[32]);wrapped[wrapped.length-1]=1;
        try{store.unwrapState(wrapped);fail("Damaged state accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("dañado"));}
    }
    @Test public void joystickDpadAndDeviceReleaseAreIndependent(){
        GameInput input=new GameInput();input.key(1,KeyEvent.KEYCODE_DPAD_RIGHT,true);
        input.motion(1,0,0,-1,0,.2f);assertEquals(16|32,input.mask());
        input.motion(1,0,0,0,0,.2f);assertEquals(16,input.mask());
        input.key(2,KeyEvent.KEYCODE_BUTTON_A,true);input.remove(1);assertEquals(1,input.mask());
        input.clear();assertEquals(0,input.mask());
    }
    @Test public void zipSingleRomAndAmbiguousZipRejected()throws Exception{
        File file=rom(),zip=new File(context().getCacheDir(),"diagnostic.zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){out.putNextEntry(new ZipEntry("nested/test.nds"));out.write(GameStorage.read(file,1024*1024));out.closeEntry();}
        GameStorage a=new GameStorage(context(),file),b=new GameStorage(context(),zip);assertEquals(a.digest,b.digest);
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){for(String name:new String[]{"one.nds","two.nds"}){out.putNextEntry(new ZipEntry(name));out.write(0);out.closeEntry();}}
        try{new GameStorage(context(),zip);fail("Ambiguous ZIP accepted");}catch(IOException expected){assertTrue(expected.getMessage().contains("varias"));}
    }
    @Test public void remoteBackOpensPausedMenu()throws Exception{
        Intent intent=new Intent(context(),GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("rom",rom().getPath()).putExtra("title","Tirsolador · Diagnóstico DS");
        Activity activity=InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        try{
            Thread.sleep(1800);InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.app.UiAutomation ui=InstrumentationRegistry.getInstrumentation().getUiAutomation();
            android.view.accessibility.AccessibilityNodeInfo root=ui.getRootInActiveWindow();assertNotNull(root);
            assertFalse(root.findAccessibilityNodeInfosByText("Guardar estado").isEmpty());
            try(android.os.ParcelFileDescriptor fd=ui.executeShellCommand("mkdir -p /sdcard/Download/tirsolador-screenshots && screencap -p /sdcard/Download/tirsolador-screenshots/06-ds-pause.png");InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        }finally{InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);}
    }
}
