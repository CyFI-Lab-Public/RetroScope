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

package com.android.test.runner;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.Debug;
import android.os.Looper;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.test.runner.listener.CoverageListener;
import com.android.test.runner.listener.DelayInjector;
import com.android.test.runner.listener.InstrumentationResultPrinter;
import com.android.test.runner.listener.InstrumentationRunListener;
import com.android.test.runner.listener.SuiteAssignmentPrinter;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Instrumentation} that runs JUnit3 and JUnit4 tests against
 * an Android package (application).
 * <p/>
 * Currently experimental. Based on {@link android.test.InstrumentationTestRunner}.
 * <p/>
 * Will eventually support a superset of {@link android.test.InstrumentationTestRunner} features,
 * while maintaining command/output format compatibility with that class.
 *
 * <h3>Typical Usage</h3>
 * <p/>
 * Write JUnit3 style {@link junit.framework.TestCase}s and/or JUnit4 style
 * {@link org.junit.Test}s that perform tests against the classes in your package.
 * Make use of the {@link com.android.test.InjectContext} and
 * {@link com.android.test.InjectInstrumentation} annotations if needed.
 * <p/>
 * In an appropriate AndroidManifest.xml, define an instrumentation with android:name set to
 * {@link com.android.test.runner.AndroidJUnitRunner} and the appropriate android:targetPackage set.
 * <p/>
 * Execution options:
 * <p/>
 * <b>Running all tests:</b> adb shell am instrument -w
 * com.android.foo/com.android.test.runner.AndroidJUnitRunner
 * <p/>
 * <b>Running all tests in a class:</b> adb shell am instrument -w
 * -e class com.android.foo.FooTest
 * com.android.foo/com.android.test.runner.AndroidJUnitRunner
 * <p/>
 * <b>Running a single test:</b> adb shell am instrument -w
 * -e class com.android.foo.FooTest#testFoo
 * com.android.foo/com.android.test.runner.AndroidJUnitRunner
 * <p/>
 * <b>Running all tests in multiple classes:</b> adb shell am instrument -w
 * -e class com.android.foo.FooTest,com.android.foo.TooTest
 * com.android.foo/com.android.test.runner.AndroidJUnitRunner
 * <p/>
 * <b>Running all tests in a java package:</b> adb shell am instrument -w
 * -e package com.android.foo.bar
 * com.android.foo/com.android.test.runner.AndroidJUnitRunner
 * <b>To debug your tests, set a break point in your code and pass:</b>
 * -e debug true
 * <p/>
 * <b>Running a specific test size i.e. annotated with
 * {@link android.test.suitebuilder.annotation.SmallTest} or
 * {@link android.test.suitebuilder.annotation.MediumTest} or
 * {@link android.test.suitebuilder.annotation.LargeTest}:</b>
 * adb shell am instrument -w -e size [small|medium|large]
 * com.android.foo/android.test.InstrumentationTestRunner
 * <p/>
 * <b>Filter test run to tests with given annotation:</b> adb shell am instrument -w
 * -e annotation com.android.foo.MyAnnotation
 * com.android.foo/android.test.InstrumentationTestRunner
 * <p/>
 * If used with other options, the resulting test run will contain the intersection of the two
 * options.
 * e.g. "-e size large -e annotation com.android.foo.MyAnnotation" will run only tests with both
 * the {@link LargeTest} and "com.android.foo.MyAnnotation" annotations.
 * <p/>
 * <b>Filter test run to tests <i>without</i> given annotation:</b> adb shell am instrument -w
 * -e notAnnotation com.android.foo.MyAnnotation
 * com.android.foo/android.test.InstrumentationTestRunner
 * <p/>
 * As above, if used with other options, the resulting test run will contain the intersection of
 * the two options.
 * e.g. "-e size large -e notAnnotation com.android.foo.MyAnnotation" will run tests with
 * the {@link LargeTest} annotation that do NOT have the "com.android.foo.MyAnnotation" annotations.
 * <p/>
 * <b>To run in 'log only' mode</b>
 * -e log true
 * This option will load and iterate through all test classes and methods, but will bypass actual
 * test execution. Useful for quickly obtaining info on the tests to be executed by an
 * instrumentation command.
 * <p/>
 * <b>To generate EMMA code coverage:</b>
 * -e coverage true
 * Note: this requires an emma instrumented build. By default, the code coverage results file
 * will be saved in a /data/<app>/coverage.ec file, unless overridden by coverageFile flag (see
 * below)
 * <p/>
 * <b> To specify EMMA code coverage results file path:</b>
 * -e coverageFile /sdcard/myFile.ec
 * <p/>
 */
public class AndroidJUnitRunner extends Instrumentation {

