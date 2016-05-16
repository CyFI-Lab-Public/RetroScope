/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.app.Application;
import android.content.res.Configuration;


public class MockApplication extends Application {

    public boolean isOnCreateCalled;
    public boolean isConstructorCalled;
    public boolean isOnConfigurationChangedCalled;
    public boolean isOnLowMemoryCalled;

    public MockApplication() {
        super();
        isConstructorCalled = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isOnCreateCalled = true;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // The documentation states that one cannot rely on this method being called. No need to
        // test it here.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isOnConfigurationChangedCalled = true;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        isOnLowMemoryCalled = true;
    }
}
