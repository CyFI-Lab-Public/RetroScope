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

import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An implementation of {@link ITestResultsRepo}.
 */
public class TestResultRepo implements ITestResultRepo {

    /**
     * ordered list of result directories. the index of each file is its session id.
     */
    private List<File> mResultDirs;

    /**
     * Create a {@link TestResultRepo} from a directory of results
     *
     * @param testResultsDir the parent directory of results
     */
    public TestResultRepo(File testResultsDir) {
        mResultDirs = new ArrayList<File>();
        File[] resultArray = testResultsDir.listFiles(new ResultDirFilter());
        if (resultArray != null) {
            List<File> resultList = new ArrayList<File>();
            Collections.addAll(resultList, resultArray);
            Collections.sort(resultList, new FileComparator());
            for (int i=0; i < resultList.size(); i++) {
                File resultFile = new File(resultList.get(i),
                        CtsXmlResultReporter.TEST_RESULT_FILE_NAME);
                if (resultFile.exists()) {
                    mResultDirs.add(resultList.get(i));
                }
            }
        }
    }

    @Override
    public File getReportDir(int sessionId) {
        return mResultDirs.get(sessionId);
    }

    private ITestSummary parseSummary(int id, File resultDir) {
        TestSummaryXml result = new TestSummaryXml(id, resultDir.getName());
        try {
            result.parse(new BufferedReader(new FileReader(new File(resultDir,
                    CtsXmlResultReporter.TEST_RESULT_FILE_NAME))));
            return result;
        } catch (ParseException e) {
            CLog.e(e);
        } catch (FileNotFoundException e) {
            // should never happen, since we check for file existence above. Barf the stack trace
            CLog.e(e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ITestSummary> getSummaries() {
        // parsing the summary data should be relatively quick, so just parse it every time
        // rather than caching it
        List<ITestSummary> summaries = new ArrayList<ITestSummary>(mResultDirs.size());
        for (int i = 0; i < mResultDirs.size(); i++) {
            summaries.add(parseSummary(i, mResultDirs.get(i)));
        }
        return summaries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestResults getResult(int sessionId) {
        // TODO: consider caching the results in future
        if (mResultDirs.size() <= sessionId) {
            CLog.e("Session id %d does not exist", sessionId);
            return null;
        }
        try {
            TestResults results = new TestResults();
            File resultFile = new File(mResultDirs.get(sessionId),
                    CtsXmlResultReporter.TEST_RESULT_FILE_NAME);
            results.parse(new BufferedReader(new FileReader(resultFile)));
            return results;
        } catch (FileNotFoundException e) {
            CLog.e("Could not find result file for session %d", sessionId);
        } catch (ParseException e) {
            CLog.e("Failed to parse result file for session %d", sessionId);
        }
        return null;
    }

    private class ResultDirFilter implements FileFilter {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    /**
     * A {@link Comparator} that compares {@link File}s by name.
     */
    private class FileComparator implements Comparator<File> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(File file0, File file1) {
            return file0.getName().compareTo(file1.getName());
        }
    }
}
