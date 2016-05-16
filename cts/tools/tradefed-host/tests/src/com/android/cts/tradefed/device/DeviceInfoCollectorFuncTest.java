/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.cts.tradefed.device;

import com.android.tradefed.build.BuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;

import java.io.File;
import java.util.Map;

/**
 * Functional test for {@link DeviceInfoCollector}.
 * <p/>
 * TODO: this test assumes the TestDeviceSetup apk is located in the "java.io.tmpdir"
 */
public class DeviceInfoCollectorFuncTest extends DeviceTestCase {

    public void testCollectDeviceInfo() throws DeviceNotAvailableException {
        CollectingTestListener testListener = new CollectingTestListener();

        testListener.invocationStarted(new BuildInfo());
        DeviceInfoCollector.collectDeviceInfo(getDevice(), new File(
                System.getProperty("java.io.tmpdir")), testListener);
        assertNotNull(testListener.getCurrentRunResults());
        assertTrue(testListener.getCurrentRunResults().getRunMetrics().size() > 0);
        for (Map.Entry<String, String> metricEntry : testListener.getCurrentRunResults().getRunMetrics().entrySet()) {
            System.out.println(String.format("%s=%s", metricEntry.getKey(), metricEntry.getValue()));
        }
        testListener.invocationEnded(0);
    }
}
