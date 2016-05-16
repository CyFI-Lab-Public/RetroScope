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

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.tests.getinfo.DeviceInfoConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data structure for the device info collected by CTS.
 * <p/>
 * Provides methods to serialize and deserialize from XML, as well as checks for consistency
 * when multiple devices are used to generate the report.
 */
class DeviceInfoResult extends AbstractXmlPullParser {
    static final String TAG = "DeviceInfo";
    private static final String ns = CtsXmlResultReporter.ns;
    static final String BUILD_TAG = "BuildInfo";
    private static final String PHONE_TAG = "PhoneSubInfo";
    private static final String SCREEN_TAG = "Screen";

    private static final String FEATURE_INFO_TAG = "FeatureInfo";
    private static final String FEATURE_TAG = "Feature";
    private static final String FEATURE_ATTR_DELIM = ":";
    private static final String FEATURE_DELIM = ";";

    private static final String OPENGL_TEXTURE_FORMATS_INFO_TAG =
            "OpenGLCompressedTextureFormatsInfo";
    private static final String OPENGL_TEXTURE_FORMAT_TAG = "TextureFormat";
    private static final String OPENGL_TEXTURE_FORMAT_DELIM = ";";

    private static final String SYSLIB_INFO_TAG = "SystemLibrariesInfo";
    private static final String SYSLIB_TAG = "Library";
    private static final String SYSLIB_DELIM = ";";

    private static final String PROCESS_INFO_TAG = "ProcessInfo";
    private static final String PROCESS_TAG = "Process";
    private static final String PROCESS_DELIM = ";";
    private static final String PROCESS_ATTR_DELIM = ":";

