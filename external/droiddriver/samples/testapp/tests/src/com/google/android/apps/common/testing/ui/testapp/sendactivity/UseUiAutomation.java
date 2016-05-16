package com.google.android.apps.common.testing.ui.testapp.sendactivity;

import com.google.android.apps.common.testing.ui.testapp.AbstractSendActivityTest;
import com.google.android.droiddriver.DroidDriver;
import com.google.android.droiddriver.DroidDriverBuilder;
import com.google.android.droiddriver.DroidDriverBuilder.Implementation;

// Optional annotation to show filtering tests by build version.
@com.google.android.droiddriver.runner.UseUiAutomation
public class UseUiAutomation extends AbstractSendActivityTest {
  @Override
  protected DroidDriver getDriver() {
    return new DroidDriverBuilder(getInstrumentation()).use(Implementation.UI_AUTOMATION).build();
  }
}
