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

import com.android.ddmlib.Log;
import com.android.tradefed.result.TestResult;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Data structure that represents a "Test" result XML element.
 */
class Test extends AbstractXmlPullParser {
    static final String TAG = "Test";
    private static final String NAME_ATTR = "name";
    private static final String MESSAGE_ATTR = "message";
    private static final String ENDTIME_ATTR = "endtime";
    private static final String STARTTIME_ATTR = "starttime";
    private static final String RESULT_ATTR = "result";
    private static final String SCENE_TAG = "FailedScene";
    private static final String STACK_TAG = "StackTrace";
    private static final String SUMMARY_TAG = "Summary";
    private static final String DETAILS_TAG = "Details";
    private static final String VALUEARRAY_TAG = "ValueArray";
    private static final String VALUE_TAG = "Value";
    private static final String TARGET_ATTR = "target";
    private static final String SCORETYPE_ATTR = "scoreType";
    private static final String UNIT_ATTR = "unit";
    private static final String SOURCE_ATTR = "source";
    // separators for the message
    private static final String LOG_SEPARATOR = "\\+\\+\\+";
    private static final String LOG_ELEM_SEPARATOR = "\\|";

    private String mName;
    private CtsTestStatus mResult;
    private String mStartTime;
    private String mEndTime;
    private String mMessage;
    private String mStackTrace;
    // summary and details passed from cts
    private String mSummary;
    private String mDetails;

    /**
     * Create an empty {@link Test}
     */
    public Test() {
    }

    /**
     * Create a {@link Test} from a {@link TestResult}.
     *
     * @param name
     */
    public Test(String name) {
        mName = name;
        mResult = CtsTestStatus.NOT_EXECUTED;
        mStartTime = TimeUtil.getTimestamp();
        updateEndTime();
    }

    /**
     * Set the name of this {@link Test}
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the name of this {@link Test}
     */
    public String getName() {
        return mName;
    }

    public CtsTestStatus getResult() {
        return mResult;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public String getEndTime() {
        return mEndTime;
    }

    public String getStackTrace() {
        return mStackTrace;
    }

    public void setStackTrace(String stackTrace) {

        mStackTrace = sanitizeStackTrace(stackTrace);
        mMessage = getFailureMessageFromStackTrace(mStackTrace);
    }

    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public String getDetails() {
        return mDetails;
    }

    public void setDetails(String details) {
        mDetails = details;
    }

    public void updateEndTime() {
        mEndTime = TimeUtil.getTimestamp();
    }

    public void setResultStatus(CtsTestStatus status) {
        mResult = status;
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer)
            throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, TAG);
        serializer.attribute(CtsXmlResultReporter.ns, NAME_ATTR, getName());
        serializer.attribute(CtsXmlResultReporter.ns, RESULT_ATTR, mResult.getValue());
        serializer.attribute(CtsXmlResultReporter.ns, STARTTIME_ATTR, mStartTime);
        serializer.attribute(CtsXmlResultReporter.ns, ENDTIME_ATTR, mEndTime);

