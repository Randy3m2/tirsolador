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
import org.junit.Before;
import org.junit.runner.RunWith;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherTest {
    @Rule public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);
    @Before public void enterOfflineLauncher() throws Throwable {
        rule.runOnUiThread(()->{
            View skip=rule.getActivity().getWindow().getDecorView().findViewWithTag("continue_offline");if(skip!=null)skip.performClick();
        });
    }
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
    @Test public void modernConsoleVisibilityCardsPersist() throws Throwable {
        Library library=new Library(rule.getActivity());boolean wasVisible=library.visible(Console.GBA);
        try{
            rule.runOnUiThread(()->{
                rule.getActivity().getWindow().getDecorView().findViewWithTag("nav_settings").performClick();
                for(Console c:Console.values())assertNotNull(rule.getActivity().getWindow().getDecorView().findViewWithTag("visible_"+c.name()));
                rule.getActivity().getWindow().getDecorView().findViewWithTag("visible_GBA").performClick();assertEquals(!wasVisible,new Library(rule.getActivity()).visible(Console.GBA));
                View card=rule.getActivity().getWindow().getDecorView().findViewWithTag("visible_GBA");card.requestFocus();card.requestRectangleOnScreen(new android.graphics.Rect(0,0,card.getWidth(),card.getHeight()),true);
            });capture("07-settings-consoles");
        }finally{library.visible(Console.GBA,wasVisible);}
    }
    private void capture(String name) throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        // Gradle uninstalls the test target before adb pull. Shell-owned captures
        // in Downloads survive that cleanup without granting storage access to the app.
        shellCaptureCommand("mkdir -p /sdcard/Download/tirsolador-screenshots");
        shellCaptureCommand("screencap -p /sdcard/Download/tirsolador-screenshots/"+name+".png");
    }
    private void shellCaptureCommand(String command) throws Exception {
        android.os.ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        try(java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){ /* Wait for the shell command to finish. */ }
        }
    }
    private void assertNoOldLauncherText(View view) {
        if(view instanceof TextView) {
            String value=((TextView)view).getText().toString();
            assertFalse(value.equals("TIRSOLADOR")||value.equals("Tus consolas")||value.contains("Vista previa del catálogo")||value.contains("D-pad  Navegar"));
        }
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)assertNoOldLauncherText(((android.view.ViewGroup)view).getChildAt(i));
    }
    @Test public void testRealNativeTdlibVersion() throws Exception {
        System.loadLibrary("tdjni");
        org.drinkless.tdlib.TdApi.Object result=org.drinkless.tdlib.Client.execute(new org.drinkless.tdlib.TdApi.GetOption("version"));
        assertTrue(result instanceof org.drinkless.tdlib.TdApi.OptionValueString);
        assertFalse(((org.drinkless.tdlib.TdApi.OptionValueString)result).value.isEmpty());
    }
    @Test public void testQrEncoderRoundTrip() throws Exception {
        String value="tirsolador-unit-test";Bitmap qr=QrCode.encode(value,256);int[] pixels=new int[256*256];qr.getPixels(pixels,0,256,0,0,256,256);
        com.google.zxing.BinaryBitmap bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(new com.google.zxing.RGBLuminanceSource(256,256,pixels)));
        assertEquals(value,new com.google.zxing.MultiFormatReader().decode(bitmap).getText());qr.recycle();
    }
    @Test public void testSourceRoutingAndDocumentIsolation() throws Exception {
        assertEquals(Console.N64,TelegramCatalog.consoleFor("TD - ROMs - N64"));
        assertEquals(Console.NDS,TelegramCatalog.consoleFor("TD - ROMs - DS"));
        assertEquals(Console.MD,TelegramCatalog.consoleFor("TD - ROMs - Genesis"));
        assertNull(TelegramCatalog.consoleFor("TD - Photos"));assertNull(TelegramCatalog.consoleFor("TD - ROMs - GameCube"));
        assertEquals("DD-ROMS-GBA",TelegramCatalog.expectedTitle(Console.GBA));
        org.drinkless.tdlib.TdApi.Chat chat=new org.drinkless.tdlib.TdApi.Chat();chat.type=new org.drinkless.tdlib.TdApi.ChatTypePrivate();
        assertFalse(TelegramCatalog.group(chat));chat.type=new org.drinkless.tdlib.TdApi.ChatTypeSupergroup();assertTrue(TelegramCatalog.group(chat));
        org.drinkless.tdlib.TdApi.Message message=new org.drinkless.tdlib.TdApi.Message();message.chatId=-123;message.id=456;
        org.drinkless.tdlib.TdApi.MessageDocument content=new org.drinkless.tdlib.TdApi.MessageDocument();
        content.document=new org.drinkless.tdlib.TdApi.Document();content.document.fileName="Super Mario 64 (USA).z64";
        content.document.document=new org.drinkless.tdlib.TdApi.File();content.document.document.id=22;content.document.document.size=100;
        message.content=content;
        Game game=TelegramCatalog.game(77,Console.N64,message);assertNotNull(game);assertEquals(-123,game.chatId);assertEquals(77,game.telegramAccount);
        assertNull(TelegramCatalog.game(77,Console.NES,message));
        assertEquals(game.id,Game.fromJson(game.toJson()).id);
    }
    @Test public void testProtectedDeviceStorage() throws Exception {
        SecretStore store=new SecretStore(rule.getActivity());store.put("testCredential","unit-test-value");
        assertEquals("unit-test-value",store.get("testCredential"));assertArrayEquals(store.databaseKey(),store.databaseKey());
        String raw=rule.getActivity().getSharedPreferences("secrets",0).getString("testCredential","");assertFalse(raw.contains("unit-test-value"));
        assertFalse(TirsoDrive.validHash("not-a-hash"));
    }
    @Test public void testNativeClientLifecycleBeforeAuthorization() throws Exception {
        System.loadLibrary("tdjni");
        java.util.concurrent.CountDownLatch waiting=new java.util.concurrent.CountDownLatch(1),closed=new java.util.concurrent.CountDownLatch(1);
        org.drinkless.tdlib.Client client=org.drinkless.tdlib.Client.create(object->{
            if(object instanceof org.drinkless.tdlib.TdApi.UpdateAuthorizationState){
                org.drinkless.tdlib.TdApi.AuthorizationState state=((org.drinkless.tdlib.TdApi.UpdateAuthorizationState)object).authorizationState;
                if(state instanceof org.drinkless.tdlib.TdApi.AuthorizationStateWaitTdlibParameters)waiting.countDown();
                if(state instanceof org.drinkless.tdlib.TdApi.AuthorizationStateClosed)closed.countDown();
            }
        },null,null);
        try {
            client.send(new org.drinkless.tdlib.TdApi.GetAuthorizationState(),object->{if(object instanceof org.drinkless.tdlib.TdApi.AuthorizationStateWaitTdlibParameters)waiting.countDown();});
            assertTrue("Native client did not reach parameter state",waiting.await(10,java.util.concurrent.TimeUnit.SECONDS));
        }finally{
            client.send(new org.drinkless.tdlib.TdApi.Close(),null);
            assertTrue("Native client did not close",closed.await(10,java.util.concurrent.TimeUnit.SECONDS));
        }
    }
    @Test public void testFirstRunConfigurationDoesNotFabricateQr() throws Throwable {
        MainActivity activity=rule.getActivity();
        rule.runOnUiThread(()->{
            activity.getWindow().getDecorView().findViewWithTag("nav_settings").performClick();
            activity.getWindow().getDecorView().findViewWithTag("telegram").performClick();
            assertNotNull(activity.getWindow().getDecorView().findViewWithTag("configure_credentials"));
        });
        capture("05-welcome-configuration");
        rule.runOnUiThread(()->activity.getWindow().getDecorView().findViewWithTag("continue_offline").performClick());
    }
}