    // constants for supported instrumentation arguments
    public static final String ARGUMENT_TEST_CLASS = "class";
    private static final String ARGUMENT_TEST_SIZE = "size";
    private static final String ARGUMENT_LOG_ONLY = "log";
    private static final String ARGUMENT_ANNOTATION = "annotation";
    private static final String ARGUMENT_NOT_ANNOTATION = "notAnnotation";
    private static final String ARGUMENT_DELAY_MSEC = "delay_msec";
    private static final String ARGUMENT_COVERAGE = "coverage";
    private static final String ARGUMENT_COVERAGE_PATH = "coverageFile";
    private static final String ARGUMENT_SUITE_ASSIGNMENT = "suiteAssignment";
    private static final String ARGUMENT_DEBUG = "debug";
    private static final String ARGUMENT_EXTRA_LISTENER = "extraListener";
    private static final String ARGUMENT_TEST_PACKAGE = "package";
    // TODO: consider supporting 'count' from InstrumentationTestRunner

    private static final String LOG_TAG = "AndroidJUnitRunner";

    private Bundle mArguments;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mArguments = arguments;

        start();
    }

    /**
     * Get the Bundle object that contains the arguments passed to the instrumentation
     *
     * @return the Bundle object
     * @hide
     */
    public Bundle getArguments(){
        return mArguments;
    }

    /**
     * Set the arguments.
     *
     * @VisibleForTesting
     */
    void setArguments(Bundle args) {
        mArguments = args;
    }

    private boolean getBooleanArgument(String tag) {
        String tagString = getArguments().getString(tag);
        return tagString != null && Boolean.parseBoolean(tagString);
    }

    /**
     * Initialize the current thread as a looper.
     * <p/>
     * Exposed for unit testing.
     */
    void prepareLooper() {
        Looper.prepare();
    }

    @Override
    public void onStart() {
        prepareLooper();

        if (getBooleanArgument(ARGUMENT_DEBUG)) {
            Debug.waitForDebugger();
        }

        setupDexmaker();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream writer = new PrintStream(byteArrayOutputStream);
        List<RunListener> listeners = new ArrayList<RunListener>();

        try {
            JUnitCore testRunner = new JUnitCore();
            addListeners(listeners, testRunner, writer);

            TestRequest testRequest = buildRequest(getArguments(), writer);
            Result result = testRunner.run(testRequest.getRequest());
            result.getFailures().addAll(testRequest.getFailures());
            Log.i(LOG_TAG, String.format("Test run complete. %d tests, %d failed, %d ignored",
                    result.getRunCount(), result.getFailureCount(), result.getIgnoreCount()));
        } catch (Throwable t) {
            // catch all exceptions so a more verbose error message can be displayed
            writer.println(String.format(
                    "Test run aborted due to unexpected exception: %s",
                    t.getMessage()));
            t.printStackTrace(writer);

        } finally {
            Bundle results = new Bundle();
            reportRunEnded(listeners, writer, results);
            writer.close();
            results.putString(Instrumentation.REPORT_KEY_STREAMRESULT,
                    String.format("\n%s",
                            byteArrayOutputStream.toString()));
            finish(Activity.RESULT_OK, results);
        }

    }

    private void addListeners(List<RunListener> listeners, JUnitCore testRunner,
            PrintStream writer) {
        if (getBooleanArgument(ARGUMENT_SUITE_ASSIGNMENT)) {
            addListener(listeners, testRunner, new SuiteAssignmentPrinter(writer));
        } else {
            addListener(listeners, testRunner, new TextListener(writer));
            addListener(listeners, testRunner, new InstrumentationResultPrinter(this));
            addDelayListener(listeners, testRunner);
            addCoverageListener(listeners, testRunner);
        }

        addExtraListeners(listeners, testRunner, writer);
    }

    private void addListener(List<RunListener> list, JUnitCore testRunner, RunListener listener) {
        list.add(listener);
        testRunner.addListener(listener);
    }

    private void addCoverageListener(List<RunListener> list, JUnitCore testRunner) {
        if (getBooleanArgument(ARGUMENT_COVERAGE)) {
            String coverageFilePath = getArguments().getString(ARGUMENT_COVERAGE_PATH);
            addListener(list, testRunner, new CoverageListener(this, coverageFilePath));
        }
    }

    /**
     * Sets up listener to inject {@link #ARGUMENT_DELAY_MSEC}, if specified.
     * @param testRunner
     */
    private void addDelayListener(List<RunListener> list, JUnitCore testRunner) {
        try {
            Object delay = getArguments().get(ARGUMENT_DELAY_MSEC);  // Accept either string or int
            if (delay != null) {
                int delayMsec = Integer.parseInt(delay.toString());
                addListener(list, testRunner, new DelayInjector(delayMsec));
            }
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Invalid delay_msec parameter", e);
        }
    }

    private void addExtraListeners(List<RunListener> listeners, JUnitCore testRunner,
            PrintStream writer) {
        String extraListenerList = getArguments().getString(ARGUMENT_EXTRA_LISTENER);
        if (extraListenerList == null) {
            return;
        }

        for (String listenerName : extraListenerList.split(",")) {
            addExtraListener(listeners, testRunner, writer, listenerName);
        }
    }

    private void addExtraListener(List<RunListener> listeners, JUnitCore testRunner,
            PrintStream writer, String extraListener) {
        if (extraListener == null || extraListener.length() == 0) {
            return;
        }

        final Class<?> klass;
        try {
            klass = Class.forName(extraListener);
        } catch (ClassNotFoundException e) {
            writer.println("Could not find extra RunListener class " + extraListener);
            return;
        }

        if (!RunListener.class.isAssignableFrom(klass)) {
            writer.println("Extra listeners must extend RunListener class " + extraListener);
            return;
        }

        try {
            klass.getConstructor().setAccessible(true);
        } catch (NoSuchMethodException e) {
            writer.println("Must have no argument constructor for class " + extraListener);
            return;
        }

        final RunListener l;
        try {
            l = (RunListener) klass.newInstance();
        } catch (Throwable t) {
            writer.println("Could not instantiate extra RunListener class " + extraListener);
            t.printStackTrace(writer);
            return;
        }

        addListener(listeners, testRunner, l);
    }

    private void reportRunEnded(List<RunListener> listeners, PrintStream writer, Bundle results) {
        for (RunListener listener : listeners) {
            if (listener instanceof InstrumentationRunListener) {
                ((InstrumentationRunListener)listener).instrumentationRunFinished(writer, results);
            }
        }
    }

    /**
     * Builds a {@link TestRequest} based on given input arguments.
     * <p/>
     * Exposed for unit testing.
     */
    TestRequest buildRequest(Bundle arguments, PrintStream writer) {
        // only load tests for current aka testContext
        // Note that this represents a change from InstrumentationTestRunner where
        // getTargetContext().getPackageCodePath() was also scanned
        TestRequestBuilder builder = createTestRequestBuilder(writer,
                getContext().getPackageCodePath());

        String testClassName = arguments.getString(ARGUMENT_TEST_CLASS);
        if (testClassName != null) {
            for (String className : testClassName.split(",")) {
                parseTestClass(className, builder);
            }
        }

        String testPackage = arguments.getString(ARGUMENT_TEST_PACKAGE);
        if (testPackage != null) {
            builder.addTestPackageFilter(testPackage);
        }

        String testSize = arguments.getString(ARGUMENT_TEST_SIZE);
        if (testSize != null) {
            builder.addTestSizeFilter(testSize);
        }

        String annotation = arguments.getString(ARGUMENT_ANNOTATION);
        if (annotation != null) {
            builder.addAnnotationInclusionFilter(annotation);
        }

        String notAnnotation = arguments.getString(ARGUMENT_NOT_ANNOTATION);
        if (notAnnotation != null) {
            builder.addAnnotationExclusionFilter(notAnnotation);
        }

        if (getBooleanArgument(ARGUMENT_LOG_ONLY)) {
            builder.setSkipExecution(true);
        }
        return builder.build(this, arguments);
    }

    /**
     * Factory method for {@link TestRequestBuilder}.
     * <p/>
     * Exposed for unit testing.
     */
    TestRequestBuilder createTestRequestBuilder(PrintStream writer, String... packageCodePaths) {
        return new TestRequestBuilder(writer, packageCodePaths);
    }

    /**
     * Parse and load the given test class and, optionally, method
     *
     * @param testClassName - full package name of test class and optionally method to add.
     *        Expected format: com.android.TestClass#testMethod
     * @param testSuiteBuilder - builder to add tests to
     */
    private void parseTestClass(String testClassName, TestRequestBuilder testRequestBuilder) {
        int methodSeparatorIndex = testClassName.indexOf('#');

        if (methodSeparatorIndex > 0) {
            String testMethodName = testClassName.substring(methodSeparatorIndex + 1);
            testClassName = testClassName.substring(0, methodSeparatorIndex);
            testRequestBuilder.addTestMethod(testClassName, testMethodName);
        } else {
            testRequestBuilder.addTestClass(testClassName);
        }
    }

    private void setupDexmaker() {
        // Explicitly set the Dexmaker cache, so tests that use mocking frameworks work
        String dexCache = getTargetContext().getCacheDir().getPath();
        Log.i(LOG_TAG, "Setting dexmaker.dexcache to " + dexCache);
        System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().getPath());
    }
}
