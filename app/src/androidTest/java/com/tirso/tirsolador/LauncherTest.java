package com.tirso.tirsolador;

import android.view.View;
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
}
