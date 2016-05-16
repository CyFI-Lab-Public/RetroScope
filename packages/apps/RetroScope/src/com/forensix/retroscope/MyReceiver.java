package com.forensix.retroscope;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class MyReceiver extends BroadcastReceiver {

@Override
public void onReceive(Context context, Intent intent) {
    // TODO Auto-generated method stub

     // Unlock the screen
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
            | PowerManager.ACQUIRE_CAUSES_WAKEUP
            | PowerManager.ON_AFTER_RELEASE, "INFO");
    wl.acquire();

    KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    KeyguardLock kl = km.newKeyguardLock("name");
    kl.disableKeyguard();

    Intent myIntent = new Intent(context, MyActivity.class);
    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(myIntent);
}
}
