package com.xtremelabs.robolectric.shadows;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.TestIntentSender;
import android.os.Parcel;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

/**
 * Shadow of {@code PendingIntent} that creates and sends {@code Intent}s appropriately.
 */
@Implements(PendingIntent.class)
public class ShadowPendingIntent {
    private Intent savedIntent;
    private Context savedContext;
    private boolean isActivityIntent;
    private boolean isBroadcastIntent;
    private boolean isServiceIntent;
    private int requestCode;

    @Implementation
    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
        return create(context, intent, true, false, false, requestCode);
    }

    @Implementation
    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
        return create(context, intent, false, true, false, requestCode);
    }

    @Implementation
    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
        return create(context, intent, false, false, true, requestCode);
    }

    @Implementation
    public void send() throws CanceledException {
        send(savedContext, 0, savedIntent);
    }

    @Implementation
    public void send(Context context, int code, Intent intent) throws CanceledException {
        savedIntent.fillIn(intent, 0 );
        if (isActivityIntent) {
            context.startActivity(savedIntent);
        } else if (isBroadcastIntent) {
            context.sendBroadcast(savedIntent);
        } else if (isServiceIntent) {
            context.startService(savedIntent);
        }
    }

    @Implementation
    public IntentSender getIntentSender() {
        TestIntentSender testIntentSender = new TestIntentSender();
        testIntentSender.intent = savedIntent;
        return testIntentSender;
    }

    @Implementation
    public static void writePendingIntentOrNullToParcel(PendingIntent sender, Parcel out) {
        if (sender == null) {
            out.writeInt(0);
            return;
        }
        sender.writeToParcel(out, 0);
    }

    @Implementation
    public static PendingIntent readPendingIntentOrNullFromParcel(Parcel in) {
        if (in.readInt() == 0) {
            return null;
        }
        boolean isActivity = readBooleanFromParcel(in);
        boolean isBroadcast = readBooleanFromParcel(in);
        boolean isService = readBooleanFromParcel(in);
        int requestCode = in.readInt();
        Intent intent = null;
        if (in.readInt() != 0) {
            intent = new Intent();
            intent.readFromParcel(in);
        }
        return create(null, intent, isActivity, isBroadcast, isService, requestCode);
    }

    @Implementation
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(1);
        writeBooleanToParcel(isActivityIntent, out);
        writeBooleanToParcel(isBroadcastIntent, out);
        writeBooleanToParcel(isServiceIntent, out);
        out.writeInt(requestCode);
        if (savedIntent != null) {
            out.writeInt(1);
            savedIntent.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    @Implementation
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isActivityIntent ? 1231 : 1237);
        result = prime * result + (isBroadcastIntent ? 1231 : 1237);
        result = prime * result + (isServiceIntent ? 1231 : 1237);
        result = prime * result + requestCode;
        result = prime * result + ((savedIntent == null) ? 0 : savedIntent.hashCode());
        return result;
    }

    @Override
    @Implementation
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof PendingIntent) {
            return shadowEquals(Robolectric.shadowOf((PendingIntent) obj));
        }
        return false;
    }

    private boolean shadowEquals(ShadowPendingIntent other) {
        if (isActivityIntent != other.isActivityIntent) {
            return false;
        }
        if (isBroadcastIntent != other.isBroadcastIntent) {
            return false;
        }
        if (isServiceIntent != other.isServiceIntent) {
            return false;
        }
        if (requestCode != other.requestCode) {
            return false;
        }
        if (savedIntent == null) {
            if (other.savedIntent != null) {
            return false;
            }
        } else if (!savedIntent.equals(other.savedIntent)) {
            return false;
        }
        return true;
    }

    public boolean isActivityIntent() {
        return isActivityIntent;
    }

    public boolean isBroadcastIntent() {
        return isBroadcastIntent;
    }

    public boolean isServiceIntent() {
        return isServiceIntent;
    }

    public Context getSavedContext() {
        return savedContext;
    }

    public Intent getSavedIntent() {
        return savedIntent;
    }

    public int getRequestCode() {
        return requestCode;
    }

    private static PendingIntent create(Context context, Intent intent, boolean isActivity, boolean isBroadcast, boolean isService, int requestCode) {
        PendingIntent pendingIntent = Robolectric.newInstanceOf(PendingIntent.class);
        ShadowPendingIntent shadowPendingIntent = Robolectric.shadowOf(pendingIntent);
        shadowPendingIntent.savedIntent = intent;
        shadowPendingIntent.isActivityIntent = isActivity;
        shadowPendingIntent.isBroadcastIntent = isBroadcast;
        shadowPendingIntent.isServiceIntent = isService;
        shadowPendingIntent.savedContext = context;
        shadowPendingIntent.requestCode = requestCode;
        return pendingIntent;
    }

    private static void writeBooleanToParcel(boolean b, Parcel out) {
        out.writeInt(b ? 1 : 0);
    }

    private static boolean readBooleanFromParcel(Parcel in) {
        return in.readInt() != 0;
    }
}
