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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class that sends a HTTP POST multipart/form-data request containing
 * the test result XML.
 */
class ResultReporter {

    private static final int RESULT_XML_BYTES = 500 * 1024;

    private final String mServerUrl;
    private final String mSuiteName;

    ResultReporter(String serverUrl, String suiteName) {
        mServerUrl = serverUrl;
        mSuiteName = suiteName;
    }

    public void reportResult(File reportFile) throws IOException {
        if (isEmpty(mServerUrl)) {
            return;
        }

        InputStream input = new FileInputStream(reportFile);
        try {
            byte[] data = IssueReporter.getBytes(input, RESULT_XML_BYTES);
            new MultipartForm(mServerUrl)
                    .addFormValue("suite", mSuiteName)
                    .addFormFile("resultXml", "testResult.xml.gz", data)
                    .submit();
        } finally {
            input.close();
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