    private Map<String, String> mMetrics = new HashMap<String, String>();

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, TAG);

        if (!mMetrics.isEmpty()) {

            // Extract metrics that need extra handling, and then dump the remainder into BuildInfo
            Map<String, String> metricsCopy = new HashMap<String, String>(mMetrics);
            serializer.startTag(ns, SCREEN_TAG);
            serializer.attribute(ns, DeviceInfoConstants.RESOLUTION,
                    getMetric(metricsCopy, DeviceInfoConstants.RESOLUTION));
            serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY,
                    getMetric(metricsCopy, DeviceInfoConstants.SCREEN_DENSITY));
            serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY_BUCKET,
                    getMetric(metricsCopy, DeviceInfoConstants.SCREEN_DENSITY_BUCKET));
            serializer.attribute(ns, DeviceInfoConstants.SCREEN_SIZE,
                    getMetric(metricsCopy, DeviceInfoConstants.SCREEN_SIZE));
            serializer.endTag(ns, SCREEN_TAG);

            serializer.startTag(ns, PHONE_TAG);
            serializer.attribute(ns, DeviceInfoConstants.PHONE_NUMBER,
                    getMetric(metricsCopy, DeviceInfoConstants.PHONE_NUMBER));
            serializer.endTag(ns, PHONE_TAG);

            String featureData = getMetric(metricsCopy, DeviceInfoConstants.FEATURES);
            String processData = getMetric(metricsCopy, DeviceInfoConstants.PROCESSES);
            String sysLibData = getMetric(metricsCopy, DeviceInfoConstants.SYS_LIBRARIES);
            String textureData = getMetric(metricsCopy,
                    DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS);

            // dump the remaining metrics without translation
            serializer.startTag(ns, BUILD_TAG);
            for (Map.Entry<String, String> metricEntry : metricsCopy.entrySet()) {
                serializer.attribute(ns, metricEntry.getKey(), metricEntry.getValue());
            }
            serializer.endTag(ns, BUILD_TAG);

            serializeFeatureInfo(serializer, featureData);
            serializeProcessInfo(serializer, processData);
            serializeSystemLibrariesInfo(serializer, sysLibData);
            serializeOpenGLCompressedTextureFormatsInfo(serializer, textureData);
        } else {
            // this might be expected, if device info collection was turned off
            CLog.d("Could not find device info");
        }
        serializer.endTag(ns, TAG);
    }

    /**
     * Fetch and remove given metric from hashmap.
     *
     * @return the metric value or empty string if it was not present in map.
     */
    private String getMetric(Map<String, String> metrics, String metricName ) {
        String value = metrics.remove(metricName);
        if (value == null) {
            value = "";
        }
        return value;
    }

    private void serializeFeatureInfo(KXmlSerializer serializer, String featureData)
            throws IOException {
        serialize(serializer, FEATURE_INFO_TAG, FEATURE_TAG, FEATURE_DELIM, FEATURE_ATTR_DELIM,
                featureData, "name", "type", "available");
    }

    private void serializeProcessInfo(KXmlSerializer serializer, String rootProcesses)
            throws IOException {
        serialize(serializer, PROCESS_INFO_TAG, PROCESS_TAG, PROCESS_DELIM, PROCESS_ATTR_DELIM,
                rootProcesses, "name", "uid");
    }

    private void serializeOpenGLCompressedTextureFormatsInfo(KXmlSerializer serializer,
            String formats) throws IOException {
        serialize(serializer, OPENGL_TEXTURE_FORMATS_INFO_TAG, OPENGL_TEXTURE_FORMAT_TAG,
                OPENGL_TEXTURE_FORMAT_DELIM, null, formats, "name");
    }

    private void serializeSystemLibrariesInfo(KXmlSerializer serializer, String libs)
            throws IOException {
        serialize(serializer, SYSLIB_INFO_TAG, SYSLIB_TAG, SYSLIB_DELIM, null, libs, "name");
    }

    /**
     * Serializes a XML structure where there is an outer tag with tags inside it.
     *
     * <pre>
     *   Input: value1:value2;value3:value4
     *
     *   Output:
     *   <OuterTag>
     *     <SubTag attr1="value1" attr2="value2" />
     *     <SubTag attr1="value3" attr2="value4" />
     *   </OuterTag>
     * </pre>
     *
     * @param serializer to do it
     * @param tag would be "OuterTag"
     * @param subTag would be "SubTag"
     * @param delim would be ";"
     * @param attrDelim would be ":" in the example but can be null if only one attrName given
     * @param data would be "value1:value2;value3:value4"
     * @param attrNames would be an array with "attr1", "attr2"
     * @throws IOException if there is a problem
     */
    private void serialize(KXmlSerializer serializer, String tag, String subTag,
            String delim, String attrDelim, String data, String... attrNames) throws IOException {
        serializer.startTag(ns, tag);

        if (data == null) {
            data = "";
        }

        String[] values = data.split(delim);
        for (String value : values) {
            if (!value.isEmpty()) {
                String[] attrValues = attrDelim != null ? value.split(attrDelim) : new String[] {value};
                if (attrValues.length == attrNames.length) {
                    serializer.startTag(ns, subTag);
                    for (int i = 0; i < attrNames.length; i++) {
                        serializer.attribute(ns, attrNames[i], attrValues[i]);
                    }
                    serializer.endTag(ns,  subTag);
                }
            }
        }

        serializer.endTag(ns, tag);
    }

    /**
     * Populates this class with package result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of a {@link #TAG}
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(TAG)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", TAG, parser.getName()));
        }
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(SCREEN_TAG) ||
                        parser.getName().equals(PHONE_TAG) ||
                        parser.getName().equals(BUILD_TAG)) {
                    addMetricsFromAttributes(parser);
                } else if (parser.getName().equals(FEATURE_INFO_TAG)) {
                    mMetrics.put(DeviceInfoConstants.FEATURES, parseFeatures(parser));
                } else if (parser.getName().equals(PROCESS_INFO_TAG)) {
                    mMetrics.put(DeviceInfoConstants.PROCESSES, parseProcess(parser));
                } else if (parser.getName().equals(SYSLIB_INFO_TAG)) {
                    mMetrics.put(DeviceInfoConstants.SYS_LIBRARIES, parseSystemLibraries(parser));
                } else if (parser.getName().equals(OPENGL_TEXTURE_FORMATS_INFO_TAG)) {
                    mMetrics.put(DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS,
                            parseOpenGLCompressedTextureFormats(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }

    private String parseFeatures(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parseTag(parser, FEATURE_INFO_TAG, FEATURE_TAG, FEATURE_DELIM, FEATURE_ATTR_DELIM,
                "name", "type", "available");
    }

    private String parseProcess(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parseTag(parser, PROCESS_INFO_TAG, PROCESS_TAG, PROCESS_DELIM,
                PROCESS_ATTR_DELIM, "name", "uid");
    }

    private String parseOpenGLCompressedTextureFormats(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        return parseTag(parser, OPENGL_TEXTURE_FORMATS_INFO_TAG, OPENGL_TEXTURE_FORMAT_TAG,
                OPENGL_TEXTURE_FORMAT_DELIM, null, "name");
    }

    private String parseSystemLibraries(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        return parseTag(parser, SYSLIB_INFO_TAG, SYSLIB_TAG, SYSLIB_DELIM, null, "name");
    }

    /**
     * Converts XML into a flattened string.
     *
     * <pre>
     *   Input:
     *   <OuterTag>
     *     <SubTag attr1="value1" attr2="value2" />
     *     <SubTag attr1="value3" attr2="value4" />
     *   </OuterTag>
     *
     *   Output: value1:value2;value3:value4
     * </pre>
     *
     * @param parser that parses the xml
     * @param tag like "OuterTag"
     * @param subTag like "SubTag"
     * @param delim like ";"
     * @param attrDelim like ":" or null if tehre is only one attribute
     * @param attrNames like "attr1", "attr2"
     * @return flattened string like "value1:value2;value3:value4"
     * @throws XmlPullParserException
     * @throws IOException
     */
    private String parseTag(XmlPullParser parser, String tag, String subTag, String delim,
            String attrDelim, String... attrNames) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(tag)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", tag,
                    parser.getName()));
        }
        StringBuilder flattened = new StringBuilder();

        for (int eventType = parser.getEventType();
                eventType != XmlPullParser.END_DOCUMENT;
                eventType = parser.next()) {

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(subTag)) {
                for (int i = 0; i < attrNames.length; i++) {
                    flattened.append(getAttribute(parser, attrNames[i]));
                    if (i + 1 < attrNames.length) {
                        flattened.append(attrDelim);
                    }
                }
                flattened.append(delim);
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(tag)) {
                break;
            }
        }

        return flattened.toString();
    }

    /**
     * Adds all attributes from the current XML tag to metrics as name-value pairs
     */
    private void addMetricsFromAttributes(XmlPullParser parser) {
        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            mMetrics.put(parser.getAttributeName(i), parser.getAttributeValue(i));
        }
    }

    /**
     * Populate the device info metrics with values collected from device.
     * <p/>
     * Check that the provided device info metrics are consistent with the currently stored metrics.
     * If any inconsistencies occur, logs errors and stores error messages in the metrics map
     */
    public void populateMetrics(Map<String, String> metrics) {
        if (mMetrics.isEmpty()) {
            // no special processing needed, no existing metrics
            mMetrics.putAll(metrics);
            return;
        }
        Map<String, String> metricsCopy = new HashMap<String, String>(
                metrics);
        // add values for metrics that might be different across runs
        combineMetrics(metricsCopy, DeviceInfoConstants.PHONE_NUMBER, DeviceInfoConstants.IMSI,
                DeviceInfoConstants.IMSI, DeviceInfoConstants.SERIAL_NUMBER);

        // ensure all the metrics we expect to be identical actually are
        checkMetrics(metricsCopy, DeviceInfoConstants.BUILD_FINGERPRINT,
                DeviceInfoConstants.BUILD_MODEL, DeviceInfoConstants.BUILD_BRAND,
                DeviceInfoConstants.BUILD_MANUFACTURER, DeviceInfoConstants.BUILD_BOARD,
                DeviceInfoConstants.BUILD_DEVICE, DeviceInfoConstants.PRODUCT_NAME,
                DeviceInfoConstants.BUILD_ABI, DeviceInfoConstants.BUILD_ABI2,
                DeviceInfoConstants.SCREEN_SIZE);
    }

    private void combineMetrics(Map<String, String> metrics, String... keysToCombine) {
        for (String combineKey : keysToCombine) {
            String currentKeyValue = mMetrics.get(combineKey);
            String valueToAdd = metrics.remove(combineKey);
            if (valueToAdd != null) {
                if (currentKeyValue == null) {
                    // strange - no existing value. Can occur during unit testing
                    mMetrics.put(combineKey, valueToAdd);
                } else if (!currentKeyValue.equals(valueToAdd)) {
                    // new value! store a comma separated list
                    valueToAdd = String.format("%s,%s", currentKeyValue, valueToAdd);
                    mMetrics.put(combineKey, valueToAdd);
                } else {
                    // ignore, current value is same as existing
                }

            } else {
                CLog.d("Missing metric %s", combineKey);
            }
        }
    }

    private void checkMetrics(Map<String, String> metrics, String... keysToCheck) {
        Set<String> keyCheckSet = new HashSet<String>();
        Collections.addAll(keyCheckSet, keysToCheck);
        for (Map.Entry<String, String> metricEntry : metrics.entrySet()) {
            String currentValue = mMetrics.get(metricEntry.getKey());
            if (keyCheckSet.contains(metricEntry.getKey()) && currentValue != null
                    && !metricEntry.getValue().equals(currentValue)) {
                CLog.e("Inconsistent info collected from devices. "
                        + "Current result has %s='%s', Received '%s'. Are you sharding or " +
                        "resuming a test run across different devices and/or builds?",
                        metricEntry.getKey(), currentValue, metricEntry.getValue());
                mMetrics.put(metricEntry.getKey(),
                        String.format("ERROR: Inconsistent results: %s, %s",
                                metricEntry.getValue(), currentValue));
            } else {
                mMetrics.put(metricEntry.getKey(), metricEntry.getValue());
            }
        }
    }

    /**
     * Return the currently stored metrics.
     * <p/>
     * Exposed for unit testing.
     */
    Map<String, String> getMetrics() {
        return mMetrics;
    }
}
