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

import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.tests.getinfo.DeviceInfoConstants;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A {@link ITestSummary} that parses summary data from the CTS result XML.
 */
public class TestSummaryXml extends AbstractXmlPullParser implements ITestSummary  {

    private final int mId;
    private final String mTimestamp;
    private int mNumFailed = 0;
    private int mNumNotExecuted = 0;
    private int mNumPassed = 0;
    private String mPlan = "NA";
    private String mStartTime = "unknown";
    private String mDeviceSerials = "unknown";

    /**
     * @param id
     * @param resultFile
     * @throws ParseException
     * @throws FileNotFoundException
     */
    public TestSummaryXml(int id, String timestamp) {
        mId = id;
        mTimestamp = timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return mId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTimestamp() {
        return mTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumIncomplete() {
        return mNumNotExecuted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumFailed() {
        return mNumFailed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumPassed() {
        return mNumPassed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTestPlan() {
        return mPlan ;
    }


    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    CtsXmlResultReporter.RESULT_TAG)) {
                mPlan = getAttribute(parser, CtsXmlResultReporter.PLAN_ATTR);
                mStartTime = getAttribute(parser, CtsXmlResultReporter.STARTTIME_ATTR);
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    DeviceInfoResult.BUILD_TAG)) {
                mDeviceSerials = getAttribute(parser, DeviceInfoConstants.SERIAL_NUMBER);
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    TestResults.SUMMARY_TAG)) {
                mNumFailed = parseIntAttr(parser, TestResults.FAILED_ATTR) +
                    parseIntAttr(parser, TestResults.TIMEOUT_ATTR);
                mNumNotExecuted = parseIntAttr(parser, TestResults.NOT_EXECUTED_ATTR);
                mNumPassed = parseIntAttr(parser, TestResults.PASS_ATTR);
                // abort after parsing Summary, which should be the last tag
                return;
             }
            eventType = parser.next();
        }
        throw new XmlPullParserException("Could not find Summary tag");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStartTime() {
        return mStartTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDeviceSerials() {
        return mDeviceSerials;
    }
}
