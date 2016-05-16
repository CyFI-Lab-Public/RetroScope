/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.openglperf.cts;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import java.util.List;

/**
 * Tests OpenGl rendering after task switching to other GL-using application
 * This test needs to control two applications and task switch between them to
 * force losing and reclaiming GL context
 */
public class GlAppSwitchTest extends
        ActivityInstrumentationTestCase2<GlPlanetsActivity> {

    private static final String REPLICA_ISLAND_PACKAGE = "com.replica.replicaisland";
    private static final String REPLICA_ISLAND_ACTIVITY = "AndouKun";
    private static final long TASK_SWITCH_SLOW_WAIT_TIME_MS = 15 * 1000;
    private static final int NUMBER_OF_TASK_SWITCHES_SLOW = 10;
    private static final long TASK_SWITCH_FAST_WAIT_TIME_MS = 2000;
    private static final int NUMBER_OF_TASK_SWITCHES_FAST = 50;
    private static final String ERROR_REPLICA_ISLAND_DEAD = "replica island not running";
    private static final int MAX_RUNNING_TASKS = 1000;

    private ActivityManager mActivityManager;

    private int mTaskIdSelf;
    private int mTaskIdReplica;

    public GlAppSwitchTest() {
        super(GlPlanetsActivity.class);
    }

    public void testGlActivitySwitchingFast() throws InterruptedException {
        runTaskSwitchTest(TASK_SWITCH_FAST_WAIT_TIME_MS, NUMBER_OF_TASK_SWITCHES_FAST);
        // wait for more time at the last run to allow watch dog timer in replica island to kick
        Thread.sleep(TASK_SWITCH_SLOW_WAIT_TIME_MS);
        assertTrue(ERROR_REPLICA_ISLAND_DEAD, isReplicaIslandRunning());
    }

    public void testGlActivitySwitchingSlow() throws InterruptedException {
        runTaskSwitchTest(TASK_SWITCH_SLOW_WAIT_TIME_MS, NUMBER_OF_TASK_SWITCHES_SLOW);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Instrumentation instrument = getInstrumentation();
        Context context = instrument.getContext();

        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        Intent intentPlanets = new Intent();
        intentPlanets.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_FRAMES, 0); //runs forever
        intentPlanets.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_PLANETS, 4); // max load
        setActivityIntent(intentPlanets);
        Activity activity = getActivity();
        instrument.waitForIdleSync();
        mTaskIdSelf = activity.getTaskId();
        // wait further to render some frames
        Thread.sleep(1000);

        Intent intentIsland = new Intent(Intent.ACTION_MAIN);
        intentIsland.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentIsland.addCategory(Intent.CATEGORY_LAUNCHER);
        intentIsland.setComponent(new ComponentName(REPLICA_ISLAND_PACKAGE,
                REPLICA_ISLAND_PACKAGE + "." + REPLICA_ISLAND_ACTIVITY));
        context.startActivity(intentIsland);
        // wait to render some frames
        Thread.sleep(5000);

        setReplicaIslandTask();
    }

    @Override
    protected void tearDown() throws Exception {
        showOrHideReplicaIsland(false);
        super.tearDown();
    }

    /**
     * run foreground / background task switching between replica island and GlPlanetsActivity.
     * @param waitTimeMs time to wait after each task switching
     * @param numberOfSwitches number of task switches to run
     * @throws InterruptedException
     */
    private void runTaskSwitchTest(long waitTimeMs, int numberOfSwitches)
            throws InterruptedException {
        boolean replicaForeground = false;
        assertTrue(ERROR_REPLICA_ISLAND_DEAD, isReplicaIslandRunning());
        for (int i = 0; i < numberOfSwitches; i++ ) {
            showOrHideReplicaIsland(replicaForeground);
            replicaForeground = !replicaForeground;
            Thread.sleep(waitTimeMs);
            assertTrue(ERROR_REPLICA_ISLAND_DEAD, isReplicaIslandRunning());
        }
    }

    private void setReplicaIslandTask() {
        boolean foundReplica = false;
        List<ActivityManager.RunningTaskInfo> tasks =
                mActivityManager.getRunningTasks(MAX_RUNNING_TASKS);
        for (ActivityManager.RunningTaskInfo info : tasks) {
            String packageName = info.baseActivity.getPackageName();
            if (packageName.contentEquals(REPLICA_ISLAND_PACKAGE)) {
                foundReplica = true;
                mTaskIdReplica = info.id;
                break;
            }
        }
        assertTrue("cannot find replica island running", foundReplica);
    }

    private boolean isReplicaIslandRunning() {
        boolean foundReplica = false;
        List<ActivityManager.RunningTaskInfo> tasks =
                mActivityManager.getRunningTasks(MAX_RUNNING_TASKS);
        for (ActivityManager.RunningTaskInfo info : tasks) {
            String packageName = info.baseActivity.getPackageName();
            if (packageName.contentEquals(REPLICA_ISLAND_PACKAGE)) {
                foundReplica = true;
                break;
            }
        }
        return foundReplica;
    }

    /**
     * send replica island to foreground (when show = true) or background
     * requires both replica island and GlPlanetsActivity to be alive.
     * @param show
     */
    private void showOrHideReplicaIsland(boolean show) {
        mActivityManager.moveTaskToFront(show ? mTaskIdReplica : mTaskIdSelf, 0);
    }
}
