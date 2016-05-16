/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.tradefed.build;

import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Helper class for retrieving files from the CTS install.
 * <p/>
 * Encapsulates the filesystem layout of the CTS installation.
 */
public class CtsBuildHelper {

    static final String CTS_DIR_NAME = "android-cts";
    private final String mSuiteName = "CTS";
    /** The root location of the extracted CTS package */
    private final File mRootDir;
    /** the {@link CTS_DIR_NAME} directory */
    private final File mCtsDir;

    /**
     * Creates a {@link CtsBuildHelper}.
     *
     * @param rootDir the parent folder that contains the "android-cts" directory and all its
     *            contents.
     */
    public CtsBuildHelper(File rootDir) {
        mRootDir = rootDir;
        mCtsDir = new File(mRootDir, CTS_DIR_NAME);
    }

    /**
     * Alternate {@link CtsBuildHelper} constructor that takes the {@link IFolderBuildInfo}
     * representation of a CTS build.
     *
     * @param build the {@link IFolderBuildInfo}
     * @throws FileNotFoundException
     */
    public CtsBuildHelper(IFolderBuildInfo build) throws FileNotFoundException {
        this(build.getRootDir());
    }

    /**
     * A helper factory method that creates and validates a {@link CtsBuildHelper} given an
     * {@link IBuildInfo}.
     *
     * @param build the {@link IBuildInfo}
     * @return the {@link CtsBuildHelper}
     * @throws IllegalArgumentException if provided <var>build</var> is not a valid CTS build
     */
    public static CtsBuildHelper createBuildHelper(IBuildInfo build) {
        if (!(build instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException(String.format(
                    "Wrong build type. Expected %s, received %s", IFolderBuildInfo.class.getName(),
                    build.getClass().getName()));
        }
        try {
            CtsBuildHelper ctsBuild = new CtsBuildHelper((IFolderBuildInfo)build);
            ctsBuild.validateStructure();
            return ctsBuild;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid CTS build provided.", e);
        }
    }

    public String getSuiteName() {
        return mSuiteName;
    }

    /**
     * @return a {@link File} representing the parent folder of the CTS installation
     */
    public File getRootDir() {
        return mRootDir;
    }

    /**
     * @return a {@link File} representing the "android-cts" folder of the CTS installation
     */
    public File getCtsDir() {
        return mCtsDir;
    }

    /**
     * @return a {@link File} representing the test application file with given name
     * @throws FileNotFoundException if file does not exist
     */
    public File getTestApp(String appFileName) throws FileNotFoundException {
        File apkFile = new File(getTestCasesDir(), appFileName);
        if (!apkFile.exists()) {
            throw new FileNotFoundException(String.format("CTS test app file %s does not exist",
                    apkFile.getAbsolutePath()));
        }
        return apkFile;
    }

    private File getRepositoryDir() {
        return new File(getCtsDir(), "repository");
    }

    /**
     * @return a {@link File} representing the results directory.
     */
    public File getResultsDir() {
        return new File(getRepositoryDir(), "results");
    }

    /**
     * @return a {@link File} representing the directory to store result logs.
     */
    public File getLogsDir() {
        return new File(getRepositoryDir(), "logs");
    }

    /**
     * @return a {@link File} representing the test cases directory
     */
    public File getTestCasesDir() {
        return new File(getRepositoryDir(), "testcases");
    }

    /**
     * @return a {@link File} representing the test plan directory
     */
    public File getTestPlansDir() {
        return new File(getRepositoryDir(), "plans");
    }

    /**
     * @return a {@link File} representing the test plan with given name. note: no attempt will be
     * made to ensure the plan actually exists
     * @throws FileNotFoundException if plans directory does not exist
     */
    public File getTestPlanFile(String planName) throws FileNotFoundException {
        String ctsPlanRelativePath = String.format("%s.xml", planName);
        return new File(getTestPlansDir(), ctsPlanRelativePath);
    }

    /**
     * Check the validity of the CTS build file system structure.
     * @throws FileNotFoundException if any major directories are missing
     */
    public void validateStructure() throws FileNotFoundException {
        if (!getCtsDir().exists()) {
            throw new FileNotFoundException(String.format(
                    "CTS install folder %s does not exist", getCtsDir().getAbsolutePath()));
        }
        if (!getTestCasesDir().exists()) {
            throw new FileNotFoundException(String.format(
                    "CTS test cases folder %s does not exist",
                    getTestCasesDir().getAbsolutePath()));
        }
        if (!getTestPlansDir().exists()) {
            throw new FileNotFoundException(String.format(
                    "CTS test plans folder %s does not exist",
                    getTestPlansDir().getAbsolutePath()));
        }
    }
}
