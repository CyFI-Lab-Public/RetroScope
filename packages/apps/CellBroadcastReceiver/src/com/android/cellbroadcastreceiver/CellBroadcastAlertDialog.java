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

package com.android.cellbroadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.CellBroadcastMessage;

/**
 * Custom alert dialog with optional flashing warning icon.
 * Alert audio and text-to-speech handled by {@link CellBroadcastAlertAudio}.
 * Keyguard handling based on {@code AlarmAlert} class from DeskClock app.
 */
public class CellBroadcastAlertDialog extends CellBroadcastAlertFullScreen {

    private BroadcastReceiver mScreenOffReceiver;

    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleScreenOff();
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Listen for the screen turning off so that when the screen comes back
        // on, the user does not need to unlock the phone to dismiss the alert.
        if (CellBroadcastConfigService.isEmergencyAlertMessage(getLatestMessage())) {
            mScreenOffReceiver = new ScreenOffReceiver();
            registerReceiver(mScreenOffReceiver,
                    new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        // stop animating warning icon, stop playing alert sound, mark broadcast as read
        dismiss();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.cell_broadcast_alert;
    }

    private void handleScreenOff() {
        // Launch the full screen activity but do not turn the screen on.
        Intent i = new Intent(this, CellBroadcastAlertFullScreen.class);
        i.putParcelableArrayListExtra(CellBroadcastMessage.SMS_CB_MESSAGE_EXTRA, mMessageList);
        i.putExtra(SCREEN_OFF_EXTRA, true);
        startActivity(i);
        finish();
    }
}
