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
        render();
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
        TextView title=label(page.equals("library")?(favorites?"Favoritos":console==null?"Biblioteca":console.title):"",18,text,true);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(page.equals("library")) {
            header.addView(icon("search","Buscar juegos","search",this::search));
            header.addView(icon("refresh","Refrescar catálogo","refresh",this::refreshCatalog));
            header.addView(icon("sort","Ordenar juegos: "+(library.descending()?"Z a A":"A a Z"),"sort",this::sortGames));
        }
        header.addView(icon("star","Favoritos","nav_favorites", () -> {
            console = null; favorites = true; query = ""; libraryFocus="";libraryScroll=0;route("library");
        }));
        header.addView(icon("settings","Ajustes","nav_settings", () -> route("settings")));
        root.addView(header);
        body = vertical(); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        if (page.equals("settings")) settings();
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
            tile.setPadding(dp(18),dp(12),dp(18),dp(8));
            ImageView logo = logo(c); tile.addView(logo, new LinearLayout.LayoutParams(-1,0,1));
            TextView name = label(library.count(c) + " juegos", 11, 0xFF526177, false);
            name.setGravity(Gravity.CENTER); tile.addView(name);
            tile.setContentDescription(c.title+", "+library.count(c)+" juegos");
            focusable(tile, "console_" + c.name(), library.dark()?0xFFDCE2ED:Color.WHITE, () -> openConsole(c));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-1,1); p.setMargins(dp(6),dp(6),dp(6),dp(6));
            row.addView(tile, p); index++;
        }
        while(index++%3!=0)row.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));
    }
    private void openConsole(Console c) {
        console=c;library.homeConsole(c);favorites=false;query="";libraryFocus="";libraryScroll=0;route("library");
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
        TextView count=label(library.count(current)+" juegos",13,0xFF526177,false);count.setGravity(Gravity.CENTER);hero.addView(count);
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
        for (Game game : games) {
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
            focusable(tile, "game_" + game.id, surface, () -> { selected = game; route("detail"); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,dp(135),1); p.setMargins(dp(5),dp(5),dp(5),dp(5));
            row.addView(tile, p); index++;
        }
        if (index == 0) grid.addView(label(favorites ? "Guarda juegos desde su ficha para verlos aquí." : "No hay juegos que coincidan.", 18, muted, false));
        else while (index++ % columns != 0) row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(135),1));
    }
    private void refreshCatalog() {
        rememberLibrary();library=new Library(this);render();
        Toast.makeText(this,"Catálogo local recargado. TirsoDrive aún no está conectado.",Toast.LENGTH_LONG).show();
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
        info.addView(button(library.favorite(selected) ? "★ Quitar de favoritos" : "☆ Guardar en favoritos", "favorite_toggle", () -> { library.toggleFavorite(selected); render(); }));
        info.addView(button("Estado de emulación", "engine", () -> message("Emulación integrada", "Esta primera preview permite probar el launcher. Los núcleos de emulación y el inicio de juegos todavía no están integrados.")));
        info.addView(button("Volver a la biblioteca", "back_library", this::backToLibrary));
        columns.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); content.addView(columns);
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
        content.addView(button("TirsoDrive · Estado", "telegram", () -> message("TirsoDrive", "API ID registrado: 38798734. La conexión con TDLib, el login QR y los canales aún están pendientes de integración. Esta APK no abre sesiones de Telegram.")));
        content.addView(button("Controles conectados", "controllers", this::controllers));
        content.addView(button("Acerca de esta versión", "about", () -> message("Tirsolador 0.2.0 · Build 2", "Launcher nativo Android TV. Esta versión incorpora el nuevo diseño, el tema Consola completa y perfiles separados de entrada. Catálogo de muestra; Telegram, emulación y respaldos siguen pendientes.\n\nLogos: tema Carbon, Rookervik / RetroPie. Licencia CC BY-NC-SA; marcas de sus respectivos propietarios.")));
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
        if (page.equals("detail")) backToLibrary();
        else if (!page.equals("home")) route("home");
        else new AlertDialog.Builder(this).setTitle("Salir de Tirsolador")
                    .setPositiveButton("Salir", (d,w) -> finish()).setNegativeButton("Cancelar", null).show();
    }
    @Override protected void onDestroy() { inputManager.unregisterInputDeviceListener(inputListener);artwork.close(); super.onDestroy(); }
}
