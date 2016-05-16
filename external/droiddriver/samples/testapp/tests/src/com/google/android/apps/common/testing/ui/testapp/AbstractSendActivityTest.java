package com.google.android.apps.common.testing.ui.testapp;

import android.test.ActivityInstrumentationTestCase2;

import com.google.android.droiddriver.finders.By;
import com.google.android.droiddriver.finders.XPaths;
import com.google.android.droiddriver.DroidDriver;

/**
 * Base class for testing SendActivity.
 */
// google3/javatests/com/google/android/apps/common/testing/ui/espresso/exampletest/ExampleTest.java
public abstract class AbstractSendActivityTest extends
    ActivityInstrumentationTestCase2<SendActivity> {

  private DroidDriver driver;
  private SendActivity activity;

  public AbstractSendActivityTest() {
    super(SendActivity.class);
  }

  protected abstract DroidDriver getDriver();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (driver == null) {
      driver = getDriver();
    }
    activity = getActivity();
  }

  public void testClick() {
    driver.on(By.text(activity.getString(R.string.button_send))).click();
    assertTrue(driver.on(By.text(getDisplayTitle())).isVisible());
  }

  public void testClickXPath() {
    driver.on(By.xpath("//ScrollView//Button")).click();
    assertTrue(driver.on(By.xpath("//TextView" + XPaths.text(getDisplayTitle()))).isVisible());
  }

  private String getDisplayTitle() {
    return activity.getString(R.string.display_title);
  }
}
