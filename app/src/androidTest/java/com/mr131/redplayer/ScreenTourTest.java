package com.mr131.redplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import androidx.core.content.FileProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public final class ScreenTourTest {
    private static final String PACKAGE = "com.mr131.redplayer";
    private static final long SCREEN_TIMEOUT_MS = 15_000;

    private UiDevice device;
    private File output;
    private ActivityScenario<MainActivity> homeScenario;

    @Before
    public void launchRealApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        homeScenario = ActivityScenario.launch(new Intent(Intent.ACTION_VIEW, installDemoVideo(target), target, MainActivity.class)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        homeScenario.onActivity(activity -> assertEquals("Home screen did not become ready",
                "131 Red Player Home Ready", activity.findViewById(R.id.root).getContentDescription()));
        device.waitForIdle();
        output = new File(target.getFilesDir(), "screenshots");
        assertTrue("Could not create screenshot directory", output.isDirectory() || output.mkdirs());
    }

    @Test
    public void captureEveryImplementedScreen() {
        captureHome();
        clickScrollableResource(R.id.subtitleButton);
        capture("18-subtitle-choices", By.text("Download matching subtitles"), true);
        device.pressBack();
        clickResource("settingsButton");
        capture("02-settings", By.text("Settings"), true);
        device.pressBack();
        waitFor(By.res(PACKAGE, "root"), "home after Settings");
        clickResource("moreButton");
        capture("03-more-menu", By.text("Private vault"), true);
        device.pressBack();
        clickResource("moreButton");
        click(By.text("Private vault"), "Private vault menu item");
        capture("04-private-vault", By.desc("Private Vault"), true);
        device.pressBack();
        clickResource("searchButton");
        capture("05-search", By.text("Search videos"), true);
        device.pressBack();
        clickScrollableResource(R.id.speedButton);
        capture("06-playback-speed", By.text("Playback speed"), true);
        device.pressBack();
        clickScrollableResource(R.id.networkButton);
        capture("07-network-stream", By.text("Open Network Stream"), true);
        device.pressBack();
        clickScrollableResource(R.id.cloudButton);
        capture("08-cloud-import", By.desc("Cloud Import Screen 17"), true);
        device.pressBack();
        clickResource("openButton");
        capture("09-video-library", By.desc("Video Library"), true);
        click(By.text("BROWSE FILES"), "Browse files");
        capture("10-system-video-picker", By.pkg("com.android.documentsui"), false);
        device.pressBack(); device.pressBack();
        startScreen(SubtitleDownloadActivity.class);
        capture("11-screen-08-subtitle-downloader", By.desc("Subtitle Downloader Screen 8"), true);
        startScreen(TechnicalInspectorActivity.class);
        capture("12-screen-12-technical-inspector", By.desc("Technical Inspector Screen 12"), true);
        startScreen(HistoryActivity.class);
        capture("13-screen-18-history-recovery", By.desc("History Screen 18"), true);
        startScreen(MainActivity.class); waitFor(By.desc("131 Red Player Home Ready"), "home restart");
        clickResource("moreButton"); click(By.text("Equalizer & Bass"), "equalizer menu");
        capture("14-equalizer-bass", By.text("Equalizer & Bass Boost"), true); device.pressBack();
        clickResource("moreButton"); click(By.text("Sleep timer"), "sleep timer menu");
        capture("15-sleep-timer", By.text("Sleep timer"), true); device.pressBack();
        clickResource("settingsButton"); click(By.text("Software decoder"), "decoder setting");
        capture("16-decoder-settings", By.text("Software decoder"), true); device.pressBack();
        startScreen(VaultActivity.class); waitFor(By.desc("Private Vault"), "vault"); click(By.text("CREATE VAULT PIN"), "create PIN");
        capture("17-vault-pin", By.text("Create vault PIN"), true);
    }

    private void startScreen(Class<?> screen) {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();Intent intent=new Intent(target,screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);target.startActivity(intent);device.waitForIdle();
    }

    private void startVideoHome() {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://example.com/test.mp4"),target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);target.startActivity(intent);device.waitForIdle();
    }

    private void clickResource(String id) {
        click(By.res(PACKAGE, id), id);
    }

    private void clickScrollableResource(int id) {
        onView(withId(id)).perform(scrollTo(), androidx.test.espresso.action.ViewActions.click());
        device.waitForIdle();
    }

    private void click(BySelector selector, String label) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                waitFor(selector, label).click();
                device.waitForIdle();
                return;
            } catch (StaleObjectException ignored) {
                device.waitForIdle();
            }
        }
        throw new AssertionError("Screen control stayed stale: " + label);
    }

    private UiObject2 waitFor(BySelector selector, String label) {
        UiObject2 object = device.wait(Until.findObject(selector), SCREEN_TIMEOUT_MS);
        assertNotNull("Screen marker did not appear: " + label, object);
        return object;
    }

    private void capture(String name, BySelector marker, boolean appMustBeForeground) {
        if (appMustBeForeground) ensureAppForeground(name);
        assertTrue("Screen marker did not appear: " + name,
                device.wait(Until.hasObject(marker), SCREEN_TIMEOUT_MS));
        device.waitForIdle();
        assertTrue("Screenshot failed: " + name, takeScreenshot(new File(output, name + ".png")));
    }

    private void captureHome() {
        homeScenario.onActivity(activity -> assertTrue("Home root is not visible",
                activity.findViewById(R.id.root).isShown()));
        ensureAppForeground("01-home");
        waitFor(By.text("demo.mp4"), "demo video title");
        waitFor(By.desc("Video Ready"), "first playable video frame");
        device.waitForIdle();
        assertTrue("Screenshot failed: 01-home", takeScreenshot(new File(output, "01-home.png")));
    }

    private Uri installDemoVideo(Context target) {
        File folder=new File(target.getCacheDir(),"proof-media");assertTrue(folder.isDirectory()||folder.mkdirs());File demo=new File(folder,"demo.mp4");
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("demo.mp4");FileOutputStream out=new FileOutputStream(demo)){byte[] buffer=new byte[16384];int read;while((read=in.read(buffer))>0)out.write(buffer,0,read);}catch(IOException error){throw new AssertionError("Could not install demo video",error);}
        return FileProvider.getUriForFile(target,target.getPackageName()+".files",demo);
    }

    private void ensureAppForeground(String screen) {
        long deadline = android.os.SystemClock.uptimeMillis() + 20_000;
        while (!PACKAGE.equals(device.getCurrentPackageName())
                && android.os.SystemClock.uptimeMillis() < deadline) {
            if ("android".equals(device.getCurrentPackageName())) {
                UiObject2 wait = device.findObject(By.res("android", "aerr_wait"));
                try {
                    if (wait != null) wait.click(); else device.pressBack();
                } catch (StaleObjectException ignored) {
                    // The platform refreshed the dialog; reacquire it on the next pass.
                }
            }
            android.os.SystemClock.sleep(500);
        }
        assertEquals("Wrong foreground app for " + screen, PACKAGE, device.getCurrentPackageName());
    }

    private boolean takeScreenshot(File destination) {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        if (bitmap == null) return false;
        try (FileOutputStream stream = new FileOutputStream(destination)) {
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (IOException error) {
            return false;
        } finally {
            bitmap.recycle();
        }
    }
}
