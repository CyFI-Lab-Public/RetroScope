/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.tradefed.result;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.testtype.CtsTest;
import com.android.cts.tradefed.testtype.ITestPackageDef;
import com.android.cts.tradefed.testtype.ITestPackageRepo;
import com.android.cts.tradefed.testtype.ITestPlan;
import com.android.cts.tradefed.testtype.TestPackageRepo;
import com.android.cts.tradefed.testtype.TestPlan;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.Option.Importance;
import com.android.tradefed.log.LogUtil.CLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Class for creating test plans from CTS result XML.
 */
public class PlanCreator {

    @Option (name = "plan", shortName = 'p', description = "the name of the plan to create",
            importance=Importance.IF_UNSET)
    private String mPlanName = null;

    @Option (name = "session", shortName = 's', description = "the session id to derive from",
            importance=Importance.IF_UNSET)
    private Integer mSessionId = null;

    @Option (name = "result", shortName = 'r',
            description = "the result type to filter. One of pass, fail, notExecuted.",
            importance=Importance.IF_UNSET)
    private String mResultFilterString = null;

    @Option(name = CtsTest.RUN_KNOWN_FAILURES_OPTION)
    private boolean mIncludeKnownFailures = false;

    private CtsTestStatus mResultFilter = null;
    private TestResults mResult = null;

    private File mPlanFile;

    /**
     * Create an empty {@link PlanCreator}.
     * <p/>
     * All {@link Option} fields must be populated via
     * {@link com.android.tradefed.config.ArgsOptionParser}
     */
    public PlanCreator() {
    }

    /**
     * Create a {@link PlanCreator} using the specified option values.
     */
    public PlanCreator(String planName, int session, CtsTestStatus result) {
        mPlanName = planName;
        mSessionId = session;
        mResultFilterString = result.getValue();
    }

    /**
     * Create and serialize a test plan derived from a result.
     * <p/>
     * {@link Option} values must all be set before this is called.
     * @throws ConfigurationException
     */
    public void createAndSerializeDerivedPlan(CtsBuildHelper build) throws ConfigurationException {
        ITestPlan derivedPlan = createDerivedPlan(build);
        if (derivedPlan != null) {
            try {
                derivedPlan.serialize(new BufferedOutputStream(new FileOutputStream(mPlanFile)));
            } catch (IOException e) {
                Log.logAndDisplay(LogLevel.ERROR, "", String.format("Failed to create plan file %s",
                        mPlanName));
                CLog.e(e);
            }
        }
    }

    /**
     * Create a test plan derived from a result.
     * <p/>
     * {@link Option} values must all be set before this is called.
     *
     * @param build
     * @return test plan
     * @throws ConfigurationException
     */
    public ITestPlan createDerivedPlan(CtsBuildHelper build) throws ConfigurationException {
        checkFields(build);
        ITestPackageRepo pkgDefRepo = new TestPackageRepo(build.getTestCasesDir(),
                mIncludeKnownFailures);
        ITestPlan derivedPlan = new TestPlan(mPlanName);
        for (TestPackageResult pkg : mResult.getPackages()) {
            Collection<TestIdentifier> filteredTests = pkg.getTestsWithStatus(mResultFilter);
            if (!filteredTests.isEmpty()) {
                String pkgUri = pkg.getAppPackageName();
                ITestPackageDef pkgDef = pkgDefRepo.getTestPackage(pkgUri);
                if (pkgDef != null) {
                    Collection<TestIdentifier> excludedTests = new LinkedHashSet<TestIdentifier>(
                            pkgDef.getTests());
                    excludedTests.removeAll(filteredTests);
                    derivedPlan.addPackage(pkgUri);
                    derivedPlan.addExcludedTests(pkgUri, excludedTests);
                } else {
                    CLog.e("Could not find package %s in repository", pkgUri);
                }
            }
        }
        return derivedPlan;
    }

    /**
     * Check that all {@Option}s have been populated with valid values.
     * @param build
     * @throws ConfigurationException if any option has an invalid value
     */
    private void checkFields(CtsBuildHelper build) throws ConfigurationException {
        if (mSessionId == null) {
            throw new ConfigurationException("Missing --session argument");
        }
        ITestResultRepo repo = new TestResultRepo(build.getResultsDir());
        mResult = repo.getResult(mSessionId);
        if (mResult == null) {
            throw new ConfigurationException(String.format("Could not find session with id %d",
                    mSessionId));
        }
        if (mResultFilterString == null) {
            throw new ConfigurationException("Missing --result argument");
        }
        mResultFilter = CtsTestStatus.getStatus(mResultFilterString);
        if (mResultFilter == null) {
            throw new ConfigurationException(
                    "Invalid result argument. Expected one of pass,fail,notExecuted");
        }
        if (mPlanName == null) {
            throw new ConfigurationException("Missing --plan argument");
        }
        try {
            mPlanFile = build.getTestPlanFile(mPlanName);
            if (mPlanFile.exists()) {
                throw new ConfigurationException(String.format("Test plan %s already exists",
                        mPlanName));
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Could not find plans directory");
        }
    }
}
