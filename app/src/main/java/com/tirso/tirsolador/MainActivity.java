package com.tirso.tirsolador;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends Activity {
    private Library library;
    private Artwork artwork;
    private LinearLayout root, body, content;
    private ControllerProfiles controllers;
    private android.hardware.input.InputManager inputManager;
    private android.hardware.input.InputManager.InputDeviceListener inputListener;
    private ScrollView activeScroll;
    private int libraryScroll;
    private String libraryFocus = "";
    private TirsoDrive drive;
    private TirsoDrive.Listener driveListener;
    private TextView downloadLabel,downloadButton;
    private int visibleGames=60;
    private android.graphics.Bitmap qrBitmap;
    private String renderedQr="";
    private String lastDriveError="";
    private int text, muted, surface, background, accent;
    private String page = "home", query = "";
    private Console console;
    private Game selected;
    private boolean favorites;
    private String lastFocus = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        library = new Library(this); artwork = new Artwork(this); controllers = new ControllerProfiles(this);
        drive=TirsoDrive.get(this);
        inputManager=(android.hardware.input.InputManager)getSystemService(INPUT_SERVICE);
        inputListener=new android.hardware.input.InputManager.InputDeviceListener(){
            public void onInputDeviceAdded(int id) { }
            public void onInputDeviceChanged(int id) { controllers.disconnect(id); }
            public void onInputDeviceRemoved(int id) { controllers.disconnect(id); }
        };
        inputManager.registerInputDeviceListener(inputListener,null);
        if (state != null) {
            page = state.getString("page", "home"); query = state.getString("query", "");
            favorites = state.getBoolean("favorites"); lastFocus = state.getString("focus", "");
            libraryFocus=state.getString("libraryFocus","");libraryScroll=state.getInt("libraryScroll",0);
            try { console = Console.valueOf(state.getString("console", "")); } catch (IllegalArgumentException ignored) { }
            for (Game g : library.games) if (g.id.equals(state.getString("game"))) selected = g;
        }
        if(!drive.onboarded()&&!drive.skipped())page="welcome";
        render();
        driveListener=catalog->{
            if(catalog){rememberLibrary();library=new Library(this);}
            if(!drive.skipped()&&(drive.stage==TirsoDrive.Stage.QR||drive.stage==TirsoDrive.Stage.PASSWORD||drive.stage==TirsoDrive.Stage.CODE))page="welcome";
            if(page.equals("welcome")||catalog)render();
            else if(page.equals("detail"))updateDownload();
            else if(!drive.error.isEmpty()&&!drive.error.equals(lastDriveError))Toast.makeText(this,drive.error,Toast.LENGTH_LONG).show();
            lastDriveError=drive.error;
        };
        drive.attach(driveListener);drive.start();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("page", page); state.putString("query", query); state.putBoolean("favorites", favorites);
        state.putString("console", console == null ? "" : console.name());
        state.putString("game", selected == null ? "" : selected.id); state.putString("focus", lastFocus);
        rememberLibrary();state.putString("libraryFocus",libraryFocus);state.putInt("libraryScroll",libraryScroll);
    }
    int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setPadding(0, dp(3), 0, dp(3)); return t;
    }
    private GradientDrawable panel(int color, boolean focused) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(12));
        g.setStroke(dp(focused ? 3 : 1), focused ? accent : library.dark() ? 0xFF293751 : 0xFFD6DEE7); return g;
    }
    private void focusable(View v, String key, int color, Runnable click) {
        v.setTag(key); v.setFocusable(true); v.setClickable(true); v.setBackground(panel(color, false));
        v.setOnClickListener(x -> click.run());
        v.setOnFocusChangeListener((x, has) -> {
            x.setBackground(panel(color, has));
            if (has) lastFocus = key;
            x.setElevation(dp(has ? 5 : 0));
        });
    }
    private TextView button(String title, String key, Runnable click) {
        TextView t = label(title, 14, text, true); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(10), dp(16), dp(10)); focusable(t, key, surface, click);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(44)); p.setMargins(dp(4), dp(4), dp(4), dp(4));
        t.setLayoutParams(p); return t;
    }
    private View icon(String name,String label,String key,Runnable click) {
        IconView v=new IconView(this,name,label,text);focusable(v,key,surface,click);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(44),dp(44));p.setMargins(dp(4),0,dp(4),0);
        v.setLayoutParams(p);return v;
    }
    private void rememberLibrary() {
        if(page.equals("library")) {
            libraryScroll=activeScroll==null?0:activeScroll.getScrollY();
            if(lastFocus.startsWith("game_"))libraryFocus=lastFocus;
        }
    }
    private void route(String target) { rememberLibrary();page = target; lastFocus = ""; render(); }
    private void backToLibrary() { page="library";lastFocus=libraryFocus;render(); }
    private void render() {
        boolean dark = library.dark();
        background = dark ? 0xFF0B1224 : 0xFFF5F7FA; surface = dark ? 0xFF162239 : Color.WHITE;
        text = dark ? 0xFFF4F7FC : 0xFF142238; muted = dark ? 0xFFABBCD1 : 0xFF52657A;
        accent = dark ? 0xFF14DADE : 0xFF007E89;
        if (library.theme().equals("Nintendo rojo")) accent = dark ? 0xFFFF6574 : 0xFFBF243B;
        if (library.theme().equals("Retro naranja")) accent = dark ? 0xFFFFAD57 : 0xFFAA5700;
        if (library.theme().equals("GBA clásico")) accent = dark ? 0xFFBFA1FF : 0xFF7042A3;
        root = vertical(); root.setPadding(dp(24), dp(12), dp(24), dp(16)); root.setBackgroundColor(background);
        setContentView(root);
        content=null;activeScroll=null;
        LinearLayout header = horizontal();
        if(!page.equals("home")) header.addView(icon("back","Volver","nav_back",this::onBackPressed));
        TextView title=label(page.equals("welcome")?"TirsoDrive":page.equals("library")?(favorites?"Favoritos":console==null?"Biblioteca":console.title):"",18,text,true);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(page.equals("library")) {
            header.addView(icon("search","Buscar juegos","search",this::search));
            header.addView(icon("refresh","Refrescar catálogo","refresh",this::refreshCatalog));
            header.addView(icon("sort","Ordenar juegos: "+(library.descending()?"Z a A":"A a Z"),"sort",this::sortGames));
        }
        if(!page.equals("welcome"))header.addView(icon("star","Favoritos","nav_favorites", () -> {
            console = null; favorites = true; query = ""; libraryFocus="";libraryScroll=0;route("library");
        }));
        if(!page.equals("welcome"))header.addView(icon("settings","Ajustes","nav_settings", () -> route("settings")));
        else header.addView(button(library.dark()?"Modo claro":"Modo oscuro","welcome_color",()->{library.dark(!library.dark());render();}));
        root.addView(header);
        body = vertical(); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        if(page.equals("welcome"))welcome();
        else if (page.equals("settings")) settings();
        else if (page.equals("detail") && selected != null) detail();
        else if (page.equals("library")) libraryScreen();
        else home();
        root.post(() -> {
            View target = lastFocus.isEmpty() ? null : root.findViewWithTag(lastFocus);
            if (target == null) target = firstContentFocusable(content == null ? body : content);
            if (target != null) target.requestFocus();
            if(page.equals("library")&&activeScroll!=null)activeScroll.scrollTo(0,libraryScroll);
        });
    }
    private View firstContentFocusable(View v) {
        if (v.isFocusable()) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
            View f = firstContentFocusable(((ViewGroup) v).getChildAt(i)); if (f != null) return f;
        }
        return null;
    }
    private void connectScreen(){page="welcome";lastFocus="";drive.enterConnection();render();}
    private void welcome(){
        heading(drive.stage==TirsoDrive.Stage.READY?"Tu biblioteca, conectada":"Tu biblioteca retro, conectada",drive.status);
        if(!drive.error.isEmpty())body.addView(label(drive.error,13,library.dark()?0xFFFFADB5:0xFFB0263D,false));
        if(drive.stage==TirsoDrive.Stage.READY){
            content=scrollContent();
            for(Console c:Console.values()){
                TirsoDrive.Source source=drive.sources.get(c);
                content.addView(button(c.title+"  ·  "+(source.chatId==0?source.issue:source.title),"source_"+c.name(),()->chooseSource(c)));
            }
            if(drive.discovering||drive.syncing)content.addView(button("Cancelar actualización","cancel_sync",drive::cancelSync));
            else{
                content.addView(button("Buscar grupos","discover_sources",drive::discover));
                content.addView(button("Actualizar catálogo","sync_sources",drive::sync));
                content.addView(button("Ir a mi biblioteca","open_connected_library",()->{drive.finishOnboarding();library=new Library(this);route("home");}));
            }
            content.addView(button("Cerrar sesión","logout",()->new AlertDialog.Builder(this).setTitle("Cerrar sesión de Telegram")
                .setMessage("Se quitará la biblioteca sincronizada de esta cuenta en la TV. Podrás volver a conectar.")
                .setPositiveButton("Cerrar sesión",(d,w)->drive.logout()).setNegativeButton("Cancelar",null).show()));return;
        }
        content=scrollContent();
        LinearLayout columns=horizontal();columns.setGravity(Gravity.TOP);
        LinearLayout instructions=vertical();instructions.setPadding(0,dp(10),dp(20),0);
        instructions.addView(label("1  Conectar    2  Encontrar consolas    3  Biblioteca",12,accent,true));
        instructions.addView(label("Abre Telegram en tu teléfono y entra en Ajustes → Dispositivos → Vincular dispositivo.",17,text,false));
        instructions.addView(label("Después buscaremos tus grupos TD - ROMs, incluidos los archivados, y asignaremos cada uno a su consola.",14,muted,false));
        if(drive.stage==TirsoDrive.Stage.CONFIG){
            instructions.addView(button("Configurar TirsoDrive","configure_credentials",this::configureCredentials));
            instructions.addView(label("Configuración inicial de la aplicación. Tu sesión de Telegram se autoriza después con el QR.",12,muted,false));
        }else if(drive.stage==TirsoDrive.Stage.PASSWORD)instructions.addView(button("Introducir contraseña","auth_password",()->authInput(true)));
        else if(drive.stage==TirsoDrive.Stage.CODE)instructions.addView(button("Introducir código","auth_code",()->authInput(false)));
        else instructions.addView(button("Reintentar conexión","retry_connection",drive::retry));
        if(!drive.error.isEmpty()&&drive.stage!=TirsoDrive.Stage.CONFIG)instructions.addView(button("Revisar configuración","repair_credentials",this::configureCredentials));
        instructions.addView(button("Continuar sin conexión","continue_offline",()->{drive.skip();route("home");}));
        columns.addView(instructions,new LinearLayout.LayoutParams(0,-2,1));
        if(drive.stage==TirsoDrive.Stage.QR&&!drive.qrLink.isEmpty()){
            try{
                if(!drive.qrLink.equals(renderedQr)){qrBitmap=QrCode.encode(drive.qrLink,480);renderedQr=drive.qrLink;}
                ImageView qr=new ImageView(this);qr.setImageBitmap(qrBitmap);qr.setContentDescription("Código QR para vincular Telegram");
                qr.setScaleType(ImageView.ScaleType.FIT_CENTER);qr.setBackgroundColor(Color.WHITE);
                columns.addView(qr,new LinearLayout.LayoutParams(dp(230),dp(230)));
            }catch(Exception e){columns.addView(label("No se pudo dibujar el QR. Reintenta la conexión.",14,muted,false));}
        }
        content.addView(columns);
    }
    private void configureCredentials(){
        EditText hash=new EditText(this);hash.setSingleLine(true);hash.setHint("API hash de TirsoDrive");
        hash.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Configurar TirsoDrive")
            .setMessage("API ID: "+BuildConfig.TELEGRAM_API_ID+". Introduce el API hash una sola vez; se guarda cifrado en esta TV.")
            .setView(hash).setPositiveButton("Guardar",(d,w)->{String value=hash.getText().toString().trim();hash.setText("");drive.configureHash(value);})
            .setNegativeButton("Cancelar",(d,w)->hash.setText("")).show();
    }
    private void authInput(boolean password){
        EditText input=new EditText(this);input.setSingleLine(true);
        input.setInputType(password?android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD:android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle(password?"Verificación en dos pasos":"Código de Telegram").setView(input)
            .setPositiveButton("Continuar",(d,w)->{String value=input.getText().toString();input.setText("");if(password)drive.password(value);else drive.code(value);})
            .setNegativeButton("Cancelar",(d,w)->input.setText("")).show();
    }
    private void chooseSource(Console console){
        if(drive.syncing||drive.discovering)return;
        java.util.List<org.drinkless.tdlib.TdApi.Chat> choices=drive.choices(console);
        String[] labels=new String[choices.size()+1];labels[0]="Sin grupo asignado";
        for(int i=0;i<choices.size();i++)labels[i+1]=choices.get(i).title+" · ID "+choices.get(i).id;
        new AlertDialog.Builder(this).setTitle(console.title).setItems(labels,(d,w)->{
            if(w==0)drive.unbind(console);else drive.bind(console,choices.get(w-1).id);
        }).setNegativeButton("Cancelar",null).show();
    }
    private void heading(String title, String subtitle) {
        body.addView(label(title, 28, text, true)); body.addView(label(subtitle, 12, muted, false));
    }
    private LinearLayout scrollContent() {
        ScrollView scroll = new ScrollView(this); scroll.setClipToPadding(false); scroll.setFillViewport(true);activeScroll=scroll;
        content = vertical(); content.setPadding(0, dp(10), 0, dp(8)); scroll.addView(content);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return content;
    }
    private ImageView logo(Console c) {
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(c.title);
        int id = getResources().getIdentifier("logo_" + c.logo, "drawable", getPackageName());
        if (id != 0) image.setImageResource(id);
        image.setPadding(dp(14), dp(9), dp(14), dp(9)); return image;
    }
    private String gameCount(int count) { return count+ (count==1?" juego":" juegos"); }
    private void home() {
        java.util.ArrayList<Console> visible=new java.util.ArrayList<>();
        for(Console c:Console.values())if(library.visible(c))visible.add(c);
        if(visible.isEmpty()) {
            content=scrollContent();content.addView(label("No hay consolas visibles",20,text,true));
            content.addView(button("Elegir consolas","empty_settings",()->route("settings")));return;
        }
        if(library.theme().equals("Consola completa")){heroHome(visible);return;}
        content=vertical();content.setPadding(0,dp(10),0,0);body.addView(content,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout row = null; int index = 0;
        for (Console c : visible) {
            if (index % 3 == 0) { row = horizontal();content.addView(row,new LinearLayout.LayoutParams(-1,0,1)); }
            LinearLayout tile = vertical(); tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(14),dp(6),dp(14),dp(4));
            ImageView logo = logo(c);logo.setPadding(dp(8),0,dp(8),0);
            tile.addView(logo, new LinearLayout.LayoutParams(-1,0,1));
            TextView name = label(gameCount(library.count(c)), 11, 0xFF526177, false);
            name.setPadding(0,dp(1),0,0);
            name.setGravity(Gravity.CENTER); tile.addView(name);
            tile.setContentDescription(c.title+", "+library.count(c)+" juegos");
            focusable(tile, "console_" + c.name(), library.dark()?0xFFDCE2ED:Color.WHITE, () -> openConsole(c));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-1,1); p.setMargins(dp(6),dp(6),dp(6),dp(6));
            row.addView(tile, p); index++;
        }
        while(index++%3!=0)row.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));
    }
    private void openConsole(Console c) {
        console=c;library.homeConsole(c);favorites=false;query="";libraryFocus="";libraryScroll=0;visibleGames=60;route("library");
    }
    private Console heroConsole(java.util.List<Console> consoles) {
        for(Console c:consoles)if(c.name().equals(library.homeConsole()))return c;return consoles.get(0);
    }
    private void moveHero(int delta) {
        java.util.ArrayList<Console> consoles=new java.util.ArrayList<>();for(Console c:Console.values())if(library.visible(c))consoles.add(c);
        if(consoles.isEmpty())return;
        int index=consoles.indexOf(heroConsole(consoles));
        library.homeConsole(consoles.get((index+delta+consoles.size())%consoles.size()));lastFocus="hero_console";render();
    }
    private void heroHome(java.util.List<Console> consoles) {
        Console current=heroConsole(consoles);
        if(lastFocus.isEmpty())lastFocus="hero_console";
        content=vertical();body.addView(content,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout row=horizontal();content.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        row.addView(icon("back","Consola anterior","hero_prev",()->moveHero(-1)));
        LinearLayout hero=vertical();hero.setGravity(Gravity.CENTER);hero.setPadding(dp(34),dp(34),dp(34),dp(20));
        hero.addView(logo(current),new LinearLayout.LayoutParams(-1,0,1));
        TextView count=label(gameCount(library.count(current)),13,0xFF526177,false);count.setGravity(Gravity.CENTER);hero.addView(count);
        hero.setContentDescription(current.title+", "+library.count(current)+" juegos. Abrir biblioteca");
        focusable(hero,"hero_console",library.dark()?0xFFDCE2ED:Color.WHITE,()->openConsole(current));
        hero.setOnKeyListener((v,key,e)->{
            if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT){moveHero(-1);return true;}
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT){moveHero(1);return true;}return false;
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(16),dp(14),dp(16),dp(8));row.addView(hero,p);
        row.addView(icon("next","Consola siguiente","hero_next",()->moveHero(1)));
    }
    private void libraryScreen() {
        List<Game> games = library.filtered(console, favorites, query);
        LinearLayout summary=horizontal();
        summary.addView(label(games.size()+" juegos  ·  "+(library.descending()?"Z – A":"A – Z")+(query.isEmpty()?"":"  ·  "+query),12,muted,false),new LinearLayout.LayoutParams(0,-2,1));
        if(!query.isEmpty())summary.addView(button("Limpiar","clear",()->{query="";libraryScroll=0;lastFocus="";render();}));
        body.addView(summary);
        int columns=getResources().getConfiguration().screenWidthDp>=800?6:5;
        LinearLayout grid = scrollContent(); LinearLayout row = null; int index = 0;
        for (Game game : games.subList(0,Math.min(visibleGames,games.size()))) {
            if (index % columns == 0) { row = horizontal(); grid.addView(row); }
            LinearLayout tile = vertical(); tile.setPadding(dp(7),dp(7),dp(7),dp(6));
            FrameLayout art = new FrameLayout(this);
            TextView placeholder = label(game.title,11,muted,true); placeholder.setGravity(Gravity.CENTER);
            art.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
            ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription(game.title);
            image.setBackgroundColor(surface); art.addView(image, new FrameLayout.LayoutParams(-1, -1));
            // Transparent fallback keeps the title placeholder visible if artwork is unavailable.
            image.setBackgroundColor(Color.TRANSPARENT);
            tile.addView(art, new LinearLayout.LayoutParams(-1,dp(90))); artwork.load(game, image);
            TextView name = label((library.favorite(game) ? "★ " : "") + game.title,11,text,true);
            name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END); tile.addView(name);
            focusable(tile, "game_" + game.id, surface, () -> { selected = game;drive.inspectFile(game);route("detail"); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,dp(135),1); p.setMargins(dp(5),dp(5),dp(5),dp(5));
            row.addView(tile, p); index++;
        }
        if (index == 0) grid.addView(label(favorites ? "Guarda juegos desde su ficha para verlos aquí." : "No hay juegos que coincidan.", 18, muted, false));
        else while (index++ % columns != 0) row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(135),1));
        if(games.size()>visibleGames)grid.addView(button("Mostrar más juegos","load_more",()->{
            int next=visibleGames;visibleGames+=60;lastFocus="game_"+games.get(next).id;render();
        }));
    }
    private void refreshCatalog() {
        if(drive.stage==TirsoDrive.Stage.READY){
            boolean assigned=false;for(TirsoDrive.Source s:drive.sources.values())if(s.chatId!=0)assigned=true;
            if(!assigned){connectScreen();return;}
            drive.sync();Toast.makeText(this,drive.syncing?"Actualizando desde Telegram…":drive.status,Toast.LENGTH_SHORT).show();
        }
        else connectScreen();
    }
    private void sortGames() {
        new AlertDialog.Builder(this).setTitle("Orden alfabético")
            .setSingleChoiceItems(new String[]{"Título: A → Z","Título: Z → A"},library.descending()?1:0,(dialog,which)->{
                library.descending(which==1);libraryScroll=0;lastFocus="sort";dialog.dismiss();render();
            }).setNegativeButton("Cancelar",null).show();
    }
    private void detail() {
        heading(selected.console.title, library.demo ? "Ficha de muestra · Sin archivo de juego" : "Información del catálogo");
        content = scrollContent(); LinearLayout columns = horizontal(); columns.setGravity(Gravity.TOP);
        ImageView cover = new ImageView(this); cover.setScaleType(ImageView.ScaleType.FIT_CENTER); cover.setContentDescription(selected.title);
        columns.addView(cover, new LinearLayout.LayoutParams(dp(215), dp(245))); artwork.load(selected, cover);
        LinearLayout info = vertical(); info.setPadding(dp(24), 0, 0, 0);
        info.addView(label(selected.title, 26, text, true));
        info.addView(label(selected.description, 15, muted, false));
        if(selected.telegramAccount!=0){
            downloadLabel=label("",13,muted,false);info.addView(downloadLabel);
            downloadButton=button("Descargar juego","download_game",()->{
                org.drinkless.tdlib.TdApi.File file=drive.file(selected);
                if(file!=null&&file.local.isDownloadingActive)drive.cancelDownload(selected);
                else if(file!=null&&file.local.isDownloadingCompleted)message("Archivo descargado","El archivo está en la TV. La ejecución se habilitará con el núcleo integrado de esta consola.");
                else if(!drive.owns(selected))connectScreen();else drive.download(selected);
                updateDownload();
            });info.addView(downloadButton);updateDownload();
        }
        info.addView(button(library.favorite(selected) ? "★ Quitar de favoritos" : "☆ Guardar en favoritos", "favorite_toggle", () -> { library.toggleFavorite(selected); render(); }));
        info.addView(button("Estado de emulación", "engine", () -> message("Emulación integrada", "Esta primera preview permite probar el launcher. Los núcleos de emulación y el inicio de juegos todavía no están integrados.")));
        info.addView(button("Volver a la biblioteca", "back_library", this::backToLibrary));
        columns.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); content.addView(columns);
    }
    private void updateDownload(){
        if(selected==null||selected.telegramAccount==0||downloadLabel==null||downloadButton==null)return;
        org.drinkless.tdlib.TdApi.File file=drive.file(selected);
        String size=android.text.format.Formatter.formatFileSize(this,selected.fileSize);
        if(file!=null&&file.local.isDownloadingCompleted){downloadLabel.setText("Archivo descargado · "+size);downloadButton.setText("Archivo descargado");}
        else if(file!=null&&file.local.isDownloadingActive){
            int percent=(int)Math.min(100,file.local.downloadedSize*100/Math.max(1,Math.max(file.size,file.expectedSize)));
            downloadLabel.setText("Descargando: "+percent+"% · "+size);downloadButton.setText("Cancelar descarga");
        }else{downloadLabel.setText(size+" · "+(drive.owns(selected)?"Disponible en Telegram":"Conecta la cuenta de este juego"));downloadButton.setText(drive.owns(selected)?"Descargar juego":"Conectar TirsoDrive");}
        if(!drive.error.isEmpty())downloadLabel.setText(drive.error);
    }
    private void search() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(query); input.setHint("Nombre del juego");
        new AlertDialog.Builder(this).setTitle("Buscar juego").setView(input)
                .setPositiveButton("Buscar", (d,w) -> { query = input.getText().toString().trim();libraryScroll=0;lastFocus="search";render(); })
                .setNegativeButton("Cancelar", null).show();
    }
    private void settings() {
        heading("Ajustes", "Personaliza tu espacio de juego"); content = scrollContent();
        content.addView(label("APARIENCIA", 12, accent, true));
        content.addView(button("Modo de color: " + (library.dark() ? "Oscuro" : "Claro") + "  ·  Cambiar", "color_mode", () -> { library.dark(!library.dark()); render(); }));
        content.addView(button("Tema: " + library.theme(), "visual_theme", () -> {
            String[] themes = {"Moderno", "GBA clásico", "Nintendo rojo", "Retro naranja", "Consola completa"};
            int current=java.util.Arrays.asList(themes).indexOf(library.theme());
            new AlertDialog.Builder(this).setTitle("Tema visual").setSingleChoiceItems(themes,current,(d,w)->{library.theme(themes[w]);d.dismiss();render();}).show();
        }));
        content.addView(label("CONSOLAS VISIBLES", 12, accent, true));
        for (Console c : Console.values()) content.addView(button((library.visible(c) ? "✓  " : "—  ") + c.title, "visible_" + c.name(), () -> {
            int count = 0; for (Console value : Console.values()) if (library.visible(value)) count++;
            if (count == 1 && library.visible(c)) {
                new AlertDialog.Builder(this).setTitle("Ocultar última consola")
                        .setMessage("El inicio quedará vacío. Puedes volver a activarla desde Ajustes.")
                        .setPositiveButton("Ocultar", (d,w) -> { library.visible(c, false); render(); }).setNegativeButton("Cancelar", null).show();
            } else { library.visible(c, !library.visible(c)); render(); }
        }));
        content.addView(button("Mostrar todas las consolas", "show_all", () -> { for (Console c : Console.values()) library.visible(c, true); render(); }));
        content.addView(label("BIBLIOTECA Y CONEXIÓN", 12, accent, true));
        content.addView(button("TirsoDrive · "+(drive.stage==TirsoDrive.Stage.READY?"Conectado":"Conectar"), "telegram",this::connectScreen));
        content.addView(button("Controles conectados", "controllers", this::controllers));
        content.addView(button("Acerca de esta versión", "about", () -> message("Tirsolador 0.3.0 · TirsoDrive", "Cliente nativo de Telegram con QR, grupos por consola, sincronización y descargas locales. Android 8 o superior. La ejecución con núcleos y los respaldos siguen pendientes.\n\nTDLib: Telegram, Boost Software License; distribución nativa tdlib-android 0.1.1. QR: ZXing, Apache 2.0. Logos: Carbon, Rookervik / RetroPie, CC BY-NC-SA; marcas de sus propietarios.")));
    }
    private void controllers() {
        java.util.ArrayList<InputDevice> devices=new java.util.ArrayList<>();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) devices.add(device);
        }
        if(devices.isEmpty()){message("Perfiles de controles","No se detectaron mandos. Vincula uno por Bluetooth o USB desde los ajustes de Android TV.");return;}
        String[] names=new String[devices.size()];
        for(int i=0;i<names.length;i++)names[i]=ControllerProfiles.model(devices.get(i))+" · "+devices.get(i).getName();
        new AlertDialog.Builder(this).setTitle("Perfil por control").setItems(names,(d,w)->controllerProfile(devices.get(w))).show();
    }
    private void controllerProfile(InputDevice device) {
        float[] values={.20f,.40f,.60f};String[] choices={"Zona muerta: baja (20%)","Zona muerta: media (40%)","Zona muerta: alta (60%)"};
        int choice=0;float current=controllers.deadzone(device);for(int i=0;i<values.length;i++)if(Math.abs(values[i]-current)<.05f)choice=i;
        new AlertDialog.Builder(this).setTitle(ControllerProfiles.model(device))
            .setSingleChoiceItems(choices,choice,(dialog,which)->{
                controllers.deadzone(device,values[which]);Toast.makeText(this,"Perfil guardado para este dispositivo",Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Diagnóstico",(d,w)->{
                ControllerProfiles.State s=controllers.state(device.getId());
                message("Entradas independientes",String.format(java.util.Locale.ROOT,
                    "ID: %04x:%04x\nD-pad teclas: %d, %d\nD-pad HAT: %d, %d\nStick izquierdo: %.2f, %.2f\nStick derecho: %.2f, %.2f\n\nCada origen mantiene su estado separado. El remapeo de emulación se añadirá con los núcleos.",
                    device.getVendorId(),device.getProductId(),s.keyX,s.keyY,s.hatX,s.hatY,s.leftX,s.leftY,s.rightX,s.rightY));
            }).setPositiveButton("Listo",null).show();
    }
    private void message(String title, String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Entendido", null).show(); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(controllers.key(event,true))return true;
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) { onBackPressed(); return true; }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            View focus = getCurrentFocus(); if (focus != null && event.getRepeatCount() == 0) focus.performClick(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_START) { route("settings"); return true; }
        return super.onKeyDown(keyCode, event);
    }
    @Override public boolean onKeyUp(int keyCode,KeyEvent event) {
        controllers.key(event,false);
        if(keyCode==KeyEvent.KEYCODE_BUTTON_A||keyCode==KeyEvent.KEYCODE_BUTTON_B||keyCode==KeyEvent.KEYCODE_BUTTON_START)return true;
        return super.onKeyUp(keyCode,event);
    }
    @Override public boolean onGenericMotionEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_MOVE && (event.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK) {
            int direction=controllers.motion(event);
            if(direction!=0) {
                View focused=getCurrentFocus();
                if(focused!=null&&"hero_console".equals(focused.getTag())&&(direction==View.FOCUS_LEFT||direction==View.FOCUS_RIGHT))moveHero(direction==View.FOCUS_RIGHT?1:-1);
                else if(focused!=null){View next=focused.focusSearch(direction);if(next!=null)next.requestFocus();}
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }
    @Override public void onBackPressed() {
        if(page.equals("welcome")){drive.skip();route("home");return;}
        if (page.equals("detail")) backToLibrary();
        else if (!page.equals("home")) route("home");
        else new AlertDialog.Builder(this).setTitle("Salir de Tirsolador")
                    .setPositiveButton("Salir", (d,w) -> finish()).setNegativeButton("Cancelar", null).show();
    }
    @Override protected void onDestroy() {drive.detach(driveListener);inputManager.unregisterInputDeviceListener(inputListener);artwork.close(); super.onDestroy(); }
}
