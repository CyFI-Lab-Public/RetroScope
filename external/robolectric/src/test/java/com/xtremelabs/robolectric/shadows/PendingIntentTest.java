package com.xtremelabs.robolectric.shadows;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.TestIntentSender;
import android.os.Parcel;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(WithTestDefaultsRunner.class)
public class PendingIntentTest {
    @Test
    public void shouldGetIntentSender() {
        Intent expectedIntent = new Intent();
        PendingIntent service = PendingIntent.getService(null, 0, expectedIntent, 0);

        IntentSender intentSender = service.getIntentSender();
        assertThat(expectedIntent, equalTo(((TestIntentSender) intentSender).intent));
    }

    @Test
    public void getBroadcast__shouldCreateIntentForBroadcast() throws Exception {
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(Robolectric.application, 99, intent, 100);
        ShadowPendingIntent shadow = shadowOf(pendingIntent);
        assertThat(shadow.isActivityIntent(), is(false));
        assertThat(shadow.isBroadcastIntent(), is(true));
        assertThat(shadow.isServiceIntent(), is(false));
        assertThat(intent, equalTo(shadow.getSavedIntent()));
        assertThat(Robolectric.application, equalTo(shadow.getSavedContext()));
    }

    @Test
    public void getActivity__shouldCreateIntentForBroadcast() throws Exception {
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(Robolectric.application, 99, intent, 100);
        ShadowPendingIntent shadow = shadowOf(pendingIntent);
        assertThat(shadow.isActivityIntent(), is(true));
        assertThat(shadow.isBroadcastIntent(), is(false));
        assertThat(shadow.isServiceIntent(), is(false));
        assertThat(intent, equalTo(shadow.getSavedIntent()));
        assertThat(Robolectric.application, equalTo(shadow.getSavedContext()));
    }

    @Test
    public void getService__shouldCreateIntentForBroadcast() throws Exception {
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getService(Robolectric.application, 99, intent, 100);
        ShadowPendingIntent shadow = shadowOf(pendingIntent);
        assertThat(shadow.isActivityIntent(), is(false));
        assertThat(shadow.isBroadcastIntent(), is(false));
        assertThat(shadow.isServiceIntent(), is(true));
        assertThat(intent, equalTo(shadow.getSavedIntent()));
        assertThat(Robolectric.application, equalTo(shadow.getSavedContext()));
    }

    @Test
    public void send__shouldFillInIntentData() throws Exception {
        Intent intent = new Intent();
        Activity context = new Activity();
        PendingIntent forActivity = PendingIntent.getActivity(context, 99, intent, 100);

        Activity otherContext = new Activity();
        Intent fillIntent = new Intent();
        fillIntent.putExtra("TEST", 23);
        forActivity.send(otherContext, 0, fillIntent);

        Intent i = shadowOf(otherContext).getNextStartedActivity();
        assertThat(i, sameInstance(intent));
        assertThat(i.getIntExtra("TEST", -1), equalTo(23));
    }

    @Test
    public void testEquals() throws Exception {
        PendingIntent pi1 = PendingIntent.getActivity(Robolectric.application, 99,
            new Intent("action"), 100);
        PendingIntent pi2 = PendingIntent.getActivity(null, 99, new Intent("action"), 100);
        PendingIntent pi3 = PendingIntent.getService(Robolectric.application, 99,
            new Intent("action"), 100);
        assertThat(pi1, equalTo(pi2));
        assertThat(pi1, not(equalTo(pi3)));
    }

    @Test
    public void parcelIo_nullPendingIntent() {
        verifyPendingIntentReadIsWhatWasWrittenToParcel(null);
    }

    @Test
    public void parcelIo_shouldGetBackBroadcastIntentWrittenToParcelWithNullIntent() {
        verifyPendingIntentReadIsWhatWasWrittenToParcel(PendingIntent
            .getBroadcast(Robolectric.application, 99, null, 100));
    }

    @Test
    public void parcelIo_shouldGetBackBroadcastIntentWrittenToParcel() {
      verifyPendingIntentReadIsWhatWasWrittenToParcel(PendingIntent
          .getBroadcast(Robolectric.application, 99, new Intent(), 100));
    }

    @Test
    public void parcelIo_shouldGetBackActivityIntentWrittenToParcel() {
        verifyPendingIntentReadIsWhatWasWrittenToParcel(PendingIntent
            .getActivity(Robolectric.application, 99, new Intent(), 100));
    }

    @Test
    public void parcelIo_shouldGetBackServiceIntentWrittenToParcel() {
        verifyPendingIntentReadIsWhatWasWrittenToParcel(PendingIntent
            .getService(Robolectric.application, 99, new Intent(), 100));
    }

    private void verifyPendingIntentReadIsWhatWasWrittenToParcel(PendingIntent expected) {
        Parcel parcel = Parcel.obtain();
        PendingIntent.writePendingIntentOrNullToParcel(expected, parcel);
        parcel.setDataPosition(0);
        PendingIntent actual = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        if (expected == null) {
            assertNull(actual);
        } else {
            assertThat(expected, equalTo(actual));
        }
    }
}