        if (mMessage != null) {
            serializer.startTag(CtsXmlResultReporter.ns, SCENE_TAG);
            serializer.attribute(CtsXmlResultReporter.ns, MESSAGE_ATTR, mMessage);
            if (mStackTrace != null) {
                serializer.startTag(CtsXmlResultReporter.ns, STACK_TAG);
                serializer.text(mStackTrace);
                serializer.endTag(CtsXmlResultReporter.ns, STACK_TAG);
            }
            serializer.endTag(CtsXmlResultReporter.ns, SCENE_TAG);
        }
        if (mSummary != null) {
            // <Summary message = "screen copies per sec" scoretype="higherBetter" unit="fps">
            // 23938.82978723404</Summary>
            PerfResultSummary summary = parseSummary(mSummary);
            if (summary != null) {
                serializer.startTag(CtsXmlResultReporter.ns, SUMMARY_TAG);
                serializer.attribute(CtsXmlResultReporter.ns, MESSAGE_ATTR, summary.mMessage);
                if (summary.mTarget.length() != 0 && !summary.mTarget.equals(" ")) {
                    serializer.attribute(CtsXmlResultReporter.ns, TARGET_ATTR, summary.mTarget);
                }
                serializer.attribute(CtsXmlResultReporter.ns, SCORETYPE_ATTR, summary.mType);
                serializer.attribute(CtsXmlResultReporter.ns, UNIT_ATTR, summary.mUnit);
                serializer.text(summary.mValue);
                serializer.endTag(CtsXmlResultReporter.ns, SUMMARY_TAG);
                // add details only if summary is present
                // <Details>
                //   <ValueArray source=”com.android.cts.dram.BandwidthTest#doRunMemcpy:98”
                //                    message=”measure1” unit="ms" scoretype="higherBetter">
                //     <Value>0.0</Value>
                //     <Value>0.1</Value>
                //   </ValueArray>
                // </Details>
                if (mDetails != null) {
                    PerfResultDetail[] ds = parseDetails(mDetails);
                    serializer.startTag(CtsXmlResultReporter.ns, DETAILS_TAG);
                        for (PerfResultDetail d : ds) {
                            if (d == null) {
                                continue;
                            }
                            serializer.startTag(CtsXmlResultReporter.ns, VALUEARRAY_TAG);
                            serializer.attribute(CtsXmlResultReporter.ns, SOURCE_ATTR, d.mSource);
                            serializer.attribute(CtsXmlResultReporter.ns, MESSAGE_ATTR,
                                    d.mMessage);
                            serializer.attribute(CtsXmlResultReporter.ns, SCORETYPE_ATTR, d.mType);
                            serializer.attribute(CtsXmlResultReporter.ns, UNIT_ATTR, d.mUnit);
                            for (String v : d.mValues) {
                                if (v == null) {
                                    continue;
                                }
                                serializer.startTag(CtsXmlResultReporter.ns, VALUE_TAG);
                                serializer.text(v);
                                serializer.endTag(CtsXmlResultReporter.ns, VALUE_TAG);
                            }
                            serializer.endTag(CtsXmlResultReporter.ns, VALUEARRAY_TAG);
                        }
                    serializer.endTag(CtsXmlResultReporter.ns, DETAILS_TAG);
                }
            }
        }
        serializer.endTag(CtsXmlResultReporter.ns, TAG);
    }

    /**
     *  class containing performance result.
     */
    public static class PerfResultCommon {
        public String mMessage;
        public String mType;
        public String mUnit;
    }

    private class PerfResultSummary extends PerfResultCommon {
        public String mTarget;
        public String mValue;
    }

    private class PerfResultDetail extends PerfResultCommon {
        public String mSource;
        public String[] mValues;
    }

    private PerfResultSummary parseSummary(String summary) {
        String[] elems = summary.split(LOG_ELEM_SEPARATOR);
        PerfResultSummary r = new PerfResultSummary();
        if (elems.length < 5) {
            Log.w(TAG, "wrong message " + summary);
            return null;
        }
        r.mMessage = elems[0];
        r.mTarget = elems[1];
        r.mType = elems[2];
        r.mUnit = elems[3];
        r.mValue = elems[4];
        return r;
    }

    private PerfResultDetail[] parseDetails(String details) {
        String[] arrays = details.split(LOG_SEPARATOR);
        PerfResultDetail[] rs = new PerfResultDetail[arrays.length];
        for (int i = 0; i < arrays.length; i++) {
            String[] elems = arrays[i].split(LOG_ELEM_SEPARATOR);
            if (elems.length < 5) {
                Log.w(TAG, "wrong message " + arrays[i]);
                continue;
            }
            PerfResultDetail r = new PerfResultDetail();
            r.mSource = elems[0];
            r.mMessage = elems[1];
            r.mType = elems[2];
            r.mUnit = elems[3];
            r.mValues = elems[4].split(" ");
            rs[i] = r;
        }
        return rs;
    }

    /**
     * Strip out any invalid XML characters that might cause the report to be unviewable.
     * http://www.w3.org/TR/REC-xml/#dt-character
     */
    private static String sanitizeStackTrace(String trace) {
        if (trace != null) {
            return trace.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]", "");
        } else {
            return null;
        }
    }

    /**
     * Gets the failure message to show from the stack trace.
     * <p/>
     * Exposed for unit testing
     *
     * @param stack the full stack trace
     * @return the failure message
     */
    static String getFailureMessageFromStackTrace(String stack) {
        // return the first two lines of stack as failure message
        int endPoint = stack.indexOf('\n');
        if (endPoint != -1) {
            int nextLine = stack.indexOf('\n', endPoint + 1);
            if (nextLine != -1) {
                return stack.substring(0, nextLine);
            }
        }
        return stack;
    }

    /**
     * Populates this class with test result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of a Test tag
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(TAG)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", TAG, parser.getName()));
        }
        setName(getAttribute(parser, NAME_ATTR));
        mResult = CtsTestStatus.getStatus(getAttribute(parser, RESULT_ATTR));
        mStartTime = getAttribute(parser, STARTTIME_ATTR);
        mEndTime = getAttribute(parser, ENDTIME_ATTR);

        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(SCENE_TAG)) {
                mMessage = getAttribute(parser, MESSAGE_ATTR);
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals(STACK_TAG)) {
                mStackTrace = parser.nextText();
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }
}
