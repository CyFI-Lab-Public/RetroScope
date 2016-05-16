/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.view.animation.cts;

import android.app.Instrumentation;
import android.cts.util.PollingCheck;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;

/**
 * The utility methods for animation test.
 */
public final class AnimationTestUtils {
    /** timeout delta when wait in case the system is sluggish */
    private static final long TIMEOUT_DELTA = 1000;

    /**
     * no public constructor since this is a utility class
     */
    private AnimationTestUtils() {

    }

    /**
     * Assert run an animation successfully. Timeout is duration of animation.
     *
     * @param instrumentation to run animation.
     * @param view view window to run animation.
     * @param animation will be run.
     */
    public static void assertRunAnimation(final Instrumentation instrumentation,
            final View view, final Animation animation) {
        assertRunAnimation(instrumentation, view, animation, animation.getDuration());
    }

    /**
     * Assert run an animation successfully.
     *
     * @param instrumentation to run animation.
     * @param view window to run animation.
     * @param animation will be run.
     * @param duration in milliseconds.
     */
    public static void assertRunAnimation(final Instrumentation instrumentation,
            final View view, final Animation animation, final long duration) {

        instrumentation.runOnMainSync(new Runnable() {
            public void run() {
                view.startAnimation(animation);
            }
        });

        // check whether it has started
        new PollingCheck() {
            @Override
            protected boolean check() {
                return animation.hasStarted();
            }
        }.run();

        // check whether it has ended after duration
        new PollingCheck(duration + TIMEOUT_DELTA) {
            @Override
            protected boolean check() {
                return animation.hasEnded();
            }
        }.run();

        instrumentation.waitForIdleSync();
    }

    /**
     * Assert run an AbsListView with LayoutAnimationController successfully.
     * @param instrumentation
     * @param view
     * @param controller
     * @param duration
     * @throws InterruptedException
     */
    public static void assertRunController(final Instrumentation instrumentation,
            final ViewGroup view, final LayoutAnimationController controller,
            final long duration) throws InterruptedException {

        instrumentation.runOnMainSync(new Runnable() {
           public void run() {
                view.setLayoutAnimation(controller);
                view.requestLayout();
           }
        });

        // LayoutAnimationController.isDone() always returns true, it's no use for stopping
        // the running, so just using sleeping fixed time instead. we reported issue 1799434 for it.
        Thread.sleep(duration + TIMEOUT_DELTA);
    }
}
