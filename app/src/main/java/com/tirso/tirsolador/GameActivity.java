package com.tirso.tirsolador;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.hardware.input.InputManager;
import android.media.*;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import java.io.*;
import java.util.concurrent.*;

/** One worker owns the core, audio, save operations and ROM preparation. */
public class GameActivity extends Activity implements InputManager.InputDeviceListener {
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private final GameInput input=new GameInput();
    private ControllerProfiles profiles;
    private GameStorage storage;
    private Screen screen;
    private volatile boolean closed,foreground,ready,menu=true;
    private volatile int touchX=128,touchY=96;
    private volatile boolean pointerTouch,showFps;
    private long handle,nextFrame;
    private AudioTrack audio;
    private final int[] pixels=new int[256*384];
    private final short[] samples=new short[4096];
    private AlertDialog pauseDialog;
    private String title,description;
    private InputManager manager;
    private int frames;
    private long fpsTime;
    private volatile float fps;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        title=getIntent().getStringExtra("title");description=getIntent().getStringExtra("description");
        profiles=new ControllerProfiles(this);screen=new Screen();setContentView(screen);
        manager=(InputManager)getSystemService(INPUT_SERVICE);manager.registerInputDeviceListener(this,null);
        worker.execute(()->{
            try{
                String path=getIntent().getStringExtra("rom");if(path==null)throw new IOException("No se recibió la ROM");
                File source=new File(path).getCanonicalFile();
                String root=getFilesDir().getCanonicalPath()+File.separator;
                String cache=getCacheDir().getCanonicalPath()+File.separator;
                if(!source.getPath().startsWith(root)&&!source.getPath().startsWith(cache))throw new IOException("Archivo fuera del almacenamiento de Tirsolador");
                String filename=getIntent().getStringExtra("filename");
                storage=new GameStorage(this,source,filename==null||filename.isEmpty()?source.getName():filename);open();ready=true;menu=false;nextFrame=System.nanoTime();fpsTime=nextFrame;
                tick();
            }catch(Exception|LinkageError e){error(e,true);}
        });
    }
    private void open()throws Exception{
        handle=DsCore.open(storage.rom.getPath(),storage.battery().getPath());
        if(audio==null){
            int size=Math.max(48000,AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT));
            audio=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(size).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(audio.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("No se pudo iniciar el audio");
        }
    }
    private void tick(){
        if(closed)return;
        try{
            if(!menu&&foreground&&handle!=0){
                if(audio.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)audio.play();
                int count=DsCore.frame(handle,input.mask(),touchX,touchY,pointerTouch||input.touching(),pixels,samples);
                if(count<0)throw new IOException("La consola se detuvo");
                if(count>0)audio.write(samples,0,count,AudioTrack.WRITE_NON_BLOCKING);
                synchronized(screen.image){screen.image.setPixels(pixels,0,256,0,0,256,384);}
                screen.postInvalidate();
                long now=System.nanoTime();frames++;
                if(now-fpsTime>=1_000_000_000L){fps=frames*1_000_000_000f/(now-fpsTime);frames=0;fpsTime=now;}
                nextFrame+=16_715_113L;if(nextFrame<now-100_000_000L)nextFrame=now;
            }else{silence();nextFrame=System.nanoTime()+16_715_113L;}
        }catch(Exception e){menu=true;silence();error(e,false);}
        if(!closed)worker.schedule(this::tick,Math.max(1,nextFrame-System.nanoTime()),TimeUnit.NANOSECONDS);
    }
    private void silence(){if(audio!=null&&audio.getPlayState()==AudioTrack.PLAYSTATE_PLAYING){audio.pause();audio.flush();}}
    private void resetInput(){input.clear();pointerTouch=false;}
    @Override protected void onResume(){super.onResume();foreground=true;if(ready)pauseMenu();}
    @Override protected void onPause(){foreground=false;if(ready)menu=true;resetInput();super.onPause();}
    @Override protected void onDestroy(){
        closed=true;resetInput();manager.unregisterInputDeviceListener(this);
        worker.execute(()->{if(handle!=0){DsCore.close(handle);handle=0;}if(audio!=null){audio.pause();audio.flush();audio.release();audio=null;}});worker.shutdown();super.onDestroy();
    }
    private void error(Throwable e,boolean finish){runOnUiThread(()->{
        if(isFinishing()||isDestroyed())return;
        new AlertDialog.Builder(this).setTitle("Nintendo DS").setMessage(e.getMessage()==null?"No se pudo ejecutar el juego":e.getMessage())
            .setPositiveButton(finish?"Volver":"Menú",(d,w)->{if(finish)finish();else pauseMenu();}).setCancelable(false).show();
    });}
    private void task(Work work){worker.execute(()->{try{if(handle==0)throw new IOException("El juego todavía se está preparando");work.run();}catch(Exception e){error(e,false);}});}
    private interface Work{void run()throws Exception;}
    private void toast(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}
    @Override public void onBackPressed(){pauseMenu();}
    private void pauseMenu(){
        if(closed||isFinishing()||(pauseDialog!=null&&pauseDialog.isShowing()))return;
        menu=true;resetInput();
        String[] options={"Continuar","Ver información del juego",showFps?"Ocultar FPS":"Mostrar FPS","Guardar estado · Ranura 1","Cargar estado · Ranura 1","Respaldar partidas","Restaurar partidas","Reiniciar juego","Salir del juego"};
        pauseDialog=new AlertDialog.Builder(this).setTitle(title).setItems(options,(d,which)->{
            d.dismiss();pauseDialog=null;
            switch(which){
                case 0:menu=false;break;
                case 1:new AlertDialog.Builder(this).setTitle(title).setMessage((description==null?"":description)+"\n\nmelonDS integrado · 256 × 192 por pantalla\nResolución nativa · Audio estéreo\nStick derecho: cursor · R3: tocar\nStick izquierdo y cruceta: direcciones independientes\nEstado y partida: almacenamiento local")
                    .setPositiveButton("Menú",(x,w)->pauseMenu()).setOnCancelListener(x->pauseMenu()).show();break;
                case 2:showFps=!showFps;screen.invalidate();pauseMenu();break;
                case 3:task(()->{storage.saveState(DsCore.snapshot(handle));toast("Estado guardado");runOnUiThread(this::pauseMenu);});break;
                case 4:new AlertDialog.Builder(this).setTitle("¿Cargar el estado guardado?").setMessage("Se reemplazará el progreso actual en memoria.").setPositiveButton("Cargar",(x,w)->task(()->{
                    if(!DsCore.restore(handle,storage.loadState()))throw new IOException("No se pudo cargar el estado");resetInput();toast("Estado cargado");runOnUiThread(this::pauseMenu);
                })).setNegativeButton("Cancelar",(x,w)->pauseMenu()).setOnCancelListener(x->pauseMenu()).show();break;
                case 5:chooseBackup(false);break;
                case 6:new AlertDialog.Builder(this).setTitle("¿Restaurar un respaldo?").setMessage("Reemplazará las partidas incluidas en el respaldo y reiniciará el juego.").setPositiveButton("Elegir respaldo",(x,w)->chooseBackup(true)).setNegativeButton("Cancelar",(x,w)->pauseMenu()).setOnCancelListener(x->pauseMenu()).show();break;
                case 7:new AlertDialog.Builder(this).setTitle("¿Reiniciar juego?").setPositiveButton("Reiniciar",(x,w)->task(()->{DsCore.close(handle);handle=0;open();resetInput();menu=false;})).setNegativeButton("Cancelar",(x,w)->pauseMenu()).setOnCancelListener(x->pauseMenu()).show();break;
                case 8:finish();break;
                default:break;
            }
        }).setOnCancelListener(d->{menu=false;resetInput();}).create();
        pauseDialog.show();
    }
    private void chooseBackup(boolean restore){
        Intent i=new Intent(restore?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE);
        if(!restore)i.putExtra(Intent.EXTRA_TITLE,"Tirsolador-"+System.currentTimeMillis()+".zip");
        try{startActivityForResult(i,restore?22:21);}catch(android.content.ActivityNotFoundException e){error(new IOException("Esta TV no tiene selector de archivos. Instala un proveedor de documentos compatible para exportar o restaurar respaldos."),false);}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null){pauseMenu();return;}
        android.net.Uri uri=data.getData();
        if(request==21)task(()->{
            try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No se puede abrir el destino");storage.backup(out);}
            toast("Respaldo creado");runOnUiThread(this::pauseMenu);
        });
        else if(request==22)task(()->{
            DsCore.close(handle);handle=0;
            try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("No se puede abrir el respaldo");storage.restore(in);}
            finally{open();}
            toast("Respaldo restaurado. El juego se reinició; puedes cargar la ranura 1.");runOnUiThread(this::pauseMenu);
        });
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(menu)return super.dispatchKeyEvent(e);
        boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
        if(e.getKeyCode()==KeyEvent.KEYCODE_BACK||e.getKeyCode()==KeyEvent.KEYCODE_BUTTON_MODE||e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(down&&e.getRepeatCount()==0)pauseMenu();return true;}
        if(input.key(e.getDeviceId(),e.getKeyCode(),down))return true;
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean onGenericMotionEvent(MotionEvent e){
        if(!menu&&e.isFromSource(InputDevice.SOURCE_JOYSTICK)&&e.getDevice()!=null){
            profiles.motion(e);ControllerProfiles.State p=profiles.state(e.getDeviceId());
            input.motion(e.getDeviceId(),p.hatX,p.hatY,p.leftX,p.leftY,profiles.deadzone(e.getDevice()));
            if(Math.abs(p.rightX)>.15||Math.abs(p.rightY)>.15){touchX=Math.max(0,Math.min(255,Math.round(128+p.rightX*127)));touchY=Math.max(0,Math.min(191,Math.round(96+p.rightY*95)));}
            return true;
        }
        return super.onGenericMotionEvent(e);
    }
    public void onInputDeviceAdded(int id){}
    public void onInputDeviceChanged(int id){input.remove(id);profiles.disconnect(id);}
    public void onInputDeviceRemoved(int id){input.remove(id);profiles.disconnect(id);}
    final class Screen extends View {
        final Bitmap image=Bitmap.createBitmap(256,384,Bitmap.Config.ARGB_8888);
        final Paint paint=new Paint();final RectF top=new RectF(),bottom=new RectF();
        Screen(){super(GameActivity.this);setFocusable(true);setContentDescription("Pantallas de Nintendo DS");}
        @Override protected void onDraw(Canvas c){
            c.drawColor(Color.BLACK);float scale=Math.min((getWidth()-24)/512f,(getHeight()-32)/192f);float w=256*scale,h=192*scale;
            float x=(getWidth()-w*2-12)/2,y=(getHeight()-h)/2;top.set(x,y,x+w,y+h);bottom.set(x+w+12,y,x+w*2+12,y+h);
            synchronized(image){c.drawBitmap(image,new Rect(0,0,256,192),top,paint);c.drawBitmap(image,new Rect(0,192,256,384),bottom,paint);}
            paint.setColor(0xFF77D8FF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
            c.drawCircle(bottom.left+touchX/256f*bottom.width(),bottom.top+touchY/192f*bottom.height(),6,paint);paint.setStyle(Paint.Style.FILL);
            if(showFps){paint.setTextSize(22);c.drawText(String.format(java.util.Locale.ROOT,"%.1f FPS",fps),16,26,paint);}
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(menu)return false;
            if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){pointerTouch=false;performClick();return true;}
            if(bottom.contains(e.getX(),e.getY())){touchX=Math.min(255,(int)((e.getX()-bottom.left)*256/bottom.width()));touchY=Math.min(191,(int)((e.getY()-bottom.top)*192/bottom.height()));pointerTouch=true;return true;}
            pointerTouch=false;return false;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
