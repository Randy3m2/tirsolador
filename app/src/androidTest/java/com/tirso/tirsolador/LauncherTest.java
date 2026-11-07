package com.tirso.tirsolador;

import android.view.View;
import android.graphics.Bitmap;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherTest {
    @Rule public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);
    @Test public void testColdLaunchAndPersistentAppearance() throws Throwable {
        MainActivity activity = rule.getActivity();
        Library model = new Library(activity);
        boolean original = model.dark();
        rule.runOnUiThread(() -> {
            View settings = activity.getWindow().getDecorView().findViewWithTag("nav_settings");
            assertNotNull(settings); settings.performClick();
            View toggle = activity.getWindow().getDecorView().findViewWithTag("color_mode");
            assertNotNull(toggle); toggle.performClick();
            assertEquals(!original, new Library(activity).dark());
            toggle = activity.getWindow().getDecorView().findViewWithTag("color_mode");
            assertTrue(toggle.requestFocus()); toggle.performClick();
            assertEquals(original, new Library(activity).dark());
        });
    }
    @Test public void testInvalidCatalogDoesNotReplaceLibrary() throws Exception {
        Library library = new Library(rule.getActivity()); int before = library.games.size();
        try { library.importCatalog("{\"schemaVersion\":1,\"games\":[{\"id\":\"a\",\"title\":\"Game\",\"console\":\"BAD\"}]}"); fail("Invalid console accepted"); }
        catch (JSONException expected) { assertEquals(before, library.games.size()); }
    }
    @Test public void testArtworkPathEncoding() {
        Game game = new Game("x", "x", Console.GBA, "", "Pokemon - Emerald (USA)");
        assertEquals("https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/Pokemon%20-%20Emerald%20(USA).png", Artwork.url(game));
    }
    @Test public void testLauncherToolbarAndCompactLibrary() throws Throwable {
        MainActivity activity=rule.getActivity();Library library=new Library(activity);
        String oldTheme=library.theme();boolean oldDark=library.dark();boolean gbaVisible=library.visible(Console.GBA);
        try {
            library.theme("Moderno");library.dark(true);library.visible(Console.GBA,true);
            rule.runOnUiThread(()->{
                activity.getWindow().getDecorView().findViewWithTag("nav_settings").performClick();
                activity.getWindow().getDecorView().findViewWithTag("nav_back").performClick();
                View root=activity.getWindow().getDecorView();
                assertNull(root.findViewWithTag("nav_home"));
                assertNoOldLauncherText(root);
                assertEquals("Favoritos",root.findViewWithTag("nav_favorites").getContentDescription());
            });
            capture("01-launcher-dark");
            rule.runOnUiThread(()->{
                activity.getWindow().getDecorView().findViewWithTag("console_GBA").performClick();
                View root=activity.getWindow().getDecorView();
                assertNotNull(root.findViewWithTag("search"));assertNotNull(root.findViewWithTag("refresh"));assertNotNull(root.findViewWithTag("sort"));
                assertNull(root.findViewWithTag("import"));
            });
            capture("02-library-compact");
        } finally { library.theme(oldTheme);library.dark(oldDark);library.visible(Console.GBA,gbaVisible); }
    }
    @Test public void testFullscreenConsoleThemePersistsAndNavigates() throws Throwable {
        MainActivity activity=rule.getActivity();Library library=new Library(activity);
        String oldTheme=library.theme(), oldConsole=library.homeConsole();boolean oldDark=library.dark();
        try {
            library.theme("Consola completa");library.dark(false);
            rule.runOnUiThread(()->{
                activity.getWindow().getDecorView().findViewWithTag("nav_settings").performClick();
                activity.getWindow().getDecorView().findViewWithTag("nav_back").performClick();
                assertNotNull(activity.getWindow().getDecorView().findViewWithTag("hero_console"));
                activity.getWindow().getDecorView().findViewWithTag("hero_next").performClick();
                assertEquals("Consola completa",new Library(activity).theme());
            });
            capture("03-fullscreen-light");
            library.dark(true);
            rule.runOnUiThread(()->{
                activity.getWindow().getDecorView().findViewWithTag("nav_settings").performClick();
                activity.getWindow().getDecorView().findViewWithTag("nav_back").performClick();
            });
            capture("04-fullscreen-dark");
        } finally {library.theme(oldTheme);library.dark(oldDark);library.homeConsole(Console.valueOf(oldConsole));}
    }
    @Test public void testReverseAlphabeticalOrderAndPersistence() throws Exception {
        Library library=new Library(rule.getActivity());boolean descending=library.descending();
        try {
            library.descending(true);assertTrue(new Library(rule.getActivity()).descending());
            java.util.List<Game> reversed=library.filtered(null,false,"");
            for(int i=1;i<reversed.size();i++)assertTrue(reversed.get(i-1).title.compareToIgnoreCase(reversed.get(i).title)>=0);
            library.descending(false);java.util.List<Game> forward=library.filtered(null,false,"");
            for(int i=1;i<forward.size();i++)assertTrue(forward.get(i-1).title.compareToIgnoreCase(forward.get(i).title)<=0);
        } finally {library.descending(descending);}
    }
    @Test public void testDpadAndSticksAreIndependentAcrossDevices() {
        ControllerProfiles profiles=new ControllerProfiles(rule.getActivity());
        ControllerProfiles.State first=profiles.state(101), second=profiles.state(202);
        first.leftX=.9f;first.hatY=-1;first.rightY=1;
        assertEquals(View.FOCUS_UP,ControllerProfiles.navigate(first,1000,.2f));
        assertEquals(0,ControllerProfiles.navigate(second,1000,.2f));
        first.hatY=0;
        assertEquals(View.FOCUS_RIGHT,ControllerProfiles.navigate(first,1300,.2f));
        first.leftX=0;
        assertEquals(0,ControllerProfiles.navigate(first,1600,.2f));
        assertEquals(1f,first.rightY,0f);assertEquals(0f,second.leftX,0f);
        profiles.disconnect(101);assertNotSame(first,profiles.state(101));
    }
    private void capture(String name) throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File folder=new File(rule.getActivity().getExternalFilesDir(null),"screenshots");assertTrue(folder.isDirectory()||folder.mkdirs());
        try(FileOutputStream stream=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{bitmap.recycle();}
    }
    private void assertNoOldLauncherText(View view) {
        if(view instanceof TextView) {
            String value=((TextView)view).getText().toString();
            assertFalse(value.equals("TIRSOLADOR")||value.equals("Tus consolas")||value.contains("Vista previa del catálogo")||value.contains("D-pad  Navegar"));
        }
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)assertNoOldLauncherText(((android.view.ViewGroup)view).getChildAt(i));
    }
}
