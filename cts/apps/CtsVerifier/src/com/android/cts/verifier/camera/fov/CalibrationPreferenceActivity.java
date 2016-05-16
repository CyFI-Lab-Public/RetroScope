// Copyright 2012 Google Inc. All Rights Reserved.

package com.android.cts.verifier.camera.fov;

import com.android.cts.verifier.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Preferences for the LightCycle calibration app.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class CalibrationPreferenceActivity extends PreferenceActivity {
  public static final String OPTION_MARKER_DISTANCE = "markerDistance";
  public static final String OPTION_TARGET_DISTANCE = "targetDistanceCm";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.camera_fov_calibration_preferences);
  }
}
