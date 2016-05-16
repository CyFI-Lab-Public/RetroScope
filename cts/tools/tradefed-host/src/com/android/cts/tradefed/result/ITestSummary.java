/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.tradefed.result;

/**
 * Interface for a single CTS result summary.
 */
public interface ITestSummary {

    /**
     * @return the session id
     */
    int getId();

    /**
     * @return the starting timestamp, also known as result directory name
     */
    String getTimestamp();

    /**
     * @return the num of not executed tests
     */
    int getNumIncomplete();

    /**
     * @return the number of failed tests
     */
    int getNumFailed();

    /**
     * @return the number of passed tests
     */
    int getNumPassed();

    /**
     * @return the test plan associated with result
     */
    String getTestPlan();

    /**
     * Return the user-friendly displayed start time stored in result XML.
     * <p/>
     * Expected format: {@link TimeUtil#getTimestamp()}
     */
    String getStartTime();

    /**
     * @return a comma separated list of device serials associated with result
     */
    String getDeviceSerials();

}
