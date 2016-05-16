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

package com.android.quicksearchbox;

import android.app.Application;

public class QsbApplicationWrapper extends Application {

    private QsbApplication mApp;

    @Override
    public void onTerminate() {
        synchronized (this) {
            if (mApp != null) {
                mApp.close();
            }
        }
        super.onTerminate();
    }

    public synchronized QsbApplication getApp() {
        if (mApp == null) {
            mApp = createQsbApplication();
        }
        return mApp;
    }

    protected QsbApplication createQsbApplication() {
        return new QsbApplication(this);
    }

}
