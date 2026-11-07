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
    private TextView status;
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
        library = new Library(this); artwork = new Artwork(this);
        if (state != null) {
            page = state.getString("page", "home"); query = state.getString("query", "");
            favorites = state.getBoolean("favorites"); lastFocus = state.getString("focus", "");
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
    private void route(String target) { page = target; lastFocus = ""; render(); }
    private void render() {
        boolean dark = library.dark();
        background = dark ? 0xFF0B1224 : 0xFFF5F7FA; surface = dark ? 0xFF162239 : Color.WHITE;
        text = dark ? 0xFFF4F7FC : 0xFF142238; muted = dark ? 0xFFABBCD1 : 0xFF52657A;
        accent = dark ? 0xFF14DADE : 0xFF007E89;
        if (library.theme().equals("Nintendo rojo")) accent = dark ? 0xFFFF6574 : 0xFFBF243B;
        if (library.theme().equals("Retro naranja")) accent = dark ? 0xFFFFAD57 : 0xFFAA5700;
        if (library.theme().equals("GBA clásico")) accent = dark ? 0xFFBFA1FF : 0xFF7042A3;
        root = vertical(); root.setPadding(dp(32), dp(16), dp(32), dp(12)); root.setBackgroundColor(background);
        setContentView(root);
        LinearLayout header = horizontal();
        TextView brand = label("TIRSOLADOR", 23, text, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(button("Consolas", "nav_home", () -> route("home")));
        header.addView(button("Favoritos", "nav_favorites", () -> { console = null; favorites = true; query = ""; route("library"); }));
        header.addView(button("Ajustes", "nav_settings", () -> route("settings")));
        root.addView(header);
        body = vertical(); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        if (page.equals("settings")) settings();
        else if (page.equals("detail") && selected != null) detail();
        else if (page.equals("library")) libraryScreen();
        else home();
        status = label("D-pad  Navegar       A / OK  Seleccionar       B  Volver     •     v0.1 Preview", 11, muted, false);
        root.addView(status);
        root.post(() -> {
            View target = lastFocus.isEmpty() ? null : root.findViewWithTag(lastFocus);
            if (target == null) target = firstContentFocusable(content == null ? body : content);
            if (target != null) target.requestFocus();
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
        ScrollView scroll = new ScrollView(this); scroll.setClipToPadding(false); scroll.setFillViewport(true);
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
        heading("Tus consolas", library.demo ? "Vista previa del catálogo · Sin ROMs incluidas" : "Tu biblioteca, a tu manera");
        LinearLayout grid = scrollContent(); LinearLayout row = null; int index = 0;
        for (Console c : Console.values()) if (library.visible(c)) {
            if (index % 3 == 0) { row = horizontal(); grid.addView(row); }
            LinearLayout tile = vertical(); tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(12), dp(6), dp(12), dp(6));
            ImageView logo = logo(c); tile.addView(logo, new LinearLayout.LayoutParams(-1, dp(62)));
            TextView name = label(c.title + "   ·   " + library.count(c) + " juegos", 11, 0xFF263449, false);
            name.setGravity(Gravity.CENTER); tile.addView(name);
            focusable(tile, "console_" + c.name(), 0xFFE6EAF2, () -> {
                console = c; favorites = false; query = ""; route("library");
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(98), 1); p.setMargins(dp(5), dp(5), dp(5), dp(5));
            row.addView(tile, p); index++;
        }
        if (index == 0) {
            grid.addView(label("No hay consolas visibles", 20, text, true));
            grid.addView(button("Elegir consolas", "empty_settings", () -> route("settings")));
        } else while (index++ % 3 != 0) row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(98), 1));
    }
    private void libraryScreen() {
        heading(favorites ? "Tus favoritos" : console == null ? "Biblioteca" : console.title,
                library.demo ? "Catálogo de muestra · Selecciona un juego para ver su ficha" : "Orden alfabético");
        LinearLayout actions = horizontal();
        actions.addView(button(query.isEmpty() ? "Buscar juego" : "Buscar: " + query, "search", this::search));
        actions.addView(button("Importar catálogo", "import", this::importCatalog));
        if (!query.isEmpty()) actions.addView(button("Limpiar búsqueda", "clear", () -> { query = ""; render(); }));
        body.addView(actions);
        List<Game> games = library.filtered(console, favorites, query);
        LinearLayout grid = scrollContent(); LinearLayout row = null; int index = 0;
        for (Game game : games) {
            if (index % 4 == 0) { row = horizontal(); grid.addView(row); }
            LinearLayout tile = vertical(); tile.setPadding(dp(8), dp(8), dp(8), dp(8));
            FrameLayout art = new FrameLayout(this);
            TextView placeholder = label(game.console.title + "\n" + game.title, 14, muted, true); placeholder.setGravity(Gravity.CENTER);
            art.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
            ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription(game.title);
            image.setBackgroundColor(surface); art.addView(image, new FrameLayout.LayoutParams(-1, -1));
            // Transparent fallback keeps the title placeholder visible if artwork is unavailable.
            image.setBackgroundColor(Color.TRANSPARENT);
            tile.addView(art, new LinearLayout.LayoutParams(-1, dp(135))); artwork.load(game, image);
            TextView name = label((library.favorite(game) ? "★ " : "") + game.title, 13, text, true);
            name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END); tile.addView(name);
            focusable(tile, "game_" + game.id, surface, () -> { selected = game; route("detail"); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(193), 1); p.setMargins(dp(5), dp(5), dp(5), dp(5));
            row.addView(tile, p); index++;
        }
        if (index == 0) grid.addView(label(favorites ? "Guarda juegos desde su ficha para verlos aquí." : "No hay juegos que coincidan.", 18, muted, false));
        else while (index++ % 4 != 0) row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(193), 1));
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
        info.addView(button("Volver a la biblioteca", "back_library", () -> route("library")));
        columns.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); content.addView(columns);
    }
    private void search() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(query); input.setHint("Nombre del juego");
        new AlertDialog.Builder(this).setTitle("Buscar juego").setView(input)
                .setPositiveButton("Buscar", (d,w) -> { query = input.getText().toString().trim(); render(); })
                .setNegativeButton("Cancelar", null).show();
    }
    private void settings() {
        heading("Ajustes", "Personaliza tu espacio de juego"); content = scrollContent();
        content.addView(label("APARIENCIA", 12, accent, true));
        content.addView(button("Modo de color: " + (library.dark() ? "Oscuro" : "Claro") + "  ·  Cambiar", "color_mode", () -> { library.dark(!library.dark()); render(); }));
        content.addView(button("Tema: " + library.theme(), "visual_theme", () -> {
            String[] themes = {"Moderno", "GBA clásico", "Nintendo rojo", "Retro naranja"};
            new AlertDialog.Builder(this).setTitle("Tema visual").setItems(themes, (d,w) -> { library.theme(themes[w]); render(); }).show();
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
        content.addView(button("Importar catálogo JSON", "import_settings", this::importCatalog));
        content.addView(button("TirsoDrive · Estado", "telegram", () -> message("TirsoDrive", "API ID registrado: 38798734. La conexión con TDLib, el login QR y los canales aún están pendientes de integración. Esta APK no abre sesiones de Telegram.")));
        content.addView(button("Controles conectados", "controllers", this::controllers));
        content.addView(button("Acerca de esta versión", "about", () -> message("Tirsolador 0.1.0 Preview", "Launcher nativo Android TV. Incluye temas, favoritos, catálogo e imágenes de Libretro. Sin ROMs ni núcleos de emulación. Respaldos y sincronización pendientes.\n\nLogos: tema Carbon, Rookervik / RetroPie. Licencia CC BY-NC-SA; marcas de sus respectivos propietarios.")));
    }
    private void controllers() {
        StringBuilder names = new StringBuilder();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (device.getSources() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD)) names.append("• ").append(device.getName()).append('\n');
        }
        message("Controles", names.length() == 0 ? "No se detectaron mandos. Vincula uno por Bluetooth o USB desde los ajustes de Android TV." : names.toString());
    }
    private void message(String title, String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Entendido", null).show(); }
    private void importCatalog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, 10); }
        catch (android.content.ActivityNotFoundException e) { message("Selector de archivos", "Este televisor no tiene un selector de documentos. Instala un gestor de archivos compatible para importar el catálogo."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 10 || result != RESULT_OK || data == null || data.getData() == null) return;
        // Import on a worker; malformed catalogs never replace the current one.
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                if (in == null) throw new java.io.IOException("No se pudo abrir el archivo");
                String json = new String(Library.readLimited(in, 2_000_000), StandardCharsets.UTF_8);
                Library.parse(json);
                runOnUiThread(() -> {
                    try { library.importCatalog(json); route("home"); Toast.makeText(this, "Catálogo importado", Toast.LENGTH_SHORT).show(); }
                    catch (Exception e) { message("Catálogo inválido", "Revisa el formato JSON del catálogo."); }
                });
            } catch (Exception e) { runOnUiThread(() -> message("No se pudo importar", "Usa un catálogo JSON válido, de hasta 2 MB y con consolas compatibles.")); }
        }, "catalog-import").start();
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) { onBackPressed(); return true; }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            View focus = getCurrentFocus(); if (focus != null && event.getRepeatCount() == 0) focus.performClick(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_START) { route("settings"); return true; }
        return super.onKeyDown(keyCode, event);
    }
    @Override public void onBackPressed() {
        if (page.equals("detail")) route("library");
        else if (!page.equals("home")) route("home");
        else new AlertDialog.Builder(this).setTitle("Salir de Tirsolador")
                    .setPositiveButton("Salir", (d,w) -> finish()).setNegativeButton("Cancelar", null).show();
    }
    @Override protected void onDestroy() { artwork.close(); super.onDestroy(); }
}
