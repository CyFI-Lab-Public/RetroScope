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

import android.app.Instrumentation;
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.android.test.runner.ClassPathScanner.ChainedClassNameFilter;
import com.android.test.runner.ClassPathScanner.ExcludePackageNameFilter;
import com.android.test.runner.ClassPathScanner.ExternalClassNameFilter;
import com.android.test.runner.ClassPathScanner.InclusivePackageNameFilter;

import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Builds a {@link Request} from test classes in given apk paths, filtered on provided set of
 * restrictions.
 */
public class TestRequestBuilder {

    private static final String LOG_TAG = "TestRequestBuilder";

    public static final String LARGE_SIZE = "large";
    public static final String MEDIUM_SIZE = "medium";
    public static final String SMALL_SIZE = "small";

    private String[] mApkPaths;
    private TestLoader mTestLoader;
    private Filter mFilter = new AnnotationExclusionFilter(Suppress.class);
    private PrintStream mWriter;
    private boolean mSkipExecution = false;
    private String mTestPackageName = null;

    /**
     * Filter that only runs tests whose method or class has been annotated with given filter.
     */
    private static class AnnotationInclusionFilter extends Filter {

        private final Class<? extends Annotation> mAnnotationClass;

        AnnotationInclusionFilter(Class<? extends Annotation> annotation) {
            mAnnotationClass = annotation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldRun(Description description) {
            if (description.isTest()) {
                return description.getAnnotation(mAnnotationClass) != null ||
                        description.getTestClass().isAnnotationPresent(mAnnotationClass);
            } else {
                // the entire test class/suite should be filtered out if all its methods are
                // filtered
                // TODO: This is not efficient since some children may end up being evaluated more
                // than once. This logic seems to be only necessary for JUnit3 tests. Look into
                // fixing in upstream
                for (Description child : description.getChildren()) {
                    if (shouldRun(child)) {
                        return true;
                    }
                }
                // no children to run, filter this out
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String describe() {
            return String.format("annotation %s", mAnnotationClass.getName());
        }
    }

    /**
     * Filter out tests whose method or class has been annotated with given filter.
     */
    private static class AnnotationExclusionFilter extends Filter {

        private final Class<? extends Annotation> mAnnotationClass;

        AnnotationExclusionFilter(Class<? extends Annotation> annotation) {
            mAnnotationClass = annotation;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldRun(Description description) {
            final Class<?> testClass = description.getTestClass();

            /* Parameterized tests have no test classes. */
            if (testClass == null) {
                return true;
            }

            if (testClass.isAnnotationPresent(mAnnotationClass) ||
                    description.getAnnotation(mAnnotationClass) != null) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String describe() {
            return String.format("not annotation %s", mAnnotationClass.getName());
        }
    }

    public TestRequestBuilder(PrintStream writer, String... apkPaths) {
        mApkPaths = apkPaths;
        mTestLoader = new TestLoader(writer);
    }

    /**
     * Add a test class to be executed. All test methods in this class will be executed.
     *
     * @param className
     */
    public void addTestClass(String className) {
        mTestLoader.loadClass(className);
    }

    /**
     * Adds a test method to run.
     * <p/>
     * Currently only supports one test method to be run.
     */
    public void addTestMethod(String testClassName, String testMethodName) {
        Class<?> clazz = mTestLoader.loadClass(testClassName);
        if (clazz != null) {
            mFilter = mFilter.intersect(matchParameterizedMethod(
                    Description.createTestDescription(clazz, testMethodName)));
        }
    }

    /**
     * A filter to get around the fact that parameterized tests append "[#]" at
     * the end of the method names. For instance, "getFoo" would become
     * "getFoo[0]".
     */
    private static Filter matchParameterizedMethod(final Description target) {
        return new Filter() {
            Pattern pat = Pattern.compile(target.getMethodName() + "(\\[[0-9]+\\])?");

            @Override
            public boolean shouldRun(Description desc) {
                if (desc.isTest()) {
                    return target.getClassName().equals(desc.getClassName())
                            && isMatch(desc.getMethodName());
                }

                for (Description child : desc.getChildren()) {
                    if (shouldRun(child)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isMatch(String first) {
                return pat.matcher(first).matches();
            }

            @Override
            public String describe() {
                return String.format("Method %s", target.getDisplayName());
            }
        };
    }

    /**
     * Run only tests within given java package
     * @param testPackage
     */
    public void addTestPackageFilter(String testPackage) {
        mTestPackageName = testPackage;
    }

    /**
     * Run only tests with given size
     * @param testSize
     */
    public void addTestSizeFilter(String testSize) {
        if (SMALL_SIZE.equals(testSize)) {
            mFilter = mFilter.intersect(new AnnotationInclusionFilter(SmallTest.class));
        } else if (MEDIUM_SIZE.equals(testSize)) {
            mFilter = mFilter.intersect(new AnnotationInclusionFilter(MediumTest.class));
        } else if (LARGE_SIZE.equals(testSize)) {
            mFilter = mFilter.intersect(new AnnotationInclusionFilter(LargeTest.class));
        } else {
            Log.e(LOG_TAG, String.format("Unrecognized test size '%s'", testSize));
        }
    }

    /**
     * Only run tests annotated with given annotation class.
     *
     * @param annotation the full class name of annotation
     */
    public void addAnnotationInclusionFilter(String annotation) {
        Class<? extends Annotation> annotationClass = loadAnnotationClass(annotation);
        if (annotationClass != null) {
            mFilter = mFilter.intersect(new AnnotationInclusionFilter(annotationClass));
        }
    }

    /**
     * Skip tests annotated with given annotation class.
     *
     * @param notAnnotation the full class name of annotation
     */
    public void addAnnotationExclusionFilter(String notAnnotation) {
        Class<? extends Annotation> annotationClass = loadAnnotationClass(notAnnotation);
        if (annotationClass != null) {
            mFilter = mFilter.intersect(new AnnotationExclusionFilter(annotationClass));
        }
    }

    /**
     * Build a request that will generate test started and test ended events, but will skip actual
     * test execution.
     */
    public void setSkipExecution(boolean b) {
        mSkipExecution = b;
    }

    /**
     * Builds the {@link TestRequest} based on current contents of added classes and methods.
     * <p/>
     * If no classes have been explicitly added, will scan the classpath for all tests.
     *
     */
    public TestRequest build(Instrumentation instr, Bundle bundle) {
        if (mTestLoader.isEmpty()) {
            // no class restrictions have been specified. Load all classes
            loadClassesFromClassPath();
        }

        Request request = classes(instr, bundle, mSkipExecution, new Computer(),
                mTestLoader.getLoadedClasses().toArray(new Class[0]));
        return new TestRequest(mTestLoader.getLoadFailures(), request.filterWith(mFilter));
    }

    /**
     * Create a <code>Request</code> that, when processed, will run all the tests
     * in a set of classes.
     *
     * @param instr the {@link Instrumentation} to inject into any tests that require it
     * @param bundle the {@link Bundle} of command line args to inject into any tests that require
     *         it
     * @param computer Helps construct Runners from classes
     * @param classes the classes containing the tests
     * @return a <code>Request</code> that will cause all tests in the classes to be run
     */
    private static Request classes(Instrumentation instr, Bundle bundle, boolean skipExecution,
            Computer computer, Class<?>... classes) {
        try {
            AndroidRunnerBuilder builder = new AndroidRunnerBuilder(true, instr, bundle,
                    skipExecution);
            Runner suite = computer.getSuite(builder, classes);
            return Request.runner(suite);
        } catch (InitializationError e) {
            throw new RuntimeException(
                    "Suite constructor, called as above, should always complete");
        }
    }

    private void loadClassesFromClassPath() {
        Collection<String> classNames = getClassNamesFromClassPath();
        for (String className : classNames) {
            mTestLoader.loadIfTest(className);
        }
    }

    private Collection<String> getClassNamesFromClassPath() {
        Log.i(LOG_TAG, String.format("Scanning classpath to find tests in apks %s",
                Arrays.toString(mApkPaths)));
        ClassPathScanner scanner = new ClassPathScanner(mApkPaths);

        ChainedClassNameFilter filter =   new ChainedClassNameFilter();
         // exclude inner classes
        filter.add(new ExternalClassNameFilter());
        if (mTestPackageName != null) {
            // request to run only a specific java package, honor that
            filter.add(new InclusivePackageNameFilter(mTestPackageName));
        } else {
            // scan all packages, but exclude junit packages
            filter.addAll(new ExcludePackageNameFilter("junit"),
                    new ExcludePackageNameFilter("org.junit"),
                    new ExcludePackageNameFilter("org.hamcrest"),
                    new ExcludePackageNameFilter("com.android.test.runner.junit3"));
        }

        try {
            return scanner.getClassPathEntries(filter);
        } catch (IOException e) {
            mWriter.println("failed to scan classes");
            Log.e(LOG_TAG, "Failed to scan classes", e);
        }
        return Collections.emptyList();
    }

    /**
     * Factory method for {@link ClassPathScanner}.
     * <p/>
     * Exposed so unit tests can mock.
     */
    ClassPathScanner createClassPathScanner(String... apkPaths) {
        return new ClassPathScanner(apkPaths);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadAnnotationClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (Class<? extends Annotation>)clazz;
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, String.format("Could not find annotation class: %s", className));
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, String.format("Class %s is not an annotation", className));
        }
        return null;
    }
}
