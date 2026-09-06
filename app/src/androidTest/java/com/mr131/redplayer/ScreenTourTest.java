package com.mr131.redplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public final class ScreenTourTest {
    private static final String PACKAGE = "com.mr131.redplayer";
    private static final long START_TIMEOUT_MS = 45_000;
    private static final long SCREEN_TIMEOUT_MS = 15_000;

    private UiDevice device;
    private File output;

    @Before
    public void launchRealApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = target.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        assertNotNull("No launcher activity for " + PACKAGE, launch);
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        target.startActivity(launch);
        assertTrue("Home screen did not become ready",
                device.wait(Until.hasObject(By.res(PACKAGE, "root")), START_TIMEOUT_MS));
        device.waitForIdle();
        output = new File(target.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshots");
        assertTrue("Could not create screenshot directory", output.isDirectory() || output.mkdirs());
    }

    @Test
    public void captureEveryImplementedScreen() {
        capture("01-home", By.res(PACKAGE, "root"), true);
        clickResource("settingsButton");
        capture("02-settings", By.text("Settings"), true);
        device.pressBack();
        waitFor(By.res(PACKAGE, "root"), "home after Settings");
        clickResource("moreButton");
        capture("03-more-menu", By.text("Private vault"), true);
        device.pressBack();
        clickResource("moreButton");
        waitFor(By.text("Private vault"), "Private vault menu item").click();
        capture("04-private-vault", By.desc("Private Vault"), true);
        device.pressBack();
        clickResource("searchButton");
        capture("05-search", By.text("Search videos"), true);
        device.pressBack();
        clickResource("speedButton");
        capture("06-playback-speed", By.text("Playback speed"), true);
        device.pressBack();
        clickResource("networkButton");
        capture("07-network-stream", By.text("Open Network Stream"), true);
        device.pressBack();
        clickResource("cloudButton");
        capture("08-cloud-import", By.desc("Cloud Import Screen 17"), true);
        device.pressBack();
        clickResource("openButton");
        capture("09-video-library", By.desc("Video Library"), true);
        waitFor(By.text("BROWSE FILES"), "Browse files").click();
        capture("10-system-video-picker", By.pkg("com.google.android.documentsui"), false);
    }

    private void clickResource(String id) {
        waitFor(By.res(PACKAGE, id), id).click();
        device.waitForIdle();
    }

    private UiObject2 waitFor(BySelector selector, String label) {
        UiObject2 object = device.wait(Until.findObject(selector), SCREEN_TIMEOUT_MS);
        assertNotNull("Screen marker did not appear: " + label, object);
        return object;
    }

    private void capture(String name, BySelector marker, boolean appMustBeForeground) {
        UiObject2 object = waitFor(marker, name);
        Rect bounds = object.getVisibleBounds();
        assertTrue("Marker is not visible for " + name, bounds.width() > 0 && bounds.height() > 0);
        if (appMustBeForeground) {
            assertEquals("Wrong foreground app for " + name, PACKAGE, device.getCurrentPackageName());
        }
        device.waitForIdle();
        assertTrue("Screenshot failed: " + name,
                device.takeScreenshot(new File(output, name + ".png")));
    }
}
