package com.tirso.tirsolador;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConsoleExecutionTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private File fixture(String name)throws Exception{
        File target=new File(context().getCacheDir(),name);
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);OutputStream out=new FileOutputStream(target)){GameStorage.copy(in,out,16*1024*1024);}return target;
    }
    private void execute(Console console,String fixture,int frames)throws Exception{
        File file=fixture(fixture);GameStorage storage=new GameStorage(context(),file,fixture,console);
        File system=new File(context().getFilesDir(),"bios");system.mkdirs();
        RetroCore.diagnostics(console==Console.N64);
        long handle=RetroCore.open(CoreRegistry.library(context(),console).getPath(),storage.rom.getPath(),storage.dir.getPath(),system.getPath(),CoreRegistry.key(console));
        try{
            int[] pixels=new int[1024*1024],keys=new int[4];short[] audio=new short[16384];float[] axes=new float[16];int samples=0;
            for(int i=0;i<frames;i++)samples+=RetroCore.frame(handle,keys,axes,pixels,audio);
            double[] info=RetroCore.info(handle);int width=(int)info[0],height=(int)info[1];
            assertTrue(console+" geometry",width>0&&height>0&&width<=1024&&height<=1024);
            assertEquals(console+" must render a real frame",0xFF000000,pixels[(height/2)*width+width/2]&0xFF000000);
            boolean visible=false;Set<Integer> colors=new HashSet<>();for(int i=0;i<width*height;i+=7){colors.add(pixels[i]);if((pixels[i]&0xFFFFFF)!=0)visible=true;}
            assertTrue(console+" must execute the ROM and draw visible pixels",visible);
            if(console==Console.N64||console==Console.MAME)assertTrue(console+" diagnostic must render its UI",colors.size()>1);
            assertTrue(console+" audio output",samples>0);
            byte[] state=RetroCore.snapshot(handle);assertTrue(state.length>0);storage.saveState(state);
            keys[0]=(1<<8)|(1<<2)|(1<<3);for(int i=0;i<6;i++)RetroCore.frame(handle,keys,axes,pixels,audio);
            assertTrue(console+" state restore",RetroCore.restore(handle,storage.loadState()));assertTrue(RetroCore.flush(handle));
            ByteArrayOutputStream backup=new ByteArrayOutputStream();storage.backup(backup);assertTrue(backup.size()>0);
            android.util.Log.i("TirsoValidation",console+": PASS "+width+"x"+height+"; audio="+samples+"; state="+state.length);
        }finally{RetroCore.close(handle);RetroCore.diagnostics(false);storage.cleanup();}
    }
    @Test public void nesBootVideoAudioState()throws Exception{execute(Console.NES,"diagnostic.nes",120);}
    @Test public void snesBootVideoAudioState()throws Exception{execute(Console.SNES,"diagnostic.sfc",120);}
    @Test public void megaDriveBootVideoAudioState()throws Exception{execute(Console.MD,"diagnostic.md",150);}
    @Test public void gameBoyBootVideoAudioState()throws Exception{execute(Console.GB,"diagnostic.gb",120);}
    @Test public void gameBoyColorBootVideoAudioState()throws Exception{execute(Console.GBC,"diagnostic.gbc",120);}
    @Test public void gameBoyAdvanceBootVideoAudioState()throws Exception{execute(Console.GBA,"diagnostic.gba",120);}
    @Test public void n64BootHardwareVideoAudioState()throws Exception{execute(Console.N64,"diagnostic-cached.z64",120);}
    @Test public void n64DynarecAndHardwareFramebuffer()throws Exception{execute(Console.N64,"diagnostic.z64",120);}
    @Test public void mameBootVideoAudioState()throws Exception{execute(Console.MAME,"gridlee.zip",240);}
    @Test public void n64ReceivesFourIndependentControllerPorts()throws Exception{
        File file=fixture("diagnostic.z64");GameStorage store=new GameStorage(context(),file,file.getName(),Console.N64);
        File system=new File(context().getFilesDir(),"bios");system.mkdirs();
        long h=RetroCore.open(CoreRegistry.library(context(),Console.N64).getPath(),store.rom.getPath(),store.dir.getPath(),system.getPath(),"n64");
        try{
            int[] keys={1,1<<1,1<<3,1<<11},pixels=new int[1024*1024];float[] axes=new float[16];axes[0]=1;short[] audio=new short[16384];
            for(int i=0;i<30;i++)RetroCore.frame(h,keys,axes,pixels,audio);
            int[] expected={0x8000,0x4000,0x1000,0x10};
            for(int i=0;i<4;i++)assertEquals("N64 PIF controller "+(i+1),expected[i],(RetroCore.systemWordForTest(h,0x310000+i*4)>>>16)&65535);
            assertTrue("Player 1 analog stick",(RetroCore.systemWordForTest(h,0x310000)&0xFF00)!=0);
            assertEquals("Player 2 stick independent",0,RetroCore.systemWordForTest(h,0x310004)&65535);
            keys[0]=0;axes[0]=0;for(int i=0;i<8;i++)RetroCore.frame(h,keys,axes,pixels,audio);
            assertEquals(0,RetroCore.systemWordForTest(h,0x310000));assertEquals(0x4000,RetroCore.systemWordForTest(h,0x310004)>>>16);
        }finally{RetroCore.close(h);store.cleanup();}
    }
    private void reopenSave(Console console,String name)throws Exception{
        File file=fixture(name);GameStorage store=new GameStorage(context(),file,name,console);
        File system=new File(context().getFilesDir(),"bios");system.mkdirs();
        long h=0;
        try{
            h=RetroCore.open(CoreRegistry.library(context(),console).getPath(),store.rom.getPath(),store.dir.getPath(),system.getPath(),CoreRegistry.key(console));
            int[] pixels=new int[1024*1024],keys=new int[4];float[] axes=new float[16];short[] audio=new short[16384];
            for(int i=0;i<6;i++)RetroCore.frame(h,keys,axes,pixels,audio);
            byte[] ram=RetroCore.saveMemoryForTest(h,91);assertEquals(91,ram[0]&255);assertTrue(RetroCore.flush(h));
            store.saveState(RetroCore.snapshot(h));
            RetroCore.close(h);h=0;
            h=RetroCore.open(CoreRegistry.library(context(),console).getPath(),store.rom.getPath(),store.dir.getPath(),system.getPath(),CoreRegistry.key(console));
            assertEquals("Battery must load on a new native session",91,RetroCore.saveMemoryForTest(h,-1)[0]&255);
            assertTrue("Manual state must restore immediately after cold open",RetroCore.restore(h,store.loadState()));
            RetroCore.frame(h,keys,axes,pixels,audio);assertEquals("Restored game must retain saved SRAM",91,RetroCore.saveMemoryForTest(h,-1)[0]&255);
        }finally{if(h!=0)RetroCore.close(h);store.cleanup();}
    }
    @Test public void n64SramAndStateSurviveClosingAndReopening()throws Exception{reopenSave(Console.N64,"diagnostic.z64");}
    @Test public void gbaSramAndStateSurviveClosingAndReopening()throws Exception{reopenSave(Console.GBA,"diagnostic.gba");}
    @Test public void gbaAutodetectedSmallerSaveIsNotDiscarded()throws Exception{
        File file=fixture("diagnostic.gba");GameStorage store=new GameStorage(context(),file,file.getName(),Console.GBA);
        byte[] old=new byte[32768];old[0]=73;GameStorage.write(store.battery(),old);
        File system=new File(context().getFilesDir(),"bios");system.mkdirs();
        long h=RetroCore.open(CoreRegistry.library(context(),Console.GBA).getPath(),store.rom.getPath(),store.dir.getPath(),system.getPath(),"gba");
        try{assertEquals(73,RetroCore.saveMemoryForTest(h,-1)[0]&255);}finally{RetroCore.close(h);store.cleanup();}
    }
    @Test public void playerAssignmentsSurvivePauseAndResume(){
        GameInput p=new GameInput();p.player(10,-1);p.player(20,-1);p.key(20,android.view.KeyEvent.KEYCODE_BUTTON_A,true);
        assertEquals(0,p.retroMasks(true)[0]);assertEquals(1,p.retroMasks(true)[1]);p.clear();
        p.key(20,android.view.KeyEvent.KEYCODE_BUTTON_A,true);assertEquals(0,p.retroMasks(true)[0]);assertEquals(1,p.retroMasks(true)[1]);
        p.key(10,android.view.KeyEvent.KEYCODE_BUTTON_B,true);assertEquals(1<<8,p.retroMasks(true)[0]);
    }
    @Test public void gbaAndOtherGroupsCannotCross(){
        assertEquals(Console.GBA,TelegramCatalog.consoleFor("DD-ROMS-GBA"));
        assertEquals(Console.GBA,TelegramCatalog.consoleFor("TD - ROMs - Game Boy Advance"));
        assertEquals(Console.GBC,TelegramCatalog.consoleFor("TD - ROMs - GBC"));
        assertEquals(Console.MAME,TelegramCatalog.consoleFor("DD-ROMS-MAME"));
        assertNull(TelegramCatalog.consoleFor("TD - ROMs - GameCube"));
        for(Console c:Console.values())assertEquals(c,TelegramCatalog.consoleFor(TelegramCatalog.expectedTitle(c)));
    }
    @Test public void cleanupDeletesOnlyTemporaryRomAndPreservesSaves()throws Exception{
        Context c=context();File file=fixture("diagnostic.gba");GameStorage storage=new GameStorage(c,file,file.getName(),Console.GBA);
        byte[] saved={4,5,6};GameStorage.write(storage.battery(),saved);String lease=TemporaryRoms.begin(c,file);
        File workspace=storage.workspace;storage.cleanup();TemporaryRoms.finish(c,lease,file);
        assertFalse(file.exists());assertFalse(workspace.exists());assertArrayEquals(saved,GameStorage.read(storage.battery(),100));
        assertTrue(TemporaryRoms.removeSource(c,storage.battery()));assertTrue(storage.battery().exists());
    }
    @Test public void controllerPortsSticksAndDpadRemainIndependent(){
        GameInput p=new GameInput();p.player(10,0);p.player(20,1);p.key(10,android.view.KeyEvent.KEYCODE_BUTTON_A,true);p.key(20,android.view.KeyEvent.KEYCODE_DPAD_LEFT,true);
        p.motion(10,0,0,1,0,.2f);p.analog(10,1,0,-1,0,.2f);int[] keys=p.retroMasks(true);assertEquals(1,keys[0]);assertEquals(1<<6,keys[1]);
        assertEquals(1,p.axes()[0],0);assertEquals(-1,p.axes()[2],0);assertEquals(0,p.axes()[4],0);
        p.motion(10,0,0,0,0,.2f);assertEquals(1,p.retroMasks(false)[0]);p.remove(20);assertEquals(0,p.retroMasks(true)[1]);
    }
    @Test public void sevenZipExtractsARealRom()throws Exception{
        File source=fixture("diagnostic.gba"),archive=new File(context().getCacheDir(),"diagnostic.7z");
        try(org.apache.commons.compress.archivers.sevenz.SevenZOutputFile out=new org.apache.commons.compress.archivers.sevenz.SevenZOutputFile(archive)){
            org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry=out.createArchiveEntry(source,"game.gba");out.putArchiveEntry(entry);out.write(GameStorage.read(source,1024*1024));out.closeArchiveEntry();
        }
        GameStorage storage=new GameStorage(context(),archive,archive.getName(),Console.GBA);assertEquals(GameStorage.hash(source),storage.digest);storage.cleanup();
    }
}
