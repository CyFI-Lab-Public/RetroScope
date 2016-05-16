package foo.bar.testback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.util.Log;

public class IdleMaintenanceReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = IdleMaintenanceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Debug.waitForDebugger();
        Log.i(LOG_TAG, (intent.getAction() != null) ? intent.getAction() : "null");
    }
}
