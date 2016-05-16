// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.cts.verifier.deskclock;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.AlarmClock;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.IntentDrivenTestActivity;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;
import com.android.cts.verifier.IntentDrivenTestActivity.ButtonInfo;
import com.android.cts.verifier.IntentDrivenTestActivity.IntentFactory;
import com.android.cts.verifier.IntentDrivenTestActivity.TestInfo;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Activity that lists all the DeskClock tests.
 */
public class DeskClockTestsActivity extends PassFailButtons.TestListActivity {

    private static final String SHOW_ALARMS_TEST = "SHOW_ALARMS";
    public static final String SET_ALARM_WITH_UI_TEST = "SET_ALARM_WITH_UI";
    public static final String START_ALARM_TEST = "START_ALARM";
    public static final String CREATE_ALARM_TEST = "CREATE_ALARM";
    public static final String SET_TIMER_WITH_UI_TEST = "SET_TIMER_WITH_UI";
    public static final String START_TIMER = "START_TIMER";
    public static final String START_TIMER_WITH_UI = "START_TIMER_WITH_UI";

    private static final ArrayList<Integer> DAYS = new ArrayList<Integer>();

    private static final Intent CREATE_ALARM_INTENT = new Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "Create Alarm Test")
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            .putExtra(AlarmClock.EXTRA_VIBRATE, true)
            .putExtra(AlarmClock.EXTRA_RINGTONE, AlarmClock.VALUE_RINGTONE_SILENT)
            .putExtra(AlarmClock.EXTRA_HOUR, 1)
            .putExtra(AlarmClock.EXTRA_MINUTES, 23)
            .putExtra(AlarmClock.EXTRA_DAYS, DAYS);

    static {
        DAYS.add(Calendar.MONDAY);
        DAYS.add(Calendar.WEDNESDAY);
    }

    private static final Intent SHOW__ALARMS_INTENT = new Intent(AlarmClock.ACTION_SHOW_ALARMS);

    private static final Intent SET_ALARM_WITH_UI_INTENT = new Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false);

    private static final Intent SET_TIMER_WITH_UI_INTENT = new Intent(AlarmClock.ACTION_SET_TIMER)
             .putExtra(AlarmClock.EXTRA_SKIP_UI, false);

    private static final Intent START_TIMER_INTENT = new Intent(AlarmClock.ACTION_SET_TIMER)
    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
    .putExtra(AlarmClock.EXTRA_MESSAGE, "Start Timer Test")
    .putExtra(AlarmClock.EXTRA_LENGTH, 30);

    private static final Intent START_TIMER_WITH_UI_INTENT = new Intent(AlarmClock.ACTION_SET_TIMER)
    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    .putExtra(AlarmClock.EXTRA_MESSAGE, "Start Timer Test")
    .putExtra(AlarmClock.EXTRA_LENGTH, 30);

    private static final TestInfo[] ALARM_TESTS = new TestInfo[] {
            new TestInfo(
                    SHOW_ALARMS_TEST,
                    R.string.dc_show_alarms_test,
                    R.string.dc_show_alarms_test_info,
                    new ButtonInfo(
                            R.string.dc_show_alarms_button,
                            SHOW__ALARMS_INTENT)),
            new TestInfo(
                    SET_ALARM_WITH_UI_TEST,
                    R.string.dc_set_alarm_with_ui_test,
                    R.string.dc_set_alarm_with_ui_test_info,
                    new ButtonInfo(
                            R.string.dc_set_alarm_button,
                            SET_ALARM_WITH_UI_INTENT)),
            new TestInfo(
                    START_ALARM_TEST,
                    R.string.dc_start_alarm_test,
                    R.string.dc_start_alarm_test_info,
                    new ButtonInfo(
                            R.string.dc_set_alarm_button,
                            DeskClockIntentFactory.class.getName()),
                    new ButtonInfo(
                            R.string.dc_set_alarm_verify_button,
                            SHOW__ALARMS_INTENT)),
            new TestInfo(
                    CREATE_ALARM_TEST,
                    R.string.dc_full_alarm_test,
                    R.string.dc_full_alarm_test_info,
                    new ButtonInfo(
                            R.string.dc_full_alarm_button,
                            CREATE_ALARM_INTENT)),
    };

    private static final TestInfo[] TIMER_TESTS = new TestInfo[] {
            new TestInfo(
                    SET_TIMER_WITH_UI_TEST,
                    R.string.dc_set_timer_with_ui_test,
                    R.string.dc_set_timer_with_ui_test_info,
                    new ButtonInfo(
                            R.string.dc_set_timer_with_ui_button,
                            SET_TIMER_WITH_UI_INTENT)),
            new TestInfo(
                    START_TIMER,
                    R.string.dc_start_timer_test,
                    R.string.dc_start_timer_test_info,
                    new ButtonInfo(
                            R.string.dc_start_timer_button,
                            START_TIMER_INTENT)),
            new TestInfo(
                    START_TIMER_WITH_UI,
                    R.string.dc_start_timer_with_ui_test,
                    R.string.dc_start_timer_with_ui_test_info,
                    new ButtonInfo(
                            R.string.dc_start_timer_button,
                            START_TIMER_WITH_UI_INTENT)),
  };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setInfoResources(R.string.deskclock_tests, R.string.deskclock_tests_info, 0);
        setPassFailButtonClickListeners();

        getPassButton().setEnabled(false);

        final ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        adapter.add(TestListItem.newCategory(this, R.string.deskclock_group_alarms));
        addTests(adapter, ALARM_TESTS);

        adapter.add(TestListItem.newCategory(this, R.string.deskclock_group_timers));
        addTests(adapter, TIMER_TESTS);

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        setTestListAdapter(adapter);
    }

    private void addTests(ArrayTestListAdapter adapter, TestInfo[] tests) {
        for (TestInfo info : tests) {

            final int title = info.getTitle();
            adapter.add(TestListItem.newTest(this, title, info.getTestId(),
            new Intent(this, IntentDrivenTestActivity.class)
                    .putExtra(IntentDrivenTestActivity.EXTRA_ID, info.getTestId())
                    .putExtra(IntentDrivenTestActivity.EXTRA_TITLE, title)
                    .putExtra(IntentDrivenTestActivity.EXTRA_INFO, info.getInfoText())
                    .putExtra(IntentDrivenTestActivity.EXTRA_BUTTONS, info.getButtons()),
                    null));
        }
    }

    /**
     * Enable Pass Button when the all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }

    public static class DeskClockIntentFactory implements IntentFactory {
        @Override
        public Intent[] createIntents(String testId, int buttonText) {
            if (testId.equals(START_ALARM_TEST)) {
                // Alarm should go off 2 minutes from now
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(cal.getTimeInMillis() + 120000);
                return new Intent[] {
                        new Intent(AlarmClock.ACTION_SET_ALARM)
                                .putExtra(AlarmClock.EXTRA_MESSAGE, "Start Alarm Test")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        .putExtra(AlarmClock.EXTRA_VIBRATE, true)
                        .putExtra(AlarmClock.EXTRA_RINGTONE, AlarmClock.VALUE_RINGTONE_SILENT)
                        .putExtra(AlarmClock.EXTRA_HOUR, cal.get(Calendar.HOUR_OF_DAY))
                        .putExtra(AlarmClock.EXTRA_MINUTES, cal.get(Calendar.MINUTE)),
                };
            } else {
                throw new IllegalArgumentException("Unknown test: " + testId);
            }
        }
    }
}
