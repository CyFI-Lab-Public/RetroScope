/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.cts.uiautomatortest;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.android.uiautomator.core.UiCollection;
import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.core.UiWatcher;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Sanity test uiautomator functionality on target device.
 */
public class CtsUiAutomatorTest extends UiAutomatorTestCase {
    private static final String LOG_TAG = CtsUiAutomatorTest.class.getSimpleName();
    private static final String[] LIST_SCROLL_TESTS = new String[] {
            "Test 17", "Test 11", "Test 20", "Test 35"
    };
    private static final String LAUNCH_APP = "am start -a android.intent.action.MAIN"
            + " -n com.android.cts.uiautomator/.MainActivity -W";
    private static final String PKG_NAME = "com.android.cts.uiautomator";

    // Maximum wait for key object to become visible
    private static final int WAIT_EXIST_TIMEOUT = 5 * 1000;

    private static final String SCREEN_SHOT_FILE_PATH_NAME = "/data/local/tmp/ctsScreenShot";

    // Should match the value defined in UiObject
    private static final int FINGER_TOUCH_HALF_WIDTH = 20;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Make sure the test app is always running
        UiDevice.getInstance().waitForIdle();
        if (!new UiObject(new UiSelector().packageName(PKG_NAME)).exists())
            runShellCommand(LAUNCH_APP);
    }

    /**
     * Helper to execute a command on the shell
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void runShellCommand(String command) throws IOException, InterruptedException {
        Process p = null;
        BufferedReader resultReader = null;
        try {
            p = Runtime.getRuntime().exec(command);
            int status = p.waitFor();
            if (status != 0) {
                throw new RuntimeException(String.format("Run shell command: %s, status: %s",
                        command, status));
            }
        } finally {
            if (resultReader != null) {
                resultReader.close();
            }
            if (p != null) {
                p.destroy();
            }
        }
    }

    /*
     * Items in the listScrollTests array should be spread out such that a
     * scroll is required to reach each item at each of the far ends.
     */
    public void testListScrollAndSelect() throws UiObjectNotFoundException {
        UiScrollable listView = new UiScrollable(
                new UiSelector().className(android.widget.ListView.class.getName()));

        // on single fragment display
        if (!listView.exists())
            UiDevice.getInstance().pressBack();

        for (String test : LIST_SCROLL_TESTS) {
            openTest(test);
            verifyTestDetailsExists(test);
        }
    }

    /**
     * Test erasing of multi word text in edit field and input of new text. Test
     * verifying input text using a complex UiSelector
     *
     * @throws UiObjectNotFoundException
     */
    public void testTextEraseAndInput() throws UiObjectNotFoundException {
        String testText = "Android Ui Automator Input Text";
        openTest("Test 1");

        UiObject editText = new UiObject(new UiSelector().className(android.widget.EditText.class
                .getName()));
        editText.setText(testText);

        UiObject submitButton = new UiObject(new UiSelector()
                .className(android.widget.Button.class.getName()).clickable(true)
                .textStartsWith("Submit"));
        submitButton.click();

        UiObject result = new UiObject(new UiSelector().className(
                android.widget.LinearLayout.class.getName()).childSelector(
                (new UiSelector().className(android.widget.ScrollView.class.getName())
                        .childSelector(new UiSelector().className(android.widget.TextView.class
                                .getName())))));

        if (!testText.equals(result.getText())) {
            throw new UiObjectNotFoundException("Test text: " + testText);
        }

        getObjectByText("OK").click();
    }

    /**
     * Select each of the buttons by using only the content description property
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectByContentDescription() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByDescription("Button 1").click();
        verifyDialogActionResults("Button 1");
        getObjectByDescription("Button 2").click();
        verifyDialogActionResults("Button 2");
        getObjectByDescription("Button 3").click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Select each of the buttons by using only the text property
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectByText() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByText("Button 1").click();
        verifyDialogActionResults("Button 1");
        getObjectByText("Button 2").click();
        verifyDialogActionResults("Button 2");
        getObjectByText("Button 3").click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Select each of the buttons by using only the index property
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectByIndex() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByIndex(android.widget.Button.class.getName(), 0).click();
        verifyDialogActionResults("Button 1");
        getObjectByIndex(android.widget.Button.class.getName(), 1).click();
        verifyDialogActionResults("Button 2");
        getObjectByIndex(android.widget.Button.class.getName(), 2).click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Select each of the buttons by using only the instance number
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectByInstance() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByInstance(android.widget.Button.class.getName(), 0).click();
        verifyDialogActionResults("Button 1");
        getObjectByInstance(android.widget.Button.class.getName(), 1).click();
        verifyDialogActionResults("Button 2");
        getObjectByInstance(android.widget.Button.class.getName(), 2).click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Test when a node's state is changed due to an action, it is updated in the accessibility
     * hierarchy.
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectAfterContentChanged() throws UiObjectNotFoundException {
        openTest("Test 2");
        UiObject dynaButton = getObjectByText("Before");
        dynaButton.click();
        assertTrue("Button state change is not refreshed in accessibility hierarchy",
                getObjectByText("After").exists());
    }

    /**
     * Test opening the options menu using the soft buttons
     *
     * @throws UiObjectNotFoundException
     * @throws InterruptedException
     * @throws IOException
     */
    public void testDeviceSoftKeys() throws UiObjectNotFoundException, IOException,
            InterruptedException {
        openTest("Test 2");
        UiDevice device = UiDevice.getInstance();
        device.pressMenu();
        getObjectByText("Finish").click();
        verifyDialogActionResults("Finish");

        // Back button
        openTest("Test 1");
        UiObject editText = new UiObject(new UiSelector().className(android.widget.EditText.class
                .getName()));
        editText.setText("Android Geppetto Test Application");

        UiObject submitButton = new UiObject(new UiSelector()
                .className(android.widget.Button.class.getName()).clickable(true)
                .textStartsWith("Submit"));
        submitButton.click();

        // Text from the popup dialog
        UiObject result = new UiObject(new UiSelector().textContains("geppetto"));

        // Back button test to dismiss the dialog
        assertTrue("Wait for exist must return true", result.waitForExists(2000));
        device.pressBack();
        result.waitUntilGone(1000);
        assertFalse("Wait for exist must return false after press back", result.exists());

        // Home button test
        openTest("Test 5");
        String pkgName = device.getCurrentPackageName();
        assertTrue("CTS test app must be running", pkgName.equals(PKG_NAME));
        device.pressHome();
        boolean gone = new UiObject(new UiSelector().packageName(PKG_NAME)).waitUntilGone(5000);
        assertTrue("CTS test app still visble after pressing home", gone);
    }

    /**
     * This view is in constant update generating window content changed events.
     * The test will read the time displayed and exhaust each wait for idle
     * timeout until it read and sets the text back into the edit field and
     * presses submit. A dialog box should pop up with the time it took since
     * reading the value until pressing submit.
     *
     * @throws UiObjectNotFoundException
     */
    public void testWaitForIdleTimeout() throws UiObjectNotFoundException {
        openTest("Test 3");
        UiObject clk = new UiObject(new UiSelector().descriptionStartsWith("Performance "));

        // First default wait for idle timeout assumed to be 10 seconds
        String txtTime = clk.getText();
        UiObject edit = new UiObject(new UiSelector().className(android.widget.EditText.class
                .getName()));

        // Second default wait for idle timeout assumed to be 10 seconds.
        // Total ~20.
        edit.setText(txtTime);

        // Third default wait for idle timeout assumed to be 10 seconds.
        // Total ~30.
        getObjectByText("Submit").click();

        // The value read should have value between 30 and 60 seconds indicating
        // that the internal default timeouts for wait-for-idle is in acceptable
        // range.
        UiObject readTime = new UiObject(new UiSelector().className(
                android.widget.TextView.class.getName()).instance(1));
        String timeDiff = readTime.getText();
        Log.i(LOG_TAG, "Sync time: " + timeDiff);

        getObjectByText("OK").click();

        int totalDelay = Integer.parseInt(timeDiff);

        // Cumulative waits in this test should add up to at minimum 30 seconds
        assertFalse("Timeout for wait-for-idle is too short. Expecting minimum 30 seconds",
                totalDelay < 30 * 1000);

        // allow for tolerance in time measurements due to differences between
        // device speeds
        assertFalse("Timeout for wait-for-idle is too long. Expecting maximum 60 seconds",
                totalDelay > 60 * 1000);
    }

    /**
     * This view is in constant update generating window content changed events.
     * This test uses the soft key presses and clicks while the background
     * screen is constantly updating causing a constant busy state.
     *
     * @throws UiObjectNotFoundException
     */
    public void testVerifyMenuClicks() throws UiObjectNotFoundException {
        openTest("Test 3");
        UiDevice.getInstance().pressMenu();
        new UiObject(new UiSelector().text("Submit")).click();
        verifyDialogActionResults("Submit");
        UiDevice.getInstance().pressMenu();
        new UiObject(new UiSelector().text("Exit")).click();
        verifyDialogActionResults("Exit");
    }

    /**
     * Verifies swipeRight, swipeLeft and raw swipe APIs perform as expected.
     *
     * @throws UiObjectNotFoundException
     */
    public void testSwipes() throws UiObjectNotFoundException {
        openTest("Test 4");
        UiObject textView = new UiObject(new UiSelector().textContains("["));

        textView.swipeLeft(10);
        assertTrue("UiObject swipe left 1->2", "[ 2 ]".equals(textView.getText()));

        textView.swipeLeft(10);
        assertTrue("UiObject swipe left 2->3", "[ 3 ]".equals(textView.getText()));

        textView.swipeLeft(10);
        assertTrue("UiObject swipe left 3->4", "[ 4 ]".equals(textView.getText()));

        textView.swipeRight(10);
        assertTrue("UiObject swipe right 3<-4", "[ 3 ]".equals(textView.getText()));

        textView.swipeRight(10);
        assertTrue("UiObject swipe right 2<-3", "[ 2 ]".equals(textView.getText()));

        textView.swipeRight(10);
        assertTrue("UiObject swipe right 1<-2", "[ 1 ]".equals(textView.getText()));

        Rect tb = textView.getBounds();
        UiDevice.getInstance().swipe(tb.right - 20, tb.centerY(), tb.left + 20, tb.centerY(), 50);

        SystemClock.sleep(100);
        assertTrue("UiDevice raw swipe 1->2", "[ 2 ]".equals(textView.getText()));
    }

    /**
     * Creates a complex selector
     *
     * @throws UiObjectNotFoundException
     */
    public void testComplexSelectors() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector frameLayout = new UiSelector().className(android.widget.FrameLayout.class
                .getName());
        UiSelector gridLayout = new UiSelector().className(android.widget.GridLayout.class
                .getName());
        UiSelector toggleButton = new UiSelector().className(android.widget.ToggleButton.class
                .getName());
        UiObject button = new UiObject(frameLayout.childSelector(gridLayout).childSelector(
                toggleButton));

        assertTrue("Toggle button value should be OFF", "OFF".equals(button.getText()));
        button.click();
        assertTrue("Toggle button value should be ON", "ON".equals(button.getText()));
        button.click();
        assertTrue("Toggle button value should be OFF", "OFF".equals(button.getText()));
    }

    /**
     * Test when an object does not exist, an exception is thrown
     * @throws UiObjectNotFoundException
     */
    public void testExceptionObjectNotFound() throws UiObjectNotFoundException {
        UiSelector selector = new UiSelector().text("Nothing should be found");
        UiSelector child = new UiSelector().className("Nothing");
        UiObject obj = new UiObject(selector.childSelector(child));

        assertFalse("Object is reported as existing", obj.exists());

        try {
            obj.click();
        } catch (UiObjectNotFoundException e) {
            return;
        }
        assertTrue("Exception not thrown for Object not found", false);
    }

    /**
     * Verifies the UiWatcher registration and trigger function
     *
     * @throws UiObjectNotFoundException
     */
    public void testUiWatcher() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiDevice device = UiDevice.getInstance();
        device.registerWatcher("Artificial crash", new UiWatcher() {

            @Override
            public boolean checkForCondition() {
                if (new UiObject(new UiSelector().packageName("android")).exists()) {
                    try {
                        // Expecting a localized OK button
                        new UiObject(new UiSelector().className(
                                android.widget.Button.class.getName()).enabled(true)).click();
                    } catch (UiObjectNotFoundException e) {
                    }
                    return true;
                }
                return false;
            }
        });

        // Causes a runtime exception to be thrown
        getObjectByText("Button").click();

        // Fake doing something while the exception is being displayed
        SystemClock.sleep(2000);
        device.runWatchers();
        assertTrue("UiWatcher not triggered", device.hasAnyWatcherTriggered());
    }

    /**
     * Verifies the 'checked' property of both UiSelector and UiObject
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorChecked() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiObject checkboxChecked = new UiObject(new UiSelector().className(
                android.widget.CheckBox.class.getName()).checked(true));
        UiObject checkboxNotChecked = new UiObject(new UiSelector().className(
                android.widget.CheckBox.class.getName()).checked(false));

        checkboxNotChecked.click();
        assertTrue("Checkbox should be checked", checkboxChecked.isChecked());
        checkboxChecked.click();
        assertFalse("Checkbox should be unchecked", checkboxNotChecked.isChecked());
    }

    /**
     * Verifies the 'Clickable' property of both the UiSelector and UiObject
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorClickable() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector clickableCheckbox = new UiSelector().clickable(true).className(
                android.widget.CheckBox.class.getName());
        UiSelector notClickableProgress = new UiSelector().clickable(false).className(
                android.widget.ProgressBar.class.getName());

        assertTrue("Selector clickable", new UiObject(clickableCheckbox).isClickable());
        assertFalse("Selector not clickable", new UiObject(notClickableProgress).isClickable());
    }

    /**
     * Verifies the 'focusable' property of both UiSelector and UiObject
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorFocusable() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector mainLayout = new UiSelector().description("Widgets Collection");
        UiSelector focusableCheckbox = mainLayout.childSelector(new UiSelector().className(
                android.widget.CheckBox.class.getName()).focusable(true));
        UiSelector notFocusableSpinner = mainLayout.childSelector(new UiSelector().className(
                android.widget.Spinner.class.getName()).focusable(false));

        assertTrue("Selector focusable", new UiObject(focusableCheckbox).isFocusable());
        assertFalse("Selector not focusable", new UiObject(notFocusableSpinner).isFocusable());
    }

    /**
     * Verifies the 'DescriptionContains' property of UiSelector
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorDescriptionContains() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector progressDescriptionContains = new UiSelector().descriptionContains("%");
        assertTrue("Selector descriptionContains", "Progress is 50 %".equals(new UiObject(
                progressDescriptionContains).getContentDescription()));
    }

    /**
     * Verifies the 'DescriptionStarts' property of UiSelector
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorDescriptionStarts() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector progressDescriptionStart = new UiSelector().descriptionStartsWith("progress");
        assertTrue("Selector descriptionStart", "Progress is 50 %".equals(new UiObject(
                progressDescriptionStart).getContentDescription()));
    }

    /**
     * Verifies the 'Enabled' property of both UiSelector and UiObject
     *
     * @throws UiObjectNotFoundException
     */
    public void testSelectorEnabled() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector mainLayout = new UiSelector().description("Widgets Collection");
        UiSelector buttonDisabled = mainLayout.childSelector(new UiSelector().className(
                android.widget.Button.class.getName()).enabled(false));
        UiSelector buttonEnabled = mainLayout.childSelector(new UiSelector().className(
                android.widget.Button.class.getName()).enabled(true));

        assertFalse("Selector enabled false", new UiObject(buttonDisabled).isEnabled());
        assertTrue("Selector enabled true", new UiObject(buttonEnabled).isEnabled());
    }

    /**
     * Verifies the UiCollection object child counting by object pattern
     *
     * @throws UiObjectNotFoundException
     */
    public void testCollectionCount() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiCollection collection = new UiCollection(
                new UiSelector().description("Widgets Collection"));
        assertTrue("Collection layout not found", collection.waitForExists(WAIT_EXIST_TIMEOUT));

        assertTrue("Collection count",
                collection.getChildCount(new UiSelector().clickable(true)) == 6);
    }

    /**
     * Verifies the UiCollection can find an object by text and returning by
     * pattern
     *
     * @throws UiObjectNotFoundException
     */
    public void testCollectionGetChildByText() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiCollection collection = new UiCollection(
                new UiSelector().description("Widgets Collection"));
        assertTrue("Collection layout not found", collection.waitForExists(WAIT_EXIST_TIMEOUT));

        UiObject item = collection.getChildByText(
                new UiSelector().className(android.widget.Button.class.getName()), "Button");

        assertTrue("Collection get child by text", "Button".equals(item.getText()));
    }

    /**
     * Verifies the UiCollection can find an object by instance and returning by
     * pattern
     *
     * @throws UiObjectNotFoundException
     */
    public void testCollectionGetChildByInstance() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiCollection collection = new UiCollection(
                new UiSelector().description("Widgets Collection"));
        assertTrue("Collection layout not found", collection.waitForExists(WAIT_EXIST_TIMEOUT));

        // find the second button
        UiObject item = collection.getChildByInstance(
                new UiSelector().className(android.widget.Button.class.getName()), 1);

        assertTrue("Collection get child by instance", "Button".equals(item.getText()));
    }

    /**
     * Verifies the UiCollection can find an object by description and returning
     * by pattern
     *
     * @throws UiObjectNotFoundException
     */
    public void testCollectionGetChildByDescription() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiCollection collection = new UiCollection(
                new UiSelector().description("Widgets Collection"));
        assertTrue("Collection layout not found", collection.waitForExists(WAIT_EXIST_TIMEOUT));

        UiObject item = collection.getChildByDescription(
                new UiSelector().className(android.widget.Button.class.getName()),
                "Description for Button");

        assertTrue("Collection get child by description", "Button".equals(item.getText()));
    }

    /**
     * Test Orientation APIs by causing rotations and verifying current state
     *
     * @throws RemoteException
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testRotation() throws RemoteException, UiObjectNotFoundException {
        openTest("Test 5");
        UiDevice device = UiDevice.getInstance();

        device.setOrientationLeft();
        device.waitForIdle(); // isNaturalOrientation is not waiting for idle
        SystemClock.sleep(1000);
        assertFalse("Device orientation should not be natural", device.isNaturalOrientation());

        device.setOrientationNatural();
        device.waitForIdle(); // isNaturalOrientation is not waiting for idle
        SystemClock.sleep(1000);
        assertTrue("Device orientation should be natural", device.isNaturalOrientation());

        device.setOrientationRight();
        device.waitForIdle(); // isNaturalOrientation is not waiting for idle
        SystemClock.sleep(1000);
        assertFalse("Device orientation should not be natural", device.isNaturalOrientation());

        device.setOrientationNatural();
    }

    /**
     * Reads the current device's product name. Since it is not possible to predetermine the
     * would be value, the check verifies that the value is not null and not empty.
     *
     * @since API Level 17
     */
    public void testGetProductName() {
        String name = UiDevice.getInstance().getProductName();
        assertFalse("Product name check returned empty string", name.isEmpty());
    }

    /**
     * Select each of the buttons by using only regex text
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectByTextMatch() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByTextMatch(".*n\\s1$").click();
        verifyDialogActionResults("Button 1");
        getObjectByTextMatch(".*n\\s2$").click();
        verifyDialogActionResults("Button 2");
        getObjectByTextMatch(".*n\\s3$").click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Select each of the buttons by using only regex content-description
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectByDescriptionMatch() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByDescriptionMatch(".*n\\s1$").click();
        verifyDialogActionResults("Button 1");
        getObjectByDescriptionMatch(".*n\\s2$").click();
        verifyDialogActionResults("Button 2");
        getObjectByDescriptionMatch(".*n\\s3$").click();
        verifyDialogActionResults("Button 3");
    }

    /**
     * Select each of the buttons by using only regex class name
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectByClassMatch() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiObject tgl = getObjectByClassMatch(".*ToggleButton$", 0);
        String tglValue = tgl.getText();
        tgl.click();

        assertFalse("Matching class by Regex failed", tglValue.equals(tgl.getText()));
    }

    /**
     * Select each of the buttons by using only class type
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectByClassType() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiObject tgl = getObjectByClass(android.widget.ToggleButton.class, 0);
        String tglValue = tgl.getText();
        tgl.click();

        assertFalse("Matching class by class type failed", tglValue.equals(tgl.getText()));
    }

    /**
     * Test the coordinates of 3 buttons side by side verifying vertical and
     * horizontal coordinates.
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testGetVisibleBounds() throws UiObjectNotFoundException {
        openTest("Test 2");
        Rect rect1 = getObjectByText("Button 1").getVisibleBounds();
        Rect rect2 = getObjectByText("Button 2").getVisibleBounds();
        Rect rect3 = getObjectByText("Button 3").getVisibleBounds();

        assertTrue("X coordinate check failed",
                rect1.left < rect2.left && rect2.right < rect3.right);
        assertTrue("Y coordinate check failed",
                rect1.top == rect2.top && rect2.bottom == rect3.bottom);
    }

   /**
     * Tests the LongClick functionality in the API
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectorLongClickable() throws UiObjectNotFoundException {
        openTest("Test 2");
        getObjectByText("Button 1").longClick();
        verifyDialogActionResults("Longclick Button 1");
    }

    /**
     * Test the UiSelector's long-clickable property
     *
     * @throws UiObjectNotFoundException
     * @since API Level 17
     */
    public void testSelectorLongClickableProperty() throws UiObjectNotFoundException {
        openTest("Test 2");
        UiObject button3 = new UiObject(new UiSelector().className(
                android.widget.Button.class).longClickable(true).instance(2));
        button3.longClick();
        verifyDialogActionResults("Longclick Button 3");
    }

    /**
     * Takes a screen shot of the current display and checks if the file is
     * created and is not zero size.
     *
     * @since API Level 17
     */
    public void testTakeScreenShots() {
        File storePath = new File(SCREEN_SHOT_FILE_PATH_NAME);
        getUiDevice().takeScreenshot(storePath);

        assertTrue("Screenshot file not detected in store", storePath.exists());
        assertTrue("Zero size for screenshot file", storePath.length() > 0);
    }

    /**
     * Verifies the 'Resource-Id' property of UiSelector
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
    public void testSelectorResourceId() throws UiObjectNotFoundException {
        openTest("Test 5");
        UiSelector toggleSelector =
                new UiSelector().resourceId("com.android.cts.uiautomator:id/test_5_toggleButton");
        UiObject toggleButton = new UiObject(toggleSelector);
        assertTrue("Object with selector resource-id not found", toggleButton.exists());
        assertTrue("Incorrect object for selector resource-id returned",
                "OFF".equals(toggleButton.getText()) || "ON".equals(toggleButton.getText()));
    }

    /**
     * Verify the UiSelector property resourceIdMatches
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
    public void testSelectorResourceIdMatches() throws UiObjectNotFoundException {
        openTest("Test 2");
        new UiObject(new UiSelector().resourceIdMatches("(?i).*button.*").instance(2)).click();
        verifyDialogActionResults("Button 3");
        new UiObject(new UiSelector().resourceIdMatches("(?i).*button1.*")).click();
        verifyDialogActionResults("Button 1");
    }

    /**
     * Performs a pinch out from the center of a view to its edges and listens to
     * the motion events to make sure the starting and ending points of both pointers
     * are correct.
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
    public void testPinchOut() throws UiObjectNotFoundException {
        openTest("Test 12");

        UiObject screen = new UiObject(
                new UiSelector().description("Details View"));

        // get the current view dimensions
        Rect screenRect = screen.getBounds();

        // perform the pinch for 100% of the view dimensions starting form
        // the center out to the edges.
        screen.pinchOut(100, 30);

        // dialog with the detected pointers motion coordinates is displayed.
        UiObject results = new UiObject(new UiSelector().className(
                android.widget.ScrollView.class).childSelector(new UiSelector().className(
                        android.widget.TextView.class)));
        String allPointers = results.getText();
        new UiObject(new UiSelector().text("OK")).click(); // dismiss dialog

        // parse pointer 1
        Point p1s = parsePointerCoordinates(allPointers, 0, 0); // start
        Point p1e = parsePointerCoordinates(allPointers, 0, 1); // end
        // parse pointer 2
        Point p2s = parsePointerCoordinates(allPointers, 1, 0); // start
        Point p2e = parsePointerCoordinates(allPointers, 1, 1); // end

        assertTrue("All Y axis coordinates for pointer 1 must be the same", p1s.y == p1e.y);
        assertTrue("All Y axis coordinates for pointer 2 must be the same", p2s.y == p2e.y);
        assertTrue("All Y axis coordinates for both pointers must be the same", p1s.y == p2s.y);
        assertTrue("Pinch must be in center of target view", p2s.y == screenRect.centerY());

        assertTrue("Touch-down X coordinate for pointer 1 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX(), p1s.x));

        assertTrue("Touch-down X coordinate for pointer 2 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX(), p2s.x));

        assertTrue("Touch-up X coordinate for pointer 1 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX() - screenRect.left,
                        screenRect.centerX() - p1e.x));

        assertTrue("Touch-up X coordinate for pointer 2 is invalid",
                withinMarginOfError(0.1f, screenRect.right, p2e.x));
    }

    /**
     * Performs a pinch in from the edges of a view to its center and listens to
     * the motion events to make sure the starting and ending points of both pointers
     * are correct.
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
    public void testPinchIn() throws UiObjectNotFoundException {
        openTest("Test 12");

        UiObject screen = new UiObject(
                new UiSelector().description("Details View"));

        // get the current view dimensions
        Rect screenRect = screen.getBounds();

        // perform the pinch for 100% of the view dimensions starting form
        // the edges in towards the center.
        screen.pinchIn(100, 30);

        // dialog with the detected pointers motion coordinates is displayed.
        UiObject results = new UiObject(new UiSelector().className(
                android.widget.ScrollView.class).childSelector(new UiSelector().className(
                        android.widget.TextView.class)));
        String allPointers = results.getText();
        new UiObject(new UiSelector().text("OK")).click(); // dismiss dialog

        // parse pointer 1
        Point p1s = parsePointerCoordinates(allPointers, 0, 0); // start
        Point p1e = parsePointerCoordinates(allPointers, 0, 1); // end
        // parse pointer 2
        Point p2s = parsePointerCoordinates(allPointers, 1, 0); // start
        Point p2e = parsePointerCoordinates(allPointers, 1, 1); // end

        assertTrue("All Y axis coordinates for pointer 1 must be the same", p1s.y == p1e.y);
        assertTrue("All Y axis coordinates for pointer 2 must be the same", p2s.y == p2e.y);
        assertTrue("All Y axis coordinates for both pointers must be the same", p1s.y == p2s.y);
        assertTrue("Pinch must be in center of target view", p2s.y == screenRect.centerY());

        assertTrue("Touch-down X coordinate for pointer 1 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX() - screenRect.left,
                        screenRect.centerX() -  p1s.x));

        assertTrue("Touch-down X coordinate for pointer 2 is invalid",
                withinMarginOfError(0.1f, screenRect.right, p2s.x));

        assertTrue("Touch-up X coordinate for pointer 1 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX() - FINGER_TOUCH_HALF_WIDTH, p1e.x));

        assertTrue("Touch-up X coordinate for pointer 2 is invalid",
                withinMarginOfError(0.1f, screenRect.centerX() + FINGER_TOUCH_HALF_WIDTH, p2e.x));
    }

    /**
     * Performs a drag and drop operation from one UiObject to another UiObject
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
    public void testDragToObject() throws UiObjectNotFoundException {
        openTest("Test 5");

        UiObject imageButton = new UiObject(new UiSelector().description("Image button"));
        UiObject starsBar = new UiObject(new UiSelector().className(android.widget.RatingBar.class));

        Rect starsBarRect = starsBar.getBounds();
        Rect imageButtonRect = imageButton.getBounds();
        imageButton.dragTo(starsBar, 30);

        // dialog with the detected pointers motion coordinates is displayed.
        UiObject results = new UiObject(new UiSelector().className(
                android.widget.ScrollView.class).childSelector(new UiSelector().className(
                        android.widget.TextView.class)));
        String allPointers = results.getText();
        new UiObject(new UiSelector().text("OK")).click(); // dismiss dialog

        // parse pointer 1
        Point p1s = parsePointerCoordinates(allPointers, 0, 0); // start
        Point p1e = parsePointerCoordinates(allPointers, 0, 1); // end

        assertTrue("Invalid touch starting.X reported",
                withinMarginOfError(0.05f, imageButtonRect.centerX(), p1s.x));
        assertTrue("Invalid touch starting.Y reported",
                withinMarginOfError(0.05f, imageButtonRect.centerY(), p1s.y));
        assertTrue("Invalid touch ending.X reported",
                withinMarginOfError(0.05f, starsBarRect.centerX(), p1e.x));
        assertTrue("Invalid touch ending.Y reported",
                withinMarginOfError(0.05f, starsBarRect.centerY(), p1e.y));
    }

    /**
     * Performs a drag and drop operation from one UiObject to a specified coordinates
     *
     * @throws UiObjectNotFoundException
     * @since API Level 18
     */
   public void testDragToCoordinates() throws UiObjectNotFoundException {
       openTest("Test 5");

       UiObject imageButton = new UiObject(new UiSelector().description("Image button"));
       UiObject starsBar = new UiObject(new UiSelector().className(android.widget.RatingBar.class));

       Rect starsBarRect = starsBar.getBounds();
       Rect imageButtonRect = imageButton.getBounds();
       imageButton.dragTo(starsBarRect.centerX(), starsBarRect.centerY(), 30);

       // dialog with the detected pointers motion coordinates is displayed.
       UiObject results = new UiObject(new UiSelector().className(
               android.widget.ScrollView.class).childSelector(new UiSelector().className(
                       android.widget.TextView.class)));
       String allPointers = results.getText();
       new UiObject(new UiSelector().text("OK")).click(); // dismiss dialog

       // parse pointer 1
       Point p1s = parsePointerCoordinates(allPointers, 0, 0); // start
       Point p1e = parsePointerCoordinates(allPointers, 0, 1); // end

       assertTrue("Invalid touch starting.X reported",
               withinMarginOfError(0.05f, imageButtonRect.centerX(), p1s.x));
       assertTrue("Invalid touch starting.Y reported",
               withinMarginOfError(0.05f, imageButtonRect.centerY(), p1s.y));
       assertTrue("Invalid touch ending.X reported",
               withinMarginOfError(0.05f, starsBarRect.centerX(), p1e.x));
       assertTrue("Invalid touch ending.Y reported",
               withinMarginOfError(0.05f, starsBarRect.centerY(), p1e.y));
   }

   /**
    * Detect if actual value is within the allowable margin of error of the expected value.
    *
    * Used essentially with actual values that may vary from the expected values such in the
    * cases of touch and pinch and touch and swipe where the starting or ending points may
    * not exactly match the expected value.
    *
    * @param marginPrecent is values between 0 and 1
    * @param expected
    * @param actual
    * @return true if actual is within the allowed range from expected
    */
   private boolean withinMarginOfError(float marginPrecent, int expected, int actual) {
       int m = (int) (marginPrecent * expected);
       return actual >= expected - m && actual <= expected + m;
   }

   /**
     * Parses a string containing starting to ending coordinates of one or more pointers.
     *
     * @param allPointers is a raw string with coordinates from all detected pointers
     * @param pointerNumber is the desired pointer to be parsed
     * @param edge is the 0 for the start or 1 for the end of the swipe
     * @return Point containing the start or end coordinates of the specified pointer number
     */
    private Point parsePointerCoordinates(String allPointers, int pointerNumber, int edge) {
        String pointers[] = allPointers.split("\n");
        String coordinates = pointers[pointerNumber].split(":")[edge];
        String xy[] = coordinates.split(",");
        return new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
    }

    /**
     * Private helper to open test views. Also covers UiScrollable tests
     *
     * @param name
     * @throws UiObjectNotFoundException
     */
    private void openTest(String name) throws UiObjectNotFoundException {
        try {
            UiDevice.getInstance().setOrientationNatural();
        } catch (RemoteException e) {
            // will catch it in its own test. For now try to put the device
            // in its natural orientation prior to each test
        }
        UiScrollable listView = new UiScrollable(
                new UiSelector().className(android.widget.ListView.class.getName()));

        // on single fragment display
        if (!listView.exists())
            UiDevice.getInstance().pressBack();

        UiObject testItem = listView.getChildByText(
                new UiSelector().className(android.widget.TextView.class.getName()), name);

        testItem.click();
    }

    private void verifyTestDetailsExists(String name) throws UiObjectNotFoundException {
        // verify that we're at the right test
        new UiObject(new UiSelector().description("Details").text(name)).getText();
    }

    private UiObject getObjectByText(String txt) {
        return new UiObject(new UiSelector().text(txt));
    }

    private UiObject getObjectByTextMatch(String regex) {
        return new UiObject(new UiSelector().textMatches(regex));
    }

    private UiObject getObjectByDescriptionMatch(String regex) {
        return new UiObject(new UiSelector().descriptionMatches(regex));
    }

    private UiObject getObjectByDescription(String txt) {
        return new UiObject(new UiSelector().description(txt));
    }

    private UiObject getObjectByClassMatch(String regex, int instance) {
        return new UiObject(new UiSelector().classNameMatches(regex).instance(instance));
    }

    private <T> UiObject getObjectByClass(Class<T> type, int instance) {
        return new UiObject(new UiSelector().className(type).instance(instance));
    }

    private UiObject getObjectByIndex(String className, int index) {
        return new UiObject(new UiSelector().className(className).index(index));
    }

    private UiObject getObjectByInstance(String className, int instance) {
        return new UiObject(new UiSelector().className(className).instance(instance));
    }

    private void verifyDialogActionResults(String txt) throws UiObjectNotFoundException {
        if (!getObjectByText("Action results").exists() || !getObjectByText(txt).exists()) {
            throw new UiObjectNotFoundException(txt);
        }
        getObjectByText("OK").click();
    }
}
