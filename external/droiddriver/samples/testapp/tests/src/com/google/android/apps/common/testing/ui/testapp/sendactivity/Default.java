package com.google.android.apps.common.testing.ui.testapp.sendactivity;

import com.google.android.apps.common.testing.ui.testapp.AbstractSendActivityTest;
import com.google.android.droiddriver.DroidDriverBuilder;
import com.google.android.droiddriver.DroidDriver;

public class Default extends AbstractSendActivityTest {
  @Override
  protected DroidDriver getDriver() {
    return new DroidDriverBuilder(getInstrumentation()).build();
  }
}