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

package android.os.cts;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;

public class CountDownTimerTestStub extends Activity {
    public boolean onFinished;
    public ArrayList<Long> tickTimes;

    public final long MILLISINFUTURE = 4500;
    public final long INTERVAL = 1000;
    public CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tickTimes = new ArrayList<Long>();
        countDownTimer = new CountDownTimer(MILLISINFUTURE, INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                tickTimes.add(System.currentTimeMillis());
            }

            @Override
            public void onFinish() {
                onFinished = true;
            }
        };
    }

}
