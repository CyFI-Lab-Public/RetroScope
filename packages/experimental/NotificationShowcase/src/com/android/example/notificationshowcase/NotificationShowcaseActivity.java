// dummy notifications for demos
// for anandx@google.com by dsandler@google.com

package com.android.example.notificationshowcase;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class NotificationShowcaseActivity extends Activity {
    private static final String TAG = "NotificationShowcase";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Intent intent = new Intent(NotificationService.ACTION_CREATE);
        intent.setComponent(new ComponentName(this, NotificationService.class));
        startService(intent);
        finish();
    }
}
