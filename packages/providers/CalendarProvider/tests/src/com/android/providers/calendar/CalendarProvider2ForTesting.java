package com.android.providers.calendar;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class CalendarProvider2ForTesting extends CalendarProvider2 {

    /**
     * For testing, don't want to start the TimezoneCheckerThread, as it results
     * in race conditions.  Thus updateTimezoneDependentFields is stubbed out.
     */
    @Override
    protected void updateTimezoneDependentFields() {
    }

    /**
     * For testing, don't want onAccountsUpdated asynchronously deleting data.
     */
    @Override
    public void onAccountsUpdated(Account[] accounts) {
    }

    @Override
    protected void doUpdateTimezoneDependentFields() {
    }

    @Override
    protected void postInitialize() {
    }

    @Override
    protected void initCalendarAlarm() {
        mCalendarAlarm = new MockCalendarAlarmManager(getContext());
    }

    @Override
    protected boolean shouldSyncFor(Uri uri) {
        return true;
    }

    private static class MockCalendarAlarmManager extends CalendarAlarmManager {

        public MockCalendarAlarmManager(Context context) {
            super(context);
        }

        @Override
        protected void initializeWithContext(Context context) {
            mContext = context;
            mNextAlarmCheckScheduled = new AtomicBoolean(false);
            mAlarmLock = new Object();
        }

        @Override
        public void set(int type, long triggerAtTime, PendingIntent operation) {
        }

        @Override
        public void cancel(PendingIntent operation) {
        }

        @Override
        public void scheduleAlarm(long alarmTime) {
        }

        @Override
        public void rescheduleMissedAlarms(ContentResolver cr) {
        }


        @Override
        PowerManager.WakeLock getScheduleNextAlarmWakeLock() {
            return null;
        }

        @Override
        void acquireScheduleNextAlarmWakeLock() {
        }

        @Override
        void releaseScheduleNextAlarmWakeLock() {
        }
    }
}
