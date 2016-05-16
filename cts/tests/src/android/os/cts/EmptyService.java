/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.os.cts;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class EmptyService extends Service {

    private final Binder mToken = new Binder();;

    private final IEmptyService.Stub mBinder = new IEmptyService.Stub() {
        public IBinder getToken() {
            return mToken;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (IEmptyService.class.getName().equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }
}
